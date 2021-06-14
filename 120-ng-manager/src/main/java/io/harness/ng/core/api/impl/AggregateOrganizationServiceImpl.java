package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.Scope.ScopeKeys;
import io.harness.ng.core.api.AggregateOrganizationService;
import io.harness.ng.core.dto.OrganizationAggregateDTO;
import io.harness.ng.core.dto.OrganizationAggregateDTO.OrganizationAggregateDTOBuilder;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.invites.dto.UserMetadataDTO;
import io.harness.ng.core.remote.OrganizationMapper;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.service.NgUserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
  private final ExecutorService executorService;

  @Inject
  public AggregateOrganizationServiceImpl(
      OrganizationService organizationService, ProjectService projectService, NgUserService ngUserService) {
    this.organizationService = organizationService;
    this.projectService = projectService;
    this.ngUserService = ngUserService;
    this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 4);
  }

  @Override
  public OrganizationAggregateDTO getOrganizationAggregateDTO(String accountIdentifier, String identifier) {
    Optional<Organization> organizationOptional = organizationService.get(accountIdentifier, identifier);
    if (!organizationOptional.isPresent()) {
      throw new NotFoundException(String.format("Organization with identifier [%s] not found", identifier));
    }
    return buildAggregateDTO(organizationOptional.get());
  }

  private OrganizationAggregateDTO buildAggregateDTO(final Organization organization) {
    OrganizationAggregateDTOBuilder organizationAggregateDTOBuilder =
        OrganizationAggregateDTO.builder().organizationResponse(OrganizationMapper.toResponseWrapper(organization));

    int projectsCount = projectService
                            .getProjectsCountPerOrganization(
                                organization.getAccountIdentifier(), singletonList(organization.getIdentifier()))
                            .getOrDefault(organization.getIdentifier(), 0);
    organizationAggregateDTOBuilder.projectsCount(projectsCount);

    List<UserMetadataDTO> orgAdmins =
        ngUserService.listUsersHavingRole(Scope.builder()
                                              .accountIdentifier(organization.getAccountIdentifier())
                                              .orgIdentifier(organization.getIdentifier())
                                              .build(),
            ORG_ADMIN_ROLE);

    organizationAggregateDTOBuilder.admins(orgAdmins);

    List<UserMembership> userMemberships =
        ngUserService.listUserMemberships(Criteria.where(UserMembershipKeys.scopes)
                                              .elemMatch(Criteria.where(ScopeKeys.accountIdentifier)
                                                             .is(organization.getAccountIdentifier())
                                                             .and(ScopeKeys.orgIdentifier)
                                                             .is(organization.getIdentifier())
                                                             .and(ScopeKeys.projectIdentifier)
                                                             .is(null)));

    List<UserMetadataDTO> collaborators =
        userMemberships.stream()
            .filter(x
                -> !orgAdmins.stream().map(UserMetadataDTO::getUuid).collect(Collectors.toSet()).contains(x.getUuid()))
            .map(userMembership
                -> UserMetadataDTO.builder()
                       .name(userMembership.getName())
                       .email(userMembership.getName())
                       .uuid(userMembership.getUuid())
                       .build())
            .collect(Collectors.toList());

    return organizationAggregateDTOBuilder.collaborators(collaborators).build();
  }

  @Override
  public Page<OrganizationAggregateDTO> listOrganizationAggregateDTO(
      String accountIdentifier, Pageable pageable, OrganizationFilterDTO organizationFilterDTO) {
    Page<Organization> organizations = organizationService.list(accountIdentifier, pageable, organizationFilterDTO);

    List<Future<OrganizationAggregateDTO>> futures = new ArrayList<>();
    List<OrganizationAggregateDTO> aggregates = new ArrayList<>();
    organizations.forEach(org -> futures.add(executorService.submit(() -> buildAggregateDTO(org))));

    for (int i = 0; i < futures.size(); i++) {
      try {
        aggregates.add(futures.get(i).get());
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        log.error("Error while computing aggregate", e);
        aggregates.add(
            OrganizationAggregateDTO.builder()
                .organizationResponse(OrganizationMapper.toResponseWrapper(organizations.getContent().get(i)))
                .build());
      }
    }

    return new PageImpl<>(aggregates, organizations.getPageable(), organizations.getTotalElements());
  }
}
