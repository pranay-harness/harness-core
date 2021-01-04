package io.harness.ng.core.api.impl;

import static io.harness.ng.core.api.impl.AggregateProjectServiceImpl.getAdmins;
import static io.harness.ng.core.api.impl.AggregateProjectServiceImpl.getCollaborators;
import static io.harness.ng.core.invites.remote.UserSearchMapper.writeDTO;
import static io.harness.ng.core.remote.OrganizationMapper.toResponseWrapper;
import static io.harness.ng.core.remote.ProjectMapper.writeDTO;

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
import io.harness.ng.core.invites.entities.UserProjectMap;
import io.harness.ng.core.invites.entities.UserProjectMap.UserProjectMapKeys;
import io.harness.ng.core.remote.OrganizationMapper;
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
public class AggregateOrganizationServiceImpl implements AggregateOrganizationService {
  private final OrganizationService organizationService;
  private final ProjectService projectService;
  private final NgUserService ngUserService;
  public static final String ORGANIZATION_ADMIN_ROLE_NAME = "Organization Admin";

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
    Criteria userOrganizationMapCriteria = Criteria.where(UserProjectMapKeys.accountIdentifier)
                                               .is(accountIdentifier)
                                               .and(UserProjectMapKeys.orgIdentifier)
                                               .is(identifier)
                                               .and(UserProjectMapKeys.projectIdentifier)
                                               .is(null);
    List<UserProjectMap> userOrganizationMaps = ngUserService.listUserProjectMap(userOrganizationMapCriteria);
    List<String> userIds = userOrganizationMaps.stream().map(UserProjectMap::getUserId).collect(Collectors.toList());
    Map<String, UserSearchDTO> userMap = getUserMap(userIds);
    return Pair.of(getAdmins(userOrganizationMaps, userMap, ORGANIZATION_ADMIN_ROLE_NAME),
        getCollaborators(userOrganizationMaps, userMap, ORGANIZATION_ADMIN_ROLE_NAME));
  }

  private Map<String, UserSearchDTO> getUserMap(List<String> userIds) {
    List<User> users = ngUserService.getUsersByIds(userIds);
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
    List<UserProjectMap> userProjectMaps = getOrgUserProjectMaps(accountIdentifier, organizations);
    Map<String, List<UserProjectMap>> orgUserMap = getOrgUserMap(userProjectMaps);
    List<String> userIds = userProjectMaps.stream().map(UserProjectMap::getUserId).collect(Collectors.toList());
    Map<String, UserSearchDTO> userMap = getUserMap(userIds);

    organizationAggregateDTOs.forEach(organizationAggregateDTO -> {
      String orgId = organizationAggregateDTO.getOrganizationResponse().getOrganization().getIdentifier();
      List<UserProjectMap> userProjectMapList = orgUserMap.getOrDefault(orgId, new ArrayList<>());
      organizationAggregateDTO.setAdmins(getAdmins(userProjectMapList, userMap, ORGANIZATION_ADMIN_ROLE_NAME));
      organizationAggregateDTO.setCollaborators(
          getCollaborators(userProjectMapList, userMap, ORGANIZATION_ADMIN_ROLE_NAME));
    });
  }

  private List<UserProjectMap> getOrgUserProjectMaps(
      String accountIdentifier, Page<OrganizationResponse> organizations) {
    List<String> orgIdentifiers =
        organizations.map(organizationResponse -> organizationResponse.getOrganization().getIdentifier()).getContent();
    Criteria userProjectMapCriteria = Criteria.where(UserProjectMapKeys.accountIdentifier)
                                          .is(accountIdentifier)
                                          .and(UserProjectMapKeys.orgIdentifier)
                                          .in(orgIdentifiers)
                                          .and(UserProjectMapKeys.projectIdentifier)
                                          .is(null);
    return ngUserService.listUserProjectMap(userProjectMapCriteria);
  }

  private Map<String, List<UserProjectMap>> getOrgUserMap(List<UserProjectMap> userProjectMaps) {
    Map<String, List<UserProjectMap>> orgProjectUserMap = new HashMap<>();
    userProjectMaps.forEach(userProjectMap -> {
      if (!orgProjectUserMap.containsKey(userProjectMap.getOrgIdentifier())) {
        orgProjectUserMap.put(userProjectMap.getOrgIdentifier(), new ArrayList<>());
      }
      orgProjectUserMap.get(userProjectMap.getOrgIdentifier()).add(userProjectMap);
    });
    return orgProjectUserMap;
  }
}
