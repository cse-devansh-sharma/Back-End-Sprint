package com.cap.admin.service;

import com.cap.admin.dto.*;
import com.cap.admin.entity.*;
import com.cap.admin.exception.BusinessRuleException;
import com.cap.admin.exception.ResourceNotFoundException;
import com.cap.admin.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MasterDataService {

    private final ProjectRepository      projectRepository;
    private final CostCenterRepository   costCenterRepository;
    private final LeavePolicyRepository  leavePolicyRepository;
    private final RabbitTemplate         rabbitTemplate;

    // ════════════════════════════════════════════════════
    // PROJECTS
    // ════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ProjectDTO> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::toProjectDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProjectDTO createProject(ProjectDTO dto, Long createdBy) {
        if (projectRepository.existsByProjectCode(dto.getProjectCode())) {
            throw new BusinessRuleException(
                    "Project code already exists: " + dto.getProjectCode());
        }

        CostCenter cc = null;
        if (dto.getCostCenterId() != null) {
            cc = costCenterRepository.findById(dto.getCostCenterId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Cost center not found: " + dto.getCostCenterId()));
        }

        Project project = Project.builder()
                .projectCode(dto.getProjectCode())
                .name(dto.getName())
                .costCenter(cc)
                .isBillable(dto.getIsBillable() != null ? dto.getIsBillable() : true)
                .isActive(true)
                .createdBy(createdBy)
                .build();

        project = projectRepository.save(project);
        publishProjectSync("ProjectCreated", project);
        return toProjectDTO(project);
    }

    @Transactional
    public ProjectDTO updateProject(Long id, ProjectDTO dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));

        // check code uniqueness only if changing it
        if (!project.getProjectCode().equals(dto.getProjectCode())
                && projectRepository.existsByProjectCode(dto.getProjectCode())) {
            throw new BusinessRuleException(
                    "Project code already in use: " + dto.getProjectCode());
        }

        project.setProjectCode(dto.getProjectCode());
        project.setName(dto.getName());
        if (dto.getIsBillable() != null) project.setIsBillable(dto.getIsBillable());

        project = projectRepository.save(project);
        publishProjectSync("ProjectUpdated", project);
        return toProjectDTO(project);
    }

    @Transactional
    public ProjectDTO deactivateProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found: " + id));
        project.setIsActive(false);
        project = projectRepository.save(project);
        publishProjectSync("ProjectDeactivated", project);
        return toProjectDTO(project);
    }

    private void publishProjectSync(String eventType, Project project) {
        try {
            // fanout — no routing key needed
            rabbitTemplate.convertAndSend("master.data.sync", "", toProjectDTO(project));
            log.info("[ADMIN] Published {} for project {}", eventType, project.getProjectCode());
        } catch (Exception e) {
            log.error("[ADMIN] Failed to publish project sync: {}", e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════
    // COST CENTERS
    // ════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<CostCenterDTO> getAllCostCenters() {
        return costCenterRepository.findAll().stream()
                .map(c -> new CostCenterDTO(c.getId(), c.getCode(), c.getName(), c.getIsActive()))
                .collect(Collectors.toList());
    }

    @Transactional
    public CostCenterDTO createCostCenter(CostCenterDTO dto) {
        if (costCenterRepository.existsByCode(dto.getCode())) {
            throw new BusinessRuleException("Cost center code already exists: " + dto.getCode());
        }
        CostCenter cc = CostCenter.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .isActive(true)
                .build();
        cc = costCenterRepository.save(cc);
        return new CostCenterDTO(cc.getId(), cc.getCode(), cc.getName(), cc.getIsActive());
    }

    @Transactional
    public CostCenterDTO updateCostCenter(Long id, CostCenterDTO dto) {
        CostCenter cc = costCenterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cost center not found: " + id));
        cc.setName(dto.getName());
        if (dto.getIsActive() != null) cc.setIsActive(dto.getIsActive());
        cc = costCenterRepository.save(cc);
        return new CostCenterDTO(cc.getId(), cc.getCode(), cc.getName(), cc.getIsActive());
    }

    // ════════════════════════════════════════════════════
    // LEAVE POLICIES
    // ════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<LeavePolicyDTO> getAllLeavePolicies() {
        return leavePolicyRepository.findAll().stream()
                .map(this::toPolicyDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public LeavePolicyDTO createLeavePolicy(LeavePolicyDTO dto) {
        if (leavePolicyRepository.existsByLeaveTypeCodeAndYear(
                dto.getLeaveTypeCode(), dto.getYear())) {
            throw new BusinessRuleException(
                    "Policy already exists for " + dto.getLeaveTypeCode()
                    + " in year " + dto.getYear());
        }
        LeavePolicy policy = LeavePolicy.builder()
                .policyCode(dto.getPolicyCode())
                .leaveTypeCode(dto.getLeaveTypeCode())
                .year(dto.getYear())
                .allotmentDays(dto.getAllotmentDays())
                .carryForwardDays(dto.getCarryForwardDays())
                .encashmentAllowed(dto.getEncashmentAllowed() != null ? dto.getEncashmentAllowed() : false)
                .probationExclusion(dto.getProbationExclusion() != null ? dto.getProbationExclusion() : false)
                .isActive(true)
                .build();
        return toPolicyDTO(leavePolicyRepository.save(policy));
    }

    @Transactional
    public LeavePolicyDTO updateLeavePolicy(Long id, LeavePolicyDTO dto) {
        LeavePolicy policy = leavePolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave policy not found: " + id));
        policy.setAllotmentDays(dto.getAllotmentDays());
        if (dto.getCarryForwardDays() != null) policy.setCarryForwardDays(dto.getCarryForwardDays());
        if (dto.getEncashmentAllowed() != null) policy.setEncashmentAllowed(dto.getEncashmentAllowed());
        if (dto.getIsActive() != null) policy.setIsActive(dto.getIsActive());
        return toPolicyDTO(leavePolicyRepository.save(policy));
    }

    // ════════════════════════════════════════════════════
    // DTO MAPPERS
    // ════════════════════════════════════════════════════

    private ProjectDTO toProjectDTO(Project p) {
        return ProjectDTO.builder()
                .id(p.getId())
                .projectCode(p.getProjectCode())
                .name(p.getName())
                .isBillable(p.getIsBillable())
                .isActive(p.getIsActive())
                .costCenterId(p.getCostCenter() != null ? p.getCostCenter().getId() : null)
                .build();
    }

    private LeavePolicyDTO toPolicyDTO(LeavePolicy lp) {
        return LeavePolicyDTO.builder()
                .id(lp.getId())
                .policyCode(lp.getPolicyCode())
                .leaveTypeCode(lp.getLeaveTypeCode())
                .year(lp.getYear())
                .allotmentDays(lp.getAllotmentDays())
                .carryForwardDays(lp.getCarryForwardDays())
                .encashmentAllowed(lp.getEncashmentAllowed())
                .probationExclusion(lp.getProbationExclusion())
                .isActive(lp.getIsActive())
                .build();
    }
}
