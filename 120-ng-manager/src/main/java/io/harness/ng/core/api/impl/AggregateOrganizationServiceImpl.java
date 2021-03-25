package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.api.impl.AggregateProjectServiceImpl.removeAdmins;
import static io.harness.ng.core.invites.remote.UserSearchMapper.writeDTO;
import static io.harness.ng.core.remote.OrganizationMapper.toResponseWrapper;
import static io.harness.ng.core.remote.ProjectMapper.writeDTO;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.AggregateOrganizationService;
import io.harness.ng.core.dto.OrganizationAggregateDTO;
import io.harness.ng.core.dto.OrganizationAggregateDTO.OrganizationAggregateDTOBuilder;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.dto.OrganizationResponse;
import io.harness.ng.core.dto.ProjectDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.entities.Project.ProjectKeys;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.invites.entities.UserMembership;
import io.harness.ng.core.invites.entities.UserMembership.Scope.ScopeKeys;
import io.harness.ng.core.invites.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.remote.OrganizationMapper;
import io.harness.ng.core.remote.ProjectMapper;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.services.api.NgUserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
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

@OwnedBy(PL)
@Singleton
@Slf4j
public class AggregateOrganizationServiceImpl implements AggregateOrganizationService {
  private static final String ORG_ADMIN_ROLE = "_organization_admin";
  private final OrganizationService organizationService;
  private final ProjectService projectService;
  private final NgUserService ngUserService;

  @Inject
  public AggregateOrganizationServiceImpl(
      OrganizationService organizationService, ProjectService projectService, NgUserService ngUserService) {
    this.organizationService = organizationService;
    this.projectService = projectService;
    this.ngUserService = ngUserService;
  }

  @Override
  public OrganizationAggregateDTO getOrganizationAggregateDTO(String accountIdentifier, String identifier) {
    Optional<Organization> organizationOptional = organizationService.get(accountIdentifier, identifier);
    if (!organizationOptional.isPresent()) {
      throw new NotFoundException(String.format("Organization with identifier [%s] not found", identifier));
    }
    OrganizationAggregateDTOBuilder organizationAggregateDTO = OrganizationAggregateDTO.builder();
    organizationAggregateDTO.organizationResponse(toResponseWrapper(organizationOptional.get()));

    return buildOrganizationAggregateDTO(organizationAggregateDTO, accountIdentifier, identifier);
  }

  private OrganizationAggregateDTO buildOrganizationAggregateDTO(
      OrganizationAggregateDTOBuilder organizationAggregateDTOBuilder, String accountIdentifier, String identifier) {
    // projects
    List<ProjectDTO> projectDTOs = getProjects(accountIdentifier, identifier);
    organizationAggregateDTOBuilder.projectsCount(projectDTOs.size());

    // admins and collaborators
    try {
      Pair<List<UserSearchDTO>, List<UserSearchDTO>> orgUsers =
          getAdminsAndCollaborators(accountIdentifier, identifier);
      organizationAggregateDTOBuilder.admins(orgUsers.getLeft());
      organizationAggregateDTOBuilder.collaborators(orgUsers.getRight());
    } catch (Exception exception) {
      log.error(
          String.format("Could not fetch Admins and Collaborators for organization with identifier [%s]", identifier),
          exception);
    }

    return organizationAggregateDTOBuilder.build();
  }

