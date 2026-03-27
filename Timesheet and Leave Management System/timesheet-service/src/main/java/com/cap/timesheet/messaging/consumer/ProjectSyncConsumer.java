package com.cap.timesheet.messaging.consumer;

import com.cap.timesheet.dto.ProjectDTO;
import com.cap.timesheet.entity.Project;
import com.cap.timesheet.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens on master.data.sync fanout exchange.
 * Upserts the local projects read-model so timesheet-service
 * never needs to call admin-service directly.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectSyncConsumer {

    private final ProjectRepository projectRepository;

    @RabbitListener(queues = "master.data.sync.timesheet")
    public void onProjectSync(ProjectDTO dto) {
        log.info("[TIMESHEET] Received project sync for code={}", dto.getProjectCode());
        try {
            Project project = projectRepository
                    .findByProjectCode(dto.getProjectCode())
                    .orElse(Project.builder().build());

            project.setId(dto.getId());
            project.setProjectCode(dto.getProjectCode());
            project.setName(dto.getName());
            project.setCostCenter(dto.getCostCenter());
            project.setIsBillable(dto.getIsBillable() != null ? dto.getIsBillable() : true);
            project.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
            project.setSyncedAt(java.time.LocalDateTime.now());

            projectRepository.save(project);
            log.info("[TIMESHEET] Project {} upserted in local read-model", dto.getProjectCode());
        } catch (Exception e) {
            log.error("[TIMESHEET] Failed to sync project {}: {}", dto.getProjectCode(), e.getMessage());
        }
    }
}
