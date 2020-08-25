package io.harness.ng.core.impl;

import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.NextGenModule.SECRET_MANAGER_CONNECTOR_SERVICE;
import static io.harness.ng.core.utils.NGUtils.getConnectorRequestDTO;
import static io.harness.ng.core.utils.NGUtils.getDefaultHarnessSecretManagerName;
import static io.harness.secretmanagerclient.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.harness.connector.services.ConnectorService;
import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.ng.core.api.repositories.spring.OrganizationRepository;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.services.OrganizationService;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import software.wings.service.impl.security.SecretManagementException;

import java.util.Objects;
import java.util.Optional;
import javax.validation.constraints.NotNull;

@Singleton
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {
  private final OrganizationRepository organizationRepository;
  private final NGSecretManagerService ngSecretManagerService;
  private final ConnectorService secretManagerConnectorService;

  void validatePresenceOfRequiredFields(Object... fields) {
    Lists.newArrayList(fields).forEach(field -> Objects.requireNonNull(field, "One of the required fields is null."));
  }

  @Inject
  public OrganizationServiceImpl(OrganizationRepository organizationRepository,
      NGSecretManagerService ngSecretManagerService,
      @Named(SECRET_MANAGER_CONNECTOR_SERVICE) ConnectorService secretManagerConnectorService) {
    this.organizationRepository = organizationRepository;
    this.ngSecretManagerService = ngSecretManagerService;
    this.secretManagerConnectorService = secretManagerConnectorService;
  }

  @Override
  public Organization create(Organization organization) {
    try {
      Organization savedOrganization = organizationRepository.save(organization);
      performActionsPostOrganizationCreation(organization);
      return savedOrganization;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(String.format("Organization [%s] under account [%s] already exists",
                                            organization.getIdentifier(), organization.getAccountIdentifier()),
          USER_SRE, ex);
    }
  }

  private void performActionsPostOrganizationCreation(Organization organization) {
    createHarnessSecretManager(organization);
  }

  private void createHarnessSecretManager(Organization organization) {
    try {
      SecretManagerConfigDTO globalSecretManager =
          ngSecretManagerService.getGlobalSecretManager(organization.getAccountIdentifier());
      globalSecretManager.setIdentifier(HARNESS_SECRET_MANAGER_IDENTIFIER);
      globalSecretManager.setDescription("Organisation: " + organization.getName());
      globalSecretManager.setName(getDefaultHarnessSecretManagerName(globalSecretManager.getEncryptionType()));
      globalSecretManager.setProjectIdentifier(null);
      globalSecretManager.setOrgIdentifier(organization.getIdentifier());
      globalSecretManager.setDefault(false);
      secretManagerConnectorService.create(
          getConnectorRequestDTO(globalSecretManager), organization.getAccountIdentifier());
    } catch (Exception ex) {
      throw new SecretManagementException(SECRET_MANAGEMENT_ERROR,
          String.format("Harness Secret Manager for organisation %s could not be created", organization.getName()), ex,
          USER);
    }
  }

  @Override
  public Optional<Organization> get(String accountIdentifier, String organizationIdentifier) {
    return organizationRepository.findByAccountIdentifierAndIdentifierAndDeletedNot(
        accountIdentifier, organizationIdentifier, true);
  }

  @Override
  public Organization update(Organization organization) {
    validatePresenceOfRequiredFields(
        organization.getAccountIdentifier(), organization.getIdentifier(), organization.getId());
    return organizationRepository.save(organization);
  }

  @Override
  public Page<Organization> list(@NotNull Criteria criteria, Pageable pageable) {
    return organizationRepository.findAll(criteria, pageable);
  }

  @Override
  public boolean delete(String accountIdentifier, String organizationIdentifier) {
    Optional<Organization> organizationOptional = get(accountIdentifier, organizationIdentifier);
    if (organizationOptional.isPresent()) {
      Organization organization = organizationOptional.get();
      organization.setDeleted(Boolean.TRUE);
      organizationRepository.save(organization);
      return true;
    }
    return false;
  }
}
