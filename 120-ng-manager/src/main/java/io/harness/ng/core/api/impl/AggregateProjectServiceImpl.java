package io.harness.ng.core.api.impl;

import static io.harness.ng.core.invites.remote.UserSearchMapper.writeDTO;
import static io.harness.ng.core.remote.OrganizationMapper.writeDto;
import static io.harness.ng.core.remote.ProjectMapper.toResponseWrapper;

import io.harness.ng.core.api.AggregateProjectService;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.ProjectAggregateDTO;
import io.harness.ng.core.dto.ProjectAggregateDTO.ProjectAggregateDTOBuilder;
import io.harness.ng.core.dto.ProjectFilterDTO;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.invites.entities.UserProjectMap.UserProjectMapKeys;
import io.harness.ng.core.remote.ProjectMapper;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.User;
import io.harness.ng.core.user.services.api.NgUserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@Slf4j
public class AggregateProjectServiceImpl implements AggregateProjectService {
  private final ProjectService projectService;
  private final OrganizationService organizationService;
  private final NgUserService ngUserService;
  public static final String PROJECT_ADMIN_ROLE_NAME = "Project Admin";

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
    ProjectAggregateDTOBuilder projectAggregateDTO = ProjectAggregateDTO.builder();
    projectAggregateDTO.projectResponse(toResponseWrapper(projectOptional.get()));

