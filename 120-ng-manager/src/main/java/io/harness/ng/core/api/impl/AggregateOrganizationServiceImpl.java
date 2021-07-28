package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.connector.services.ConnectorService;
import io.harness.ng.core.api.AggregateOrganizationService;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.dto.OrganizationAggregateDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.remote.OrganizationMapper;
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
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@OwnedBy(PL)
@Singleton
@Slf4j
public class AggregateOrganizationServiceImpl implements AggregateOrganizationService {
  private static final String ORG_ADMIN_ROLE = "_organization_admin";
  private final OrganizationService organizationService;
  private final ProjectService projectService;
  private final NGSecretServiceV2 secretServiceV2;
  private final ConnectorService defaultConnectorService;
  private final NgUserService ngUserService;
  private final ExecutorService executorService;

  @Inject
  public AggregateOrganizationServiceImpl(OrganizationService organizationService, ProjectService projectService,
      NGSecretServiceV2 secretService, @Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService defaultConnectorService,
      NgUserService ngUserService, @Named("aggregate-orgs") ExecutorService executorService) {
    this.organizationService = organizationService;
    this.projectService = projectService;
    this.secretServiceV2 = secretService;
    this.defaultConnectorService = defaultConnectorService;
    this.ngUserService = ngUserService;
    this.executorService = executorService;
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
    int projectsCount = projectService
                            .getProjectsCountPerOrganization(
                                organization.getAccountIdentifier(), singletonList(organization.getIdentifier()))
                            .getOrDefault(organization.getIdentifier(), 0);
    long secretsCount = secretServiceV2.count(organization.getAccountIdentifier(), organization.getIdentifier(), null);
    long connectorsCount =
        defaultConnectorService.count(organization.getAccountIdentifier(), organization.getIdentifier(), null);

    Scope scope = Scope.builder()
                      .accountIdentifier(organization.getAccountIdentifier())
                      .orgIdentifier(organization.getIdentifier())
                      .build();
    List<UserMetadataDTO> orgAdmins = ngUserService.listUsersHavingRole(scope, ORG_ADMIN_ROLE);
    List<UserMetadataDTO> collaborators = ngUserService.listUsers(scope);
    collaborators.removeAll(orgAdmins);

    return OrganizationAggregateDTO.builder()
        .organizationResponse(OrganizationMapper.toResponseWrapper(organization))
        .projectsCount(projectsCount)
        .admins(orgAdmins)
        .secretsCount(secretsCount)
        .connectorsCount(connectorsCount)
        .collaborators(collaborators)
        .build();
  }

  @Override
  public Page<OrganizationAggregateDTO> listOrganizationAggregateDTO(
      String accountIdentifier, Pageable pageable, OrganizationFilterDTO organizationFilterDTO) {
    Page<Organization> organizations = organizationService.list(accountIdentifier, pageable, organizationFilterDTO);
    List<Organization> organizationList = organizations.toList();

    List<Callable<OrganizationAggregateDTO>> tasks = new ArrayList<>();
    organizations.forEach(org -> tasks.add(() -> buildAggregateDTO(org)));

    List<Future<OrganizationAggregateDTO>> futures;
    try {
      futures = executorService.invokeAll(tasks, 10, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Page.empty();
    }

    List<OrganizationAggregateDTO> aggregates = new ArrayList<>();
    for (int i = 0; i < futures.size(); i++) {
      try {
        aggregates.add(futures.get(i).get());
      } catch (CancellationException e) {
        log.error("Org aggregate task cancelled for [{}/{}]", organizationList.get(i).getAccountIdentifier(),
            organizationList.get(i).getIdentifier(), e);
        aggregates.add(OrganizationAggregateDTO.builder()
                           .organizationResponse(OrganizationMapper.toResponseWrapper(organizationList.get(i)))
                           .build());
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
        return Page.empty();
      } catch (ExecutionException e) {
        log.error("Error occurred while computing aggregate for org [{}/{}]",
            organizationList.get(i).getAccountIdentifier(), organizationList.get(i).getIdentifier(), e);
        aggregates.add(OrganizationAggregateDTO.builder()
                           .organizationResponse(OrganizationMapper.toResponseWrapper(organizationList.get(i)))
                           .build());
      }
    }

    return new PageImpl<>(aggregates, organizations.getPageable(), organizations.getTotalElements());
  }
}
