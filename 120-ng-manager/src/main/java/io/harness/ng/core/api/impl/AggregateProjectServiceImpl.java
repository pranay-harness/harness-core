package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.remote.OrganizationMapper.writeDto;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.Scope.ScopeKeys;
import io.harness.ng.core.api.AggregateProjectService;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.ProjectAggregateDTO;
import io.harness.ng.core.dto.ProjectAggregateDTO.ProjectAggregateDTOBuilder;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.invites.dto.UserMetadataDTO;
import io.harness.ng.core.remote.ProjectMapper;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.service.NgUserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(PL)
@Singleton
@Slf4j
public class AggregateProjectServiceImpl implements AggregateProjectService {
  private static final String PROJECT_ADMIN_ROLE = "_project_admin";
  private final ProjectService projectService;
  private final OrganizationService organizationService;
  private final NgUserService ngUserService;

  @Inject
  public AggregateProjectServiceImpl(
      ProjectService projectService, OrganizationService organizationService, NgUserService ngUserService) {
    this.projectService = projectService;
    this.organizationService = organizationService;
    this.ngUserService = ngUserService;
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
    return projects.map(project -> buildAggregateDTO(accountIdentifier, project));
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

    List<UserMetadataDTO> usersInProject =
        ngUserService
            .listUserMemberships(Criteria.where(UserMembershipKeys.scopes)
                                     .elemMatch(Criteria.where(ScopeKeys.accountIdentifier)
                                                    .is(accountIdentifier)
                                                    .and(ScopeKeys.orgIdentifier)
                                                    .is(project.getOrgIdentifier())
                                                    .and(ScopeKeys.projectIdentifier)
                                                    .is(project.getIdentifier())))
            .stream()
            .map(x -> UserMetadataDTO.builder().uuid(x.getUuid()).email(x.getEmailId()).name(x.getName()).build())
            .collect(toList());

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

    return projectAggregateDTOBuilder.admins(adminUsers).collaborators(collaborators).build();
  }
}
