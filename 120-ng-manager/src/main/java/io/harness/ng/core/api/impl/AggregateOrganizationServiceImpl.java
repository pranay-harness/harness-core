package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.ng.core.api.impl.AggregateProjectServiceImpl.removeAdmins;
import static io.harness.ng.core.remote.OrganizationMapper.toResponseWrapper;
import static io.harness.ng.core.user.remote.mapper.UserSearchMapper.writeDTO;

import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.AggregateOrganizationService;
import io.harness.ng.core.dto.OrganizationAggregateDTO;
import io.harness.ng.core.dto.OrganizationAggregateDTO.OrganizationAggregateDTOBuilder;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.dto.OrganizationResponse;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.invites.dto.UserSearchDTO;
import io.harness.ng.core.remote.OrganizationMapper;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.Scope;
import io.harness.ng.core.user.entities.UserMembership.Scope.ScopeKeys;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.service.NgUserService;

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
    int projectsCount = getProjectsCountForOrganization(accountIdentifier, identifier);
    organizationAggregateDTOBuilder.projectsCount(projectsCount);

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

  private int getProjectsCountForOrganization(String accountIdentifier, String orgIdentifier) {
    return projectService.getProjectsCountPerOrganization(accountIdentifier, singletonList(orgIdentifier))
        .getOrDefault(orgIdentifier, 0);
  }

  private Pair<List<UserSearchDTO>, List<UserSearchDTO>> getAdminsAndCollaborators(
      String accountIdentifier, String identifier) {
    Criteria userMembershipCriteria = Criteria.where(UserMembershipKeys.scopes)
                                          .elemMatch(Criteria.where(ScopeKeys.accountIdentifier)
                                                         .is(accountIdentifier)
                                                         .and(ScopeKeys.orgIdentifier)
                                                         .is(identifier)
                                                         .and(ScopeKeys.projectIdentifier)
                                                         .is(null));
    List<UserMembership> userMemberships = ngUserService.listUserMemberships(userMembershipCriteria);
    List<String> userIds = userMemberships.stream().map(UserMembership::getUserId).collect(Collectors.toList());
    Map<String, UserSearchDTO> userMap = getUserMap(userIds, accountIdentifier);
    List<UserSearchDTO> collaborators = new ArrayList<>(userMap.values());
    List<UserSearchDTO> admins = getAdmins(accountIdentifier, identifier, userMap);
    return Pair.of(admins, removeAdmins(collaborators, admins));
  }

  private Map<String, UserSearchDTO> getUserMap(List<String> userIds, String accountIdentifier) {
    List<UserInfo> users = ngUserService.getUsersByIds(userIds, accountIdentifier);
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
    Map<String, Integer> projectMap = getProjectsCountPerOrganization(accountIdentifier, organizations);
    organizationAggregateDTOs.forEach(organizationAggregateDTO -> {
      Integer count = Optional
                          .ofNullable(projectMap.get(
                              organizationAggregateDTO.getOrganizationResponse().getOrganization().getIdentifier()))
                          .orElse(0);
      organizationAggregateDTO.setProjectsCount(count);
    });

    // admins and collaborators
    try {
      addAdminsAndCollaborators(organizationAggregateDTOs, accountIdentifier, organizations);
    } catch (Exception exception) {
      log.error("Could not fetch Org Members for Organizations in the account", exception);
    }
  }

  private Map<String, Integer> getProjectsCountPerOrganization(
      String accountIdentifier, Page<OrganizationResponse> organizations) {
    List<String> orgIdentifiers =
        organizations.map(organizationResponse -> organizationResponse.getOrganization().getIdentifier()).getContent();
    return projectService.getProjectsCountPerOrganization(accountIdentifier, orgIdentifiers);
  }

  private void addAdminsAndCollaborators(Page<OrganizationAggregateDTO> organizationAggregateDTOs,
      String accountIdentifier, Page<OrganizationResponse> organizations) {
    List<UserMembership> userMemberships = getUserMemberships(accountIdentifier, organizations);
    List<String> userIds = userMemberships.stream().map(UserMembership::getUserId).collect(Collectors.toList());
    Map<String, UserSearchDTO> userMap = getUserMap(userIds, accountIdentifier);
    Map<String, List<UserSearchDTO>> orgUsersMap = getOrgUsersMap(userMemberships, userMap);
    Map<String, List<UserSearchDTO>> orgAdminsMap = getOrgAdminsMap(accountIdentifier, organizations, userMap);
    organizationAggregateDTOs.forEach(organizationAggregateDTO -> {
      String orgId = organizationAggregateDTO.getOrganizationResponse().getOrganization().getIdentifier();
      List<UserSearchDTO> admins = orgAdminsMap.getOrDefault(orgId, new ArrayList<>());
      organizationAggregateDTO.setAdmins(admins);
      organizationAggregateDTO.setCollaborators(
          removeAdmins(orgUsersMap.getOrDefault(orgId, Collections.emptyList()), admins));
    });
  }

  private Map<String, List<UserSearchDTO>> getOrgAdminsMap(
      String accountIdentifier, Page<OrganizationResponse> organizations, Map<String, UserSearchDTO> userMap) {
    Map<String, List<UserSearchDTO>> orgAdminsMap = new HashMap<>();
    List<Scope> scopes = new ArrayList<>();
    organizations.forEach(organizationResponse
        -> scopes.add(Scope.builder()
                          .accountIdentifier(accountIdentifier)
                          .orgIdentifier(organizationResponse.getOrganization().getIdentifier())
                          .projectIdentifier(null)
                          .build()));
    /*
    Performance can be improved by a batch call with multiple scopes as input.
     */
    scopes.forEach(scope
        -> orgAdminsMap.put(scope.getOrgIdentifier(), getAdmins(accountIdentifier, scope.getOrgIdentifier(), userMap)));
    return orgAdminsMap;
  }

  private List<UserSearchDTO> getAdmins(String accountIdentifier, String orgId, Map<String, UserSearchDTO> userMap) {
    List<String> userIds = ngUserService.getUserIdsWithRole(
        UserMembership.Scope.builder().accountIdentifier(accountIdentifier).orgIdentifier(orgId).build(),
        ORG_ADMIN_ROLE);
    return userIds.stream().filter(userMap::containsKey).map(userMap::get).collect(Collectors.toList());
  }

  private List<UserMembership> getUserMemberships(String accountIdentifier, Page<OrganizationResponse> organizations) {
    List<Criteria> criteriaList = new ArrayList<>();
    organizations.forEach(organizationResponse -> {
      Criteria criteria = Criteria.where(UserMembershipKeys.scopes)
                              .elemMatch(Criteria.where(ScopeKeys.accountIdentifier)
                                             .is(accountIdentifier)
                                             .and(ScopeKeys.orgIdentifier)
                                             .is(organizationResponse.getOrganization().getIdentifier())
                                             .and(ScopeKeys.projectIdentifier)
                                             .is(null));
      criteriaList.add(criteria);
    });
    if (isEmpty(criteriaList)) {
      return new ArrayList<>();
    }
    return ngUserService.listUserMemberships(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])));
  }

  private Map<String, List<UserSearchDTO>> getOrgUsersMap(
      List<UserMembership> userMemberships, Map<String, UserSearchDTO> userMap) {
    Map<String, List<UserSearchDTO>> orgProjectUserMap = new HashMap<>();
    userMemberships.forEach(userMembership
        -> userMembership.getScopes()
               .stream()
               .filter(scope -> scope.getOrgIdentifier() != null)
               .map(UserMembership.Scope::getOrgIdentifier)
               .distinct()
               .forEach(orgIdentifier -> {
                 orgProjectUserMap.computeIfAbsent(orgIdentifier, arg -> new ArrayList<>());
                 if (userMap.containsKey(userMembership.getUserId())) {
                   orgProjectUserMap.get(orgIdentifier).add(userMap.get(userMembership.getUserId()));
                 }
               }));
    return orgProjectUserMap;
  }
}
