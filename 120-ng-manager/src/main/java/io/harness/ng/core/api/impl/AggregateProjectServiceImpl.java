package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.remote.OrganizationMapper.writeDto;
import static io.harness.ng.core.remote.ProjectMapper.toResponseWrapper;
import static io.harness.ng.core.user.remote.UserSearchMapper.writeDTO;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
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
import io.harness.ng.core.remote.ProjectMapper;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.Scope.ScopeKeys;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.service.NgUserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
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

    ProjectAggregateDTOBuilder projectAggregateDTO = ProjectAggregateDTO.builder();
    projectAggregateDTO.projectResponse(toResponseWrapper(projectOptional.get()));

    return buildProjectAggregateDTO(projectAggregateDTO, accountIdentifier, orgIdentifier, identifier);
  }

  private ProjectAggregateDTO buildProjectAggregateDTO(ProjectAggregateDTOBuilder projectAggregateDTOBuilder,
      String accountIdentifier, String orgIdentifier, String identifier) {
    // organization
    Optional<Organization> organizationOptional = organizationService.get(accountIdentifier, orgIdentifier);
    organizationOptional.ifPresent(organization -> {
      projectAggregateDTOBuilder.organization(writeDto(organization));
      projectAggregateDTOBuilder.harnessManagedOrg(Boolean.TRUE.equals(organization.getHarnessManaged()));
    });

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
    Criteria criteria = Criteria.where(UserMembershipKeys.scopes)
                            .elemMatch(Criteria.where(ScopeKeys.accountIdentifier)
                                           .is(accountIdentifier)
                                           .and(ScopeKeys.orgIdentifier)
                                           .is(orgIdentifier)
                                           .and(ScopeKeys.projectIdentifier)
                                           .is(identifier));
    List<UserMembership> userMemberships = ngUserService.listUserMemberships(criteria);
    List<String> userIds = userMemberships.stream().map(UserMembership::getUserId).collect(toList());
    Map<String, UserSearchDTO> userMap = getUserMap(userIds, accountIdentifier);
    List<UserSearchDTO> admins = getAdmins(accountIdentifier, orgIdentifier, identifier, userMap);
    return Pair.of(admins, removeAdmins(new ArrayList<>(userMap.values()), admins));
  }

  private List<UserSearchDTO> getAdmins(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Map<String, UserSearchDTO> userMap) {
    List<String> userIds = ngUserService.getUsersHavingRole(UserMembership.Scope.builder()
                                                                .accountIdentifier(accountIdentifier)
                                                                .orgIdentifier(orgIdentifier)
                                                                .projectIdentifier(projectIdentifier)
                                                                .build(),
        PROJECT_ADMIN_ROLE);
    return userIds.stream().filter(userMap::containsKey).map(userMap::get).collect(toList());
  }

  private Map<String, UserSearchDTO> getUserMap(List<String> userIds, String accountIdentifier) {
    List<UserInfo> users = ngUserService.getUsersByIds(userIds, accountIdentifier);
    Map<String, UserSearchDTO> userMap = new HashMap<>();
    users.forEach(user -> userMap.put(user.getUuid(), writeDTO(user)));
    return userMap;
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
    projectAggregateDTOs.forEach(projectAggregateDTO -> {
      String orgIdentifier = projectAggregateDTO.getProjectResponse().getProject().getOrgIdentifier();
      projectAggregateDTO.setOrganization(organizationMap.getOrDefault(orgIdentifier, null));
      projectAggregateDTO.setHarnessManagedOrg(organizationMap.containsKey(orgIdentifier)
          && Boolean.TRUE.equals(organizationMap.get(orgIdentifier).isHarnessManaged()));
    });

    // admins and collaborators
    try {
      addAdminsAndCollaborators(projectAggregateDTOs, accountIdentifier, projects);
    } catch (Exception exception) {
      log.error("Could not fetch Admins and Collaborators for projects in the account", exception);
    }
  }

  private void addAdminsAndCollaborators(
      Page<ProjectAggregateDTO> projectAggregateDTOs, String accountIdentifier, Page<ProjectResponse> projects) {
    List<UserMembership> userMemberships = getOrgUserMemberships(accountIdentifier, projects);
    List<String> userIds = userMemberships.stream().map(UserMembership::getUserId).collect(toList());
    Map<String, UserSearchDTO> userMap = getUserMap(userIds, accountIdentifier);
    Map<String, List<UserSearchDTO>> orgCollaboratorUserMap = getProjectCollaboratorMap(userMemberships, userMap);

    projectAggregateDTOs.forEach(projectAggregateDTO -> {
      String orgProjectId =
          getUniqueOrgProjectId(projectAggregateDTO.getProjectResponse().getProject().getOrgIdentifier(),
              projectAggregateDTO.getProjectResponse().getProject().getIdentifier());
      List<UserSearchDTO> collaborators = orgCollaboratorUserMap.getOrDefault(orgProjectId, new ArrayList<>());
      List<UserSearchDTO> admins =
          getAdmins(accountIdentifier, projectAggregateDTO.getProjectResponse().getProject().getOrgIdentifier(),
              projectAggregateDTO.getProjectResponse().getProject().getIdentifier(), userMap);
      projectAggregateDTO.setAdmins(admins);
      projectAggregateDTO.setCollaborators(removeAdmins(collaborators, admins));
    });
  }

  public static List<UserSearchDTO> removeAdmins(List<UserSearchDTO> collaborators, List<UserSearchDTO> admins) {
    Set<String> adminIds = admins.stream().map(UserSearchDTO::getUuid).collect(Collectors.toSet());
    return collaborators.stream().filter(collaborator -> !adminIds.contains(collaborator.getUuid())).collect(toList());
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

  private List<UserMembership> getOrgUserMemberships(String accountIdentifier, Page<ProjectResponse> projects) {
    List<Criteria> criteriaList = new ArrayList<>();
    projects.forEach(projectResponse -> {
      Criteria criteria = Criteria.where(UserMembershipKeys.scopes)
                              .elemMatch(Criteria.where(ScopeKeys.accountIdentifier)
                                             .is(accountIdentifier)
                                             .and(ScopeKeys.orgIdentifier)
                                             .is(projectResponse.getProject().getOrgIdentifier())
                                             .and(ScopeKeys.projectIdentifier)
                                             .is(projectResponse.getProject().getIdentifier()));
      criteriaList.add(criteria);
    });
    if (isEmpty(criteriaList)) {
      return new ArrayList<>();
    }
    return ngUserService.listUserMemberships(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
  }

  private Map<String, List<UserSearchDTO>> getProjectCollaboratorMap(
      List<UserMembership> userMemberships, Map<String, UserSearchDTO> userMap) {
    Map<String, List<UserSearchDTO>> orgProjectUserMap = new HashMap<>();
    userMemberships.forEach(userMembership
        -> userMembership.getScopes()
               .stream()
               .filter(scope -> scope.getOrgIdentifier() != null && scope.getProjectIdentifier() != null)
               .map(scope -> getUniqueOrgProjectId(scope.getOrgIdentifier(), scope.getProjectIdentifier()))
               .distinct()
               .forEach(orgProjectId -> {
                 orgProjectUserMap.computeIfAbsent(orgProjectId, arg -> new ArrayList<>());
                 if (userMap.containsKey(userMembership.getUserId())) {
                   orgProjectUserMap.get(orgProjectId).add(userMap.get(userMembership.getUserId()));
                 }
               }));
    return orgProjectUserMap;
  }

  private String getUniqueOrgProjectId(String orgIdentifier, String projectIdentifier) {
    return orgIdentifier + "/" + projectIdentifier;
  }
}