  private List<ProjectDTO> getProjects(String accountIdentifier, String identifier) {
    Criteria criteria = Criteria.where(ProjectKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(ProjectKeys.orgIdentifier)
                            .is(identifier)
                            .and(ProjectKeys.deleted)
                            .ne(Boolean.TRUE);
    return projectService.list(criteria).stream().map(ProjectMapper::writeDTO).collect(Collectors.toList());
  }

  private Pair<List<UserSearchDTO>, List<UserSearchDTO>> getAdminsAndCollaborators(
      String accountIdentifier, String identifier) {
    Criteria userOrganizationMapCriteria = Criteria.where(UserMembershipKeys.scopes + "." + ScopeKeys.accountIdentifier)
                                               .is(accountIdentifier)
                                               .and(UserMembershipKeys.scopes + "." + ScopeKeys.orgIdentifier)
                                               .is(identifier)
                                               .and(UserMembershipKeys.scopes + "." + ScopeKeys.projectIdentifier)
                                               .is(null);
    List<UserMembership> userMemberships = ngUserService.listUserMemberships(userOrganizationMapCriteria);
    List<String> userIds = userMemberships.stream().map(UserMembership::getUserId).collect(Collectors.toList());
    Map<String, UserSearchDTO> userMap = getUserMap(userIds);
    List<UserSearchDTO> collaborators = new ArrayList<>(userMap.values());
    List<UserSearchDTO> admins = getAdmins(accountIdentifier, identifier, userMap);
    return Pair.of(admins, removeAdmins(collaborators, admins));
  }

  private Map<String, UserSearchDTO> getUserMap(List<String> userIds) {
    List<UserInfo> users = ngUserService.getUsersByIds(userIds);
    Map<String, UserSearchDTO> userMap = new HashMap<>();
    users.forEach(user -> userMap.put(user.getUuid(), writeDTO(user)));
    return userMap;
  }

  @Override
  public Page<OrganizationAggregateDTO> listOrganizationAggregateDTO(
      String accountIdentifier, Pageable pageable, OrganizationFilterDTO organizationFilterDTO) {
    Page<OrganizationResponse> organizations =
        organizationService.list(accountIdentifier, pageable, organizationFilterDTO)
            .map(OrganizationMapper::toResponseWrapper);
    Page<OrganizationAggregateDTO> organizationAggregateDTOs = organizations.map(
        organizationResponse -> OrganizationAggregateDTO.builder().organizationResponse(organizationResponse).build());

    buildOrganizationAggregateDTOPage(organizationAggregateDTOs, accountIdentifier, organizations);
    return organizationAggregateDTOs;
  }

  private void buildOrganizationAggregateDTOPage(Page<OrganizationAggregateDTO> organizationAggregateDTOs,
      String accountIdentifier, Page<OrganizationResponse> organizations) {
    // projects
    Map<String, List<ProjectDTO>> projectMap = getProjects(accountIdentifier, organizations);
    organizationAggregateDTOs.forEach(organizationAggregateDTO
        -> organizationAggregateDTO.setProjectsCount(
            projectMap
                .getOrDefault(organizationAggregateDTO.getOrganizationResponse().getOrganization().getIdentifier(),
                    new ArrayList<>())
                .size()));

    // admins and collaborators
    try {
      addAdminsAndCollaborators(organizationAggregateDTOs, accountIdentifier, organizations);
    } catch (Exception exception) {
      log.error("Could not fetch Org Members for Organizations in the account", exception);
    }
  }

  private Map<String, List<ProjectDTO>> getProjects(
      String accountIdentifier, Page<OrganizationResponse> organizations) {
    List<String> orgIdentifiers =
        organizations.map(organizationResponse -> organizationResponse.getOrganization().getIdentifier()).getContent();
    Criteria projectCriteria = Criteria.where(ProjectKeys.accountIdentifier)
                                   .is(accountIdentifier)
                                   .and(ProjectKeys.orgIdentifier)
                                   .in(orgIdentifiers)
                                   .and(ProjectKeys.deleted)
                                   .ne(Boolean.TRUE);
    List<Project> projects = projectService.list(projectCriteria);
    Map<String, List<ProjectDTO>> projectMap = new HashMap<>();
    projects.forEach(project -> {
      if (!projectMap.containsKey(project.getOrgIdentifier())) {
        projectMap.put(project.getOrgIdentifier(), new ArrayList<>());
      }
      projectMap.get(project.getOrgIdentifier()).add(writeDTO(project));
    });
    return projectMap;
  }

  private void addAdminsAndCollaborators(Page<OrganizationAggregateDTO> organizationAggregateDTOs,
      String accountIdentifier, Page<OrganizationResponse> organizations) {
    List<UserMembership> userMemberships = getOrgUserProjectMaps(accountIdentifier, organizations);
    List<String> userIds = userMemberships.stream().map(UserMembership::getUserId).collect(Collectors.toList());
    Map<String, UserSearchDTO> userMap = getUserMap(userIds);
    Map<String, List<UserSearchDTO>> orgCollaboratorMap = getOrgCollaboratorMap(userMemberships, userMap);
    organizationAggregateDTOs.forEach(organizationAggregateDTO -> {
      String orgId = organizationAggregateDTO.getOrganizationResponse().getOrganization().getIdentifier();
      List<UserSearchDTO> admins = getAdmins(accountIdentifier, orgId, userMap);
      organizationAggregateDTO.setAdmins(admins);
      organizationAggregateDTO.setCollaborators(
          removeAdmins(orgCollaboratorMap.getOrDefault(orgId, Collections.emptyList()), admins));
    });
  }

  private List<UserSearchDTO> getAdmins(String accountIdentifier, String orgId, Map<String, UserSearchDTO> userMap) {
    List<String> userIds = ngUserService.getUsersHavingRole(
        UserMembership.Scope.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgId).build(),
        ORG_ADMIN_ROLE);
    return userIds.stream().filter(userMap::containsKey).map(userMap::get).collect(Collectors.toList());
  }

  private List<UserMembership> getOrgUserProjectMaps(
      String accountIdentifier, Page<OrganizationResponse> organizations) {
    List<String> orgIdentifiers =
        organizations.map(organizationResponse -> organizationResponse.getOrganization().getIdentifier()).getContent();
    Criteria userProjectMapCriteria = Criteria.where(UserMembershipKeys.scopes + "." + ScopeKeys.accountIdentifier)
                                          .is(accountIdentifier)
                                          .and(UserMembershipKeys.scopes + "." + ScopeKeys.orgIdentifier)
                                          .in(orgIdentifiers)
                                          .and(UserMembershipKeys.scopes + "." + ScopeKeys.projectIdentifier)
                                          .is(null);
    return ngUserService.listUserMemberships(userProjectMapCriteria);
  }

  private Map<String, List<UserSearchDTO>> getOrgCollaboratorMap(
      List<UserMembership> userMemberships, Map<String, UserSearchDTO> userMap) {
    Map<String, List<UserSearchDTO>> orgProjectUserMap = new HashMap<>();
    userMemberships.forEach(userMembership -> userMembership.getScopes().forEach(scope -> {
      if (!orgProjectUserMap.containsKey(scope.getOrgIdentifier())) {
        orgProjectUserMap.put(scope.getOrgIdentifier(), new ArrayList<>());
      }
      if (userMap.containsKey(userMembership.getUserId())) {
        orgProjectUserMap.get(scope.getOrgIdentifier()).add(userMap.get(userMembership.getUserId()));
      }
    }));
    return orgProjectUserMap;
  }
}
