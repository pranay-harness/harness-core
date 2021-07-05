package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.remote.OrganizationMapper.writeDto;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.ng.core.api.AggregateProjectService;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.ProjectAggregateDTO;
import io.harness.ng.core.dto.ProjectAggregateDTO.ProjectAggregateDTOBuilder;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.remote.ProjectMapper;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;
import io.harness.ng.core.user.service.NgUserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@OwnedBy(PL)
@Singleton
@Slf4j
public class AggregateProjectServiceImpl implements AggregateProjectService {
  private static final String PROJECT_ADMIN_ROLE = "_project_admin";
  private final ProjectService projectService;
  private final OrganizationService organizationService;
  private final NgUserService ngUserService;
  private final ExecutorService executorService;

  @Inject
  public AggregateProjectServiceImpl(ProjectService projectService, OrganizationService organizationService,
      NgUserService ngUserService, @Named("aggregate-projects") ExecutorService executorService) {
    this.projectService = projectService;
    this.organizationService = organizationService;
    this.ngUserService = ngUserService;
    this.executorService = executorService;
  }

  @Override
  public ProjectAggregateDTO getProjectAggregateDTO(String accountIdentifier, String orgIdentifier, String identifier) {
    Optional<Project> projectOptional = projectService.get(accountIdentifier, orgIdentifier, identifier);
    if (!projectOptional.isPresent()) {
      throw new NotFoundException(
          String.format("Project with orgIdentifier [%s] and identifier [%s] not found", orgIdentifier, identifier));
    }
    return buildAggregateDTO(accountIdentifier, projectOptional.get());
  }

  @Override
  public Page<ProjectAggregateDTO> listProjectAggregateDTO(
      String accountIdentifier, Pageable pageable, ProjectFilterDTO projectFilterDTO) {
    Page<Project> projects = projectService.list(accountIdentifier, pageable, projectFilterDTO);
    List<Project> projectList = projects.toList();

    List<Callable<ProjectAggregateDTO>> tasks = new ArrayList<>();
    List<ProjectAggregateDTO> aggregates = new ArrayList<>();

    projects.forEach(project -> tasks.add(() -> buildAggregateDTO(accountIdentifier, project)));
    List<Future<ProjectAggregateDTO>> futures = null;

    try {
      futures = executorService.invokeAll(tasks, 10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    for (int i = 0; i < futures.size(); i++) {
      try {
        aggregates.add(futures.get(i).get());
      } catch (CancellationException e) {
        Project project = projectList.get(i);
        log.error("Project aggregate task cancelled for project [{}/{}/{}]", project.getAccountIdentifier(),
            project.getOrgIdentifier(), project.getIdentifier(), e);
        aggregates.add(ProjectAggregateDTO.builder().projectResponse(ProjectMapper.toResponseWrapper(project)).build());
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        Project project = projectList.get(i);
        log.error("Error while computing aggregate for project [{}/{}/{}]", project.getAccountIdentifier(),
            project.getOrgIdentifier(), project.getIdentifier(), e);
        aggregates.add(ProjectAggregateDTO.builder().projectResponse(ProjectMapper.toResponseWrapper(project)).build());
      }
    }

    return new PageImpl<>(aggregates, projects.getPageable(), projects.getTotalElements());
  }

  private ProjectAggregateDTO buildAggregateDTO(final String accountIdentifier, final Project project) {
    ProjectAggregateDTOBuilder projectAggregateDTOBuilder =
        ProjectAggregateDTO.builder().projectResponse(ProjectMapper.toResponseWrapper(project));

    Optional<Organization> organizationOptional =
        organizationService.get(accountIdentifier, project.getOrgIdentifier());

    if (organizationOptional.isPresent()) {
      OrganizationDTO organizationDTO = writeDto(organizationOptional.get());
      projectAggregateDTOBuilder.organization(organizationDTO).harnessManagedOrg(organizationDTO.isHarnessManaged());
    }

    Scope projectScope = Scope.builder()
                             .accountIdentifier(project.getAccountIdentifier())
                             .orgIdentifier(project.getOrgIdentifier())
                             .projectIdentifier(project.getIdentifier())
                             .build();
    List<UserMetadataDTO> usersInProject = ngUserService.listUsers(projectScope);

    List<UserMetadataDTO> adminUsers = ngUserService.listUsersHavingRole(Scope.builder()
                                                                             .accountIdentifier(accountIdentifier)
                                                                             .orgIdentifier(project.getOrgIdentifier())
                                                                             .projectIdentifier(project.getIdentifier())
                                                                             .build(),
        PROJECT_ADMIN_ROLE);

    List<UserMetadataDTO> collaborators = usersInProject.stream()
                                              .filter(user
                                                  -> !adminUsers.stream()
                                                          .map(UserMetadataDTO::getUuid)
                                                          .collect(Collectors.toSet())
                                                          .contains(user.getUuid()))
                                              .collect(toList());
    collaborators.removeAll(adminUsers);

    return projectAggregateDTOBuilder.admins(adminUsers).collaborators(collaborators).build();
  }
}
