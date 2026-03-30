package com.cap.timesheet.messaging.consumer;

import com.cap.timesheet.dto.ProjectDTO;
import com.cap.timesheet.entity.Project;
import com.cap.timesheet.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectSyncConsumerTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectSyncConsumer projectSyncConsumer;

    @Test
    void onProjectSync_CreateNew_Success() {
        ProjectDTO dto = ProjectDTO.builder()
                .id(1L)
                .projectCode("NEW-PRJ")
                .name("New Project")
                .costCenter("CC001")
                .isBillable(true)
                .isActive(true)
                .build();

        when(projectRepository.findByProjectCode("NEW-PRJ")).thenReturn(Optional.empty());

        projectSyncConsumer.onProjectSync(dto);

        verify(projectRepository, times(1)).save(any(Project.class));
    }

    @Test
    void onProjectSync_UpdateExisting_Success() {
        ProjectDTO dto = ProjectDTO.builder()
                .id(1L)
                .projectCode("EXISTING-PRJ")
                .name("Updated Name")
                .isActive(false)
                .build();

        Project existing = Project.builder()
                .id(1L)
                .projectCode("EXISTING-PRJ")
                .name("Old Name")
                .isActive(true)
                .build();

        when(projectRepository.findByProjectCode("EXISTING-PRJ")).thenReturn(Optional.of(existing));

        projectSyncConsumer.onProjectSync(dto);

        verify(projectRepository, times(1)).save(argThat(p -> 
            p.getName().equals("Updated Name") && !p.getIsActive()
        ));
    }
}