    return buildProjectAggregateDTO(projectAggregateDTO, accountIdentifier, orgIdentifier, identifier);
  }

  private ProjectAggregateDTO buildProjectAggregateDTO(ProjectAggregateDTOBuilder projectAggregateDTOBuilder,
      String accountIdentifier, String orgIdentifier, String identifier) {
    // organization
    Optional<Organization> organizationOptional = organizationService.get(accountIdentifier, orgIdentifier);
    organizationOptional.ifPresent(organization -> projectAggregateDTOBuilder.organization(writeDto(organization)));

    // admins and collaborators
    try {
      Pair<List<UserSearchDTO>, List<UserSearchDTO>> projectUsers =
          getAdminsAndCollaborators(accountIdentifier, orgIdentifier, identifier);
      projectAggregateDTOBuilder.admins(projectUsers.getLeft());
      projectAggregateDTOBuilder.collaborators(projectUsers.getRight());
    } catch (Exception exception) {
      log.error(String.format(
                    "Could not fetch Admins and Collaborators for project with identifier [%s] and orgIdentifier [%s]",
                    identifier, orgIdentifier),
          exception);
    }

    return projectAggregateDTOBuilder.build();
  }

  private Pair<List<UserSearchDTO>, List<UserSearchDTO>> getAdminsAndCollaborators(
      String accountIdentifier, String orgIdentifier, String identifier) {
    Criteria userProjectMapCriteria = Criteria.where(UserProjectMapKeys.accountIdentifier)
                                          .is(accountIdentifier)
                                          .and(UserProjectMapKeys.orgIdentifier)
                                          .is(orgIdentifier)
                                          .and(UserProjectMapKeys.projectIdentifier)
                                          .is(identifier);
    List<UserProjectMap> userProjectMaps = ngUserService.listUserProjectMap(userProjectMapCriteria);
    List<String> userIds = userProjectMaps.stream().map(UserProjectMap::getUserId).collect(Collectors.toList());
    Map<String, UserSearchDTO> userMap = getUserMap(userIds);

    return Pair.of(getAdmins(userProjectMaps, userMap, PROJECT_ADMIN_ROLE_NAME),
        getCollaborators(userProjectMaps, userMap, PROJECT_ADMIN_ROLE_NAME));
  }

  private Map<String, UserSearchDTO> getUserMap(List<String> userIds) {
    List<User> users = ngUserService.getUsersByIds(userIds);
    Map<String, UserSearchDTO> userMap = new HashMap<>();
    users.forEach(user -> userMap.put(user.getUuid(), writeDTO(user)));
    return userMap;
  }

  public static List<UserSearchDTO> getAdmins(
      List<UserProjectMap> userProjectMaps, Map<String, UserSearchDTO> userMap, String roleName) {
    List<UserSearchDTO> admins = new ArrayList<>();
    userProjectMaps.forEach(userProjectMap -> {
      if (userProjectMap.getRoles().stream().anyMatch(role -> roleName.equals(role.getName()))) {
        if (userMap.get(userProjectMap.getUserId()) != null) {
          admins.add(userMap.get(userProjectMap.getUserId()));
        }
      }
    });
    return admins;
  }

  public static List<UserSearchDTO> getCollaborators(
      List<UserProjectMap> userProjectMaps, Map<String, UserSearchDTO> userMap, String roleName) {
    List<UserSearchDTO> collaborators = new ArrayList<>();
    userProjectMaps.forEach(userProjectMap -> {
      if (userProjectMap.getRoles().stream().noneMatch(role -> roleName.equals(role.getName()))) {
        if (userMap.get(userProjectMap.getUserId()) != null) {
          collaborators.add(userMap.get(userProjectMap.getUserId()));
        }
      }
    });
    return collaborators;
  }

  @Override
  public Page<ProjectAggregateDTO> listProjectAggregateDTO(
      String accountIdentifier, Pageable pageable, ProjectFilterDTO projectFilterDTO) {
    Page<ProjectResponse> projects =
        projectService.list(accountIdentifier, pageable, projectFilterDTO).map(ProjectMapper::toResponseWrapper);
    Page<ProjectAggregateDTO> projectAggregateDTOs =
        projects.map(projectResponse -> ProjectAggregateDTO.builder().projectResponse(projectResponse).build());

    buildProjectAggregateDTOPage(projectAggregateDTOs, accountIdentifier, projects);
    return projectAggregateDTOs;
  }

  private void buildProjectAggregateDTOPage(
      Page<ProjectAggregateDTO> projectAggregateDTOs, String accountIdentifier, Page<ProjectResponse> projects) {
    // organization
    Map<String, OrganizationDTO> organizationMap = getOrganizations(accountIdentifier, projects);
    projectAggregateDTOs.forEach(projectAggregateDTO
        -> projectAggregateDTO.setOrganization(organizationMap.getOrDefault(
            projectAggregateDTO.getProjectResponse().getProject().getOrgIdentifier(), null)));

    // admins and collaborators
    try {
      addAdminsAndCollaborators(projectAggregateDTOs, accountIdentifier, projects);
    } catch (Exception exception) {
      log.error("Could not fetch Admins and Collaborators for projects in the account", exception);
    }
  }

  private void addAdminsAndCollaborators(
      Page<ProjectAggregateDTO> projectAggregateDTOs, String accountIdentifier, Page<ProjectResponse> projects) {
    List<UserProjectMap> userProjectMaps = getOrgProjectUserMaps(accountIdentifier, projects);
    Map<String, List<UserProjectMap>> orgProjectUserMap = getOrgProjectUserMap(userProjectMaps);
    List<String> userIds = userProjectMaps.stream().map(UserProjectMap::getUserId).collect(Collectors.toList());
    Map<String, UserSearchDTO> userMap = getUserMap(userIds);

    projectAggregateDTOs.forEach(projectAggregateDTO -> {
      String orgProjectId =
          getUniqueOrgProjectId(projectAggregateDTO.getProjectResponse().getProject().getOrgIdentifier(),
              projectAggregateDTO.getProjectResponse().getProject().getIdentifier());
      List<UserProjectMap> userProjectMapList = orgProjectUserMap.getOrDefault(orgProjectId, new ArrayList<>());
      projectAggregateDTO.setAdmins(getAdmins(userProjectMapList, userMap, PROJECT_ADMIN_ROLE_NAME));
      projectAggregateDTO.setCollaborators(getCollaborators(userProjectMapList, userMap, PROJECT_ADMIN_ROLE_NAME));
    });
  }

  private Map<String, OrganizationDTO> getOrganizations(String accountIdentifier, Page<ProjectResponse> projects) {
    List<String> orgIdentifiers =
        projects.map(projectResponse -> projectResponse.getProject().getOrgIdentifier()).getContent();
    Criteria orgCriteria = Criteria.where(OrganizationKeys.accountIdentifier)
                               .is(accountIdentifier)
                               .and(OrganizationKeys.identifier)
                               .in(orgIdentifiers)
                               .and(OrganizationKeys.deleted)
                               .ne(Boolean.TRUE);
    List<Organization> organizations = organizationService.list(orgCriteria);
    Map<String, OrganizationDTO> organizationMap = new HashMap<>();
    organizations.forEach(organization -> organizationMap.put(organization.getIdentifier(), writeDto(organization)));
    return organizationMap;
  }

  private List<UserProjectMap> getOrgProjectUserMaps(String accountIdentifier, Page<ProjectResponse> projects) {
    List<String> orgIdentifiers =
        projects.map(projectResponse -> projectResponse.getProject().getOrgIdentifier()).getContent();
    List<String> projectIdentifiers =
        projects.map(projectResponse -> projectResponse.getProject().getIdentifier()).getContent();
    Criteria userProjectMapCriteria = Criteria.where(UserProjectMapKeys.accountIdentifier)
                                          .is(accountIdentifier)
                                          .and(UserProjectMapKeys.orgIdentifier)
                                          .in(orgIdentifiers)
                                          .and(UserProjectMapKeys.projectIdentifier)
                                          .in(projectIdentifiers);
    return ngUserService.listUserProjectMap(userProjectMapCriteria);
  }

  private Map<String, List<UserProjectMap>> getOrgProjectUserMap(List<UserProjectMap> userProjectMaps) {
    Map<String, List<UserProjectMap>> orgProjectUserMap = new HashMap<>();
    userProjectMaps.forEach(userProjectMap -> {
      String orgProjectId =
          getUniqueOrgProjectId(userProjectMap.getOrgIdentifier(), userProjectMap.getProjectIdentifier());
      if (!orgProjectUserMap.containsKey(orgProjectId)) {
        orgProjectUserMap.put(orgProjectId, new ArrayList<>());
      }
      orgProjectUserMap.get(orgProjectId).add(userProjectMap);
    });
    return orgProjectUserMap;
  }

  private String getUniqueOrgProjectId(String orgIdentifier, String projectIdentifier) {
    return orgIdentifier + "/" + projectIdentifier;
  }
}
