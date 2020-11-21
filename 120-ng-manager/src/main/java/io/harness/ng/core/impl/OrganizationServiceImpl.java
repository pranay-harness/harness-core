package io.harness.ng.core.impl;

import static io.harness.NGConstants.HARNESS_SECRET_MANAGER_IDENTIFIER;
import static io.harness.eraro.ErrorCode.SECRET_MANAGEMENT_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.NextGenModule.SECRET_MANAGER_CONNECTOR_SERVICE;
import static io.harness.ng.core.remote.OrganizationMapper.toOrganization;
import static io.harness.ng.core.utils.NGUtils.getConnectorRequestDTO;
import static io.harness.ng.core.utils.NGUtils.getDefaultHarnessSecretManagerName;
import static io.harness.ng.core.utils.NGUtils.validate;
import static io.harness.ng.core.utils.NGUtils.verifyValuesNotChanged;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.NGCommonEntityConstants;
import io.harness.connector.services.ConnectorService;
import io.harness.data.validator.EntityNameValidator;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.SecretManagementException;
import io.harness.ng.core.api.NGSecretManagerService;
import io.harness.ng.core.api.repositories.spring.OrganizationRepository;
import io.harness.ng.core.dto.OrganizationDTO;
import io.harness.ng.core.dto.OrganizationFilterDTO;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Organization.OrganizationKeys;
import io.harness.ng.core.services.OrganizationService;
import io.harness.secretmanagerclient.dto.SecretManagerConfigDTO;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {
  private final OrganizationRepository organizationRepository;
  private final NGSecretManagerService ngSecretManagerService;
  private final ConnectorService secretManagerConnectorService;

  @Inject
  public OrganizationServiceImpl(OrganizationRepository organizationRepository,
      NGSecretManagerService ngSecretManagerService,
      @Named(SECRET_MANAGER_CONNECTOR_SERVICE) ConnectorService secretManagerConnectorService) {
    this.organizationRepository = organizationRepository;
    this.ngSecretManagerService = ngSecretManagerService;
    this.secretManagerConnectorService = secretManagerConnectorService;
  }

  @Override
  public Organization create(String accountIdentifier, OrganizationDTO organizationDTO) {
    validateCreateOrganizationRequest(accountIdentifier, organizationDTO);
    Organization organization = toOrganization(organizationDTO);
    organization.setAccountIdentifier(accountIdentifier);
    try {
      validate(organization);
      Organization savedOrganization = organizationRepository.save(organization);
      performActionsPostOrganizationCreation(organization);
      return savedOrganization;
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Try using different org identifier, [%s] cannot be used", organization.getIdentifier()),
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
  public Organization update(String accountIdentifier, String identifier, OrganizationDTO organizationDTO) {
    validateUpdateOrganizationRequest(accountIdentifier, identifier, organizationDTO);
    Criteria criteria = Criteria.where(OrganizationKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(OrganizationKeys.identifier)
                            .is(identifier)
                            .and(OrganizationKeys.deleted)
                            .ne(Boolean.TRUE);
    if (organizationDTO.getVersion() != null) {
      criteria.and(OrganizationKeys.version).is(organizationDTO.getVersion());
    }
    Query query = new Query(criteria);
    Update update = getUpdate(organizationDTO);
    organizationDTO.setAccountIdentifier(accountIdentifier);
    organizationDTO.setIdentifier(identifier);
    organizationDTO.setName(Optional.ofNullable(organizationDTO.getName()).orElse(NGCommonEntityConstants.NAME_KEY));
    validate(toOrganization(organizationDTO));
    Organization organization = organizationRepository.update(query, update);
    if (organization == null) {
      throw new InvalidRequestException("Resource not found or your resource version might be older", USER);
    }
    return organization;
  }

  private Update getUpdate(OrganizationDTO organizationDTO) {
    Update update = new Update();
    if (isNotBlank(organizationDTO.getName()) && EntityNameValidator.isValid(organizationDTO.getName())) {
      update.set(OrganizationKeys.name, organizationDTO.getName());
    }
    if (isNotBlank(organizationDTO.getColor())) {
      update.set(OrganizationKeys.color, organizationDTO.getColor());
    }
    if (organizationDTO.getDescription() != null) {
      update.set(OrganizationKeys.description, organizationDTO.getDescription());
    }
    if (organizationDTO.getTags() != null) {
      update.set(OrganizationKeys.tags, organizationDTO.getTags());
    }
    return update;
  }

  @Override
  public Page<Organization> list(
      String accountIdentifier, Pageable pageable, OrganizationFilterDTO organizationFilterDTO) {
    Criteria criteria = createOrganizationFilterCriteria(Criteria.where(OrganizationKeys.accountIdentifier)
                                                             .is(accountIdentifier)
                                                             .and(OrganizationKeys.deleted)
                                                             .ne(Boolean.TRUE),
        organizationFilterDTO);
    return organizationRepository.findAll(criteria, pageable);
  }

  @Override
  public Page<Organization> list(Criteria criteria, Pageable pageable) {
    return organizationRepository.findAll(criteria, pageable);
  }

  private Criteria createOrganizationFilterCriteria(Criteria criteria, OrganizationFilterDTO organizationFilterDTO) {
    if (organizationFilterDTO == null) {
      return criteria;
    }
    if (isNotBlank(organizationFilterDTO.getSearchTerm())) {
      criteria.orOperator(Criteria.where(OrganizationKeys.name).regex(organizationFilterDTO.getSearchTerm(), "i"),
          Criteria.where(OrganizationKeys.tags).regex(organizationFilterDTO.getSearchTerm(), "i"));
    }
    return criteria;
  }

  @Override
  public boolean delete(String accountIdentifier, String organizationIdentifier, Long version) {
    return organizationRepository.delete(accountIdentifier, organizationIdentifier, version);
  }

  private void validateCreateOrganizationRequest(String accountIdentifier, OrganizationDTO organization) {
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(accountIdentifier, organization.getAccountIdentifier())), true);
  }

  private void validateUpdateOrganizationRequest(
      String accountIdentifier, String identifier, OrganizationDTO organization) {
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(accountIdentifier, organization.getAccountIdentifier()),
                               Pair.of(identifier, organization.getIdentifier())),
        true);
  }
}
