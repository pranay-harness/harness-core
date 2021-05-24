package io.harness.connector.impl;

import static io.harness.NGConstants.CONNECTOR_HEARTBEAT_LOG_PREFIX;
import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.connector.ConnectivityStatus.FAILURE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.connector.ConnectorType.GIT;
import static io.harness.errorhandling.NGErrorHelper.DEFAULT_ERROR_SUMMARY;
import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.beans.IdentifierRef;
import io.harness.beans.SortOrder;
import io.harness.beans.SortOrder.OrderType;
import io.harness.connector.ConnectorCatalogueResponseDTO;
import io.harness.connector.ConnectorCategory;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.ConnectorValidationResult.ConnectorValidationResultBuilder;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.connector.helper.CatalogueHelper;
import io.harness.connector.helper.HarnessManagedConnectorHelper;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.services.ConnectorFilterService;
import io.harness.connector.services.ConnectorHeartbeatService;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.stats.ConnectorStatistics;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.encryption.SecretRefData;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.DelegateServiceDriverException;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.exception.ngexception.ConnectorValidationException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.repositories.ConnectorRepository;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.PageUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(HarnessTeam.DX)
public class DefaultConnectorServiceImpl implements ConnectorService {
  private final ConnectorMapper connectorMapper;
  private final ConnectorRepository connectorRepository;
  private final ConnectorFilterService filterService;
  private Map<String, ConnectionValidator> connectionValidatorMap;
  private final CatalogueHelper catalogueHelper;
  private final ProjectService projectService;
  private final OrganizationService organizationService;
  EntitySetupUsageClient entitySetupUsageClient;
  ConnectorStatisticsHelper connectorStatisticsHelper;
  private NGErrorHelper ngErrorHelper;
  private ConnectorErrorMessagesHelper connectorErrorMessagesHelper;
  private SecretRefInputValidationHelper secretRefInputValidationHelper;
  ConnectorHeartbeatService connectorHeartbeatService;
  private final HarnessManagedConnectorHelper harnessManagedConnectorHelper;
  private final ConnectorEntityReferenceHelper connectorEntityReferenceHelper;
  GitSyncSdkService gitSyncSdkService;

  @Override
  public Optional<ConnectorResponseDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Optional<Connector> connector = connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(
        fullyQualifiedIdentifier, projectIdentifier, orgIdentifier, accountIdentifier, true);
    return connector.map(connectorMapper::writeDTO);
  }

  @Override
  public Page<ConnectorResponseDTO> list(int page, int size, String accountIdentifier,
      ConnectorFilterPropertiesDTO filterProperties, String orgIdentifier, String projectIdentifier,
      String filterIdentifier, String searchTerm, Boolean includeAllConnectorsAccessibleAtScope,
      Boolean getDistinctFromBranches) {
    Criteria criteria = filterService.createCriteriaFromConnectorListQueryParams(accountIdentifier, orgIdentifier,
        projectIdentifier, filterIdentifier, searchTerm, filterProperties, includeAllConnectorsAccessibleAtScope);
    Pageable pageable = PageUtils.getPageRequest(
        PageRequest.builder()
            .pageIndex(page)
            .pageSize(size)
            .sortOrders(Collections.singletonList(
                SortOrder.Builder.aSortOrder().withField(ConnectorKeys.createdAt, OrderType.DESC).build()))
            .build());
    Page<Connector> connectors;
    if (Boolean.TRUE.equals(getDistinctFromBranches)
        && gitSyncSdkService.isGitSyncEnabled(accountIdentifier, orgIdentifier, projectIdentifier)) {
      connectors = connectorRepository.findAll(criteria, pageable, true);
    } else {
      connectors = connectorRepository.findAll(criteria, pageable, projectIdentifier, orgIdentifier, accountIdentifier);
    }
    return connectors.map(connectorMapper::writeDTO);
  }

  public Page<ConnectorResponseDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType type, ConnectorCategory category) {
    Criteria criteria = filterService.createCriteriaFromConnectorFilter(
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type, category);
    Pageable pageable = PageUtils.getPageRequest(
        PageRequest.builder()
            .pageIndex(page)
            .pageSize(size)
            .sortOrders(Collections.singletonList(
                SortOrder.Builder.aSortOrder().withField(ConnectorKeys.createdAt, OrderType.DESC).build()))
            .build());
    Page<Connector> connectors =
        connectorRepository.findAll(criteria, pageable, projectIdentifier, orgIdentifier, accountIdentifier);
    return connectors.map(connector -> connectorMapper.writeDTO(connector));
  }

  @VisibleForTesting
  void assurePredefined(ConnectorDTO connectorDTO, String accountIdentifier) {
    assureThatTheProjectAndOrgExists(connectorDTO, accountIdentifier);
    assureThatTheSecretIsValidAndItExists(accountIdentifier, connectorDTO.getConnectorInfo());
  }

  private void assureThatTheSecretIsValidAndItExists(String accountIdentifier, ConnectorInfoDTO connectorDTO) {
    List<DecryptableEntity> decryptableEntities = connectorDTO.getConnectorConfig().getDecryptableEntities();
    if (isEmpty(decryptableEntities)) {
      return;
    }
    Map<String, SecretRefData> secrets = secretRefInputValidationHelper.getDecryptableFieldsData(decryptableEntities);
    NGAccess baseNGAccess = BaseNGAccess.builder()
                                .accountIdentifier(accountIdentifier)
                                .orgIdentifier(connectorDTO.getOrgIdentifier())
                                .projectIdentifier(connectorDTO.getProjectIdentifier())
                                .build();
    if (isNotEmpty(secrets)) {
      secrets.forEach(
          (fieldName, secret) -> secretRefInputValidationHelper.validateTheSecretInput(secret, baseNGAccess));
    }
  }

  void assureThatTheProjectAndOrgExists(ConnectorDTO connectorDTO, String accountIdentifier) {
    String orgIdentifier = connectorDTO.getConnectorInfo().getOrgIdentifier();
    String projectIdentifier = connectorDTO.getConnectorInfo().getProjectIdentifier();

    if (isNotEmpty(projectIdentifier)) {
      // its a project level connector
      if (isEmpty(orgIdentifier)) {
        throw new InvalidRequestException(
            String.format("Project %s specified without the org Identifier", projectIdentifier));
      }
      checkThatTheProjectExists(orgIdentifier, projectIdentifier, accountIdentifier);
    } else if (isNotEmpty(orgIdentifier)) {
      // its a org level connector
      checkThatTheOrganizationExists(orgIdentifier, accountIdentifier);
    }
  }

  private void checkThatTheOrganizationExists(String orgIdentifier, String accountIdentifier) {
    if (isNotEmpty(orgIdentifier)) {
      final Optional<Organization> organization = organizationService.get(accountIdentifier, orgIdentifier);
      if (!organization.isPresent()) {
        throw new NotFoundException(String.format("org [%s] not found.", orgIdentifier));
      }
    }
  }

  private void checkThatTheProjectExists(String orgIdentifier, String projectIdentifier, String accountIdentifier) {
    if (isNotEmpty(orgIdentifier) && isNotEmpty(projectIdentifier)) {
      final Optional<Project> project = projectService.get(accountIdentifier, orgIdentifier, projectIdentifier);
      if (!project.isPresent()) {
        throw new NotFoundException(String.format("project [%s] not found.", projectIdentifier));
      }
    }
  }

  @Override
  public ConnectorResponseDTO create(ConnectorDTO connectorRequestDTO, String accountIdentifier) {
    assurePredefined(connectorRequestDTO, accountIdentifier);
    ConnectorInfoDTO connectorInfo = connectorRequestDTO.getConnectorInfo();
    final boolean isIdentifierUnique = validateTheIdentifierIsUnique(accountIdentifier,
        connectorInfo.getOrgIdentifier(), connectorInfo.getProjectIdentifier(), connectorInfo.getIdentifier());
    if (!isIdentifierUnique) {
      throw new InvalidRequestException(
          String.format("The connector with identifier %s already exists in the account %s, org %s, project %s",
              connectorInfo.getIdentifier(), accountIdentifier, connectorInfo.getOrgIdentifier(),
              connectorInfo.getProjectIdentifier()));
    }
    validateThatAConnectorWithThisNameDoesNotExists(connectorRequestDTO.getConnectorInfo(), accountIdentifier);
    Connector connectorEntity = connectorMapper.toConnector(connectorRequestDTO, accountIdentifier);
    connectorEntity.setTimeWhenConnectorIsLastUpdated(System.currentTimeMillis());
    Connector savedConnectorEntity = null;
    try {
      savedConnectorEntity = connectorRepository.save(connectorEntity, connectorRequestDTO);
      connectorEntityReferenceHelper.createSetupUsageForSecret(
          connectorRequestDTO.getConnectorInfo(), accountIdentifier, false);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format("Connector [%s] already exists", connectorEntity.getIdentifier()));
    }

    return connectorMapper.writeDTO(savedConnectorEntity);
  }

  private void validateThatAConnectorWithThisNameDoesNotExists(
      ConnectorInfoDTO connectorRequestDTO, String accountIdentifier) {
    Page<Connector> connectors = getConnectorsWithGivenNames(accountIdentifier, connectorRequestDTO.getOrgIdentifier(),
        connectorRequestDTO.getProjectIdentifier(), connectorRequestDTO.getName());
    if (connectors != null && connectors.getSize() >= 1) {
      throw new InvalidRequestException(
          format("Connector with name [%s] already exists", connectorRequestDTO.getName()));
    }
  }

  private Page<Connector> getConnectorsWithGivenNames(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Object name) {
    Criteria criteria = new Criteria()
                            .and(ConnectorKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(ConnectorKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(ConnectorKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(ConnectorKeys.name)
                            .is(name);
    return connectorRepository.findAll(
        criteria, Pageable.unpaged(), projectIdentifier, orgIdentifier, accountIdentifier);
  }

  @Override
  public ConnectorResponseDTO update(ConnectorDTO connectorRequest, String accountIdentifier) {
    assurePredefined(connectorRequest, accountIdentifier);
    ConnectorInfoDTO connector = connectorRequest.getConnectorInfo();
    Objects.requireNonNull(connector.getIdentifier());
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, connector.getOrgIdentifier(), connector.getProjectIdentifier(), connector.getIdentifier());
    Optional<Connector> existingConnectorOptional =
        connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(fullyQualifiedIdentifier,
            connector.getProjectIdentifier(), connector.getOrgIdentifier(), accountIdentifier, true);
    if (!existingConnectorOptional.isPresent()) {
      throw new InvalidRequestException(
          format("No connector exists with the  Identifier %s", connector.getIdentifier()));
    }
    Connector existingConnector = existingConnectorOptional.get();
    validateTheUpdateRequestIsValid(existingConnector, connectorRequest.getConnectorInfo(), accountIdentifier);
    Connector newConnector = connectorMapper.toConnector(connectorRequest, accountIdentifier);
    newConnector.setId(existingConnector.getId());
    newConnector.setVersion(existingConnector.getVersion());
    newConnector.setConnectivityDetails(existingConnector.getConnectivityDetails());
    newConnector.setCreatedAt(existingConnector.getCreatedAt());
    newConnector.setTimeWhenConnectorIsLastUpdated(System.currentTimeMillis());
    newConnector.setActivityDetails(existingConnector.getActivityDetails());
    if (existingConnector.getHeartbeatPerpetualTaskId() == null
        && !harnessManagedConnectorHelper.isHarnessManagedSecretManager(connector)) {
      PerpetualTaskId connectorHeartbeatTaskId =
          connectorHeartbeatService.createConnectorHeatbeatTask(accountIdentifier, existingConnector.getOrgIdentifier(),
              existingConnector.getProjectIdentifier(), existingConnector.getIdentifier());
      newConnector.setHeartbeatPerpetualTaskId(
          connectorHeartbeatTaskId == null ? null : connectorHeartbeatTaskId.getId());
    } else {
      newConnector.setHeartbeatPerpetualTaskId(existingConnector.getHeartbeatPerpetualTaskId());
    }
    Connector updatedConnector;
    try {
      updatedConnector = connectorRepository.save(newConnector, connectorRequest, ChangeType.MODIFY);
      connectorEntityReferenceHelper.createSetupUsageForSecret(connector, accountIdentifier, true);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format("Connector [%s] already exists", existingConnector.getIdentifier()));
    }
    return connectorMapper.writeDTO(updatedConnector);
  }

  private void validateTheUpdateRequestIsValid(
      Connector existingConnector, ConnectorInfoDTO connectorInfo, String accountIdentifier) {
    validateTheConnectorTypeIsNotChanged(existingConnector.getType(), connectorInfo.getConnectorType(),
        accountIdentifier, connectorInfo.getOrgIdentifier(), connectorInfo.getProjectIdentifier(),
        connectorInfo.getIdentifier());
    validateThatAConnectorWithThisNameDoesNotExistsDuringUpdate(
        connectorInfo, accountIdentifier, existingConnector.getId());
  }

  private void validateThatAConnectorWithThisNameDoesNotExistsDuringUpdate(
      ConnectorInfoDTO connectorInfo, String accountIdentifier, String id) {
    Page<Connector> connectors = getConnectorsWithGivenNames(accountIdentifier, connectorInfo.getOrgIdentifier(),
        connectorInfo.getProjectIdentifier(), connectorInfo.getName());
    if (connectors.getSize() > 1 || whetherADifferentConnectorExistsWithThisName(id, connectors)) {
      throw new InvalidRequestException(format("Connector with name [%s] already exists", connectorInfo.getName()));
    }
  }

  private boolean whetherADifferentConnectorExistsWithThisName(String id, Page<Connector> connectors) {
    if (connectors.getSize() == 1) {
      Connector connector = connectors.getContent().get(0);
      return !connector.getId().equals(id);
    }
    return false;
  }

  private void validateTheConnectorTypeIsNotChanged(ConnectorType existingConnectorType,
      ConnectorType typeInTheUpdateRequest, String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String connectorIdentifier) {
    if (existingConnectorType != typeInTheUpdateRequest) {
      String noConnectorExistsWithTypeMessage = String.format("%s with type %s",
          connectorErrorMessagesHelper.createConnectorNotFoundMessage(
              accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier),
          typeInTheUpdateRequest);
      throw new InvalidRequestException(noConnectorExistsWithTypeMessage);
    }
  }

  @Override
  public boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Optional<Connector> existingConnectorOptional = connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(
        fullyQualifiedIdentifier, projectIdentifier, orgIdentifier, accountIdentifier, true);
    if (!existingConnectorOptional.isPresent()) {
      throw new InvalidRequestException(connectorErrorMessagesHelper.createConnectorNotFoundMessage(
          accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
    }
    Connector existingConnector = existingConnectorOptional.get();
    ConnectorResponseDTO connectorDTO = connectorMapper.writeDTO(existingConnector);
    checkThatTheConnectorIsNotUsedByOthers(existingConnector);
    connectorEntityReferenceHelper.deleteConnectorEntityReferenceWhenConnectorGetsDeleted(
        connectorDTO.getConnector(), accountIdentifier);
    existingConnector.setDeleted(true);
    connectorRepository.save(existingConnector, null, ChangeType.DELETE);
    return true;
  }

  private void checkThatTheConnectorIsNotUsedByOthers(Connector connector) {
    boolean isEntityReferenced;
    IdentifierRef identifierRef = IdentifierRef.builder()
                                      .accountIdentifier(connector.getAccountIdentifier())
                                      .orgIdentifier(connector.getOrgIdentifier())
                                      .projectIdentifier(connector.getProjectIdentifier())
                                      .identifier(connector.getIdentifier())
                                      .build();
    String referredEntityFQN = identifierRef.getFullyQualifiedName();
    try {
      isEntityReferenced = execute(entitySetupUsageClient.isEntityReferenced(
          connector.getAccountIdentifier(), referredEntityFQN, EntityType.CONNECTORS));
    } catch (Exception ex) {
      log.info("Encountered exception while requesting the Entity Reference records of [{}], with exception",
          connector.getIdentifier(), ex);
      throw new UnexpectedException("Error while deleting the connector");
    }
    if (isEntityReferenced) {
      throw new InvalidRequestException(String.format(
          "Could not delete the connector %s as it is referenced by other entities", connector.getIdentifier()));
    }
  }

  public ConnectorValidationResult validate(ConnectorDTO connectorRequest, String accountIdentifier) {
    ConnectorInfoDTO connector = connectorRequest.getConnectorInfo();
    return validateSafely(connector, accountIdentifier, connector.getOrgIdentifier(), connector.getProjectIdentifier(),
        connector.getIdentifier());
  }

  public boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    return !connectorRepository.existsByFullyQualifiedIdentifier(
        fullyQualifiedIdentifier, projectIdentifier, orgIdentifier, accountIdentifier);
  }

  @Override
  public ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    Connector connector =
        getConnectorWithIdentifier(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    ConnectorResponseDTO connectorDTO = connectorMapper.writeDTO(connector);
    ConnectorInfoDTO connectorInfo = connectorDTO.getConnector();
    return validateConnector(connector, connectorDTO, connectorInfo, accountIdentifier, orgIdentifier,
        projectIdentifier, connectorIdentifier);
  }

  /**
   * This function tests connection for git connector with repo url from parameter.
   */

  public ConnectorValidationResult testGitRepoConnection(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifier, String gitRepoURL) {
    Connector connector =
        getConnectorWithIdentifier(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    ConnectorValidationResult validationResult;

    if (connector.getType() != GIT) {
      log.info("Test Connection failed for connector with identifier[{}] in account[{}] with error [{}]",
          connector.getIdentifier(), accountIdentifier, "Non git connector is provided for repo verification");
      validationResult = ConnectorValidationResult.builder().status(FAILURE).build();
      return validationResult;
    }

    ConnectorResponseDTO connectorDTO = connectorMapper.writeDTO(connector);
    ConnectorInfoDTO connectorInfo = connectorDTO.getConnector();
    GitConfigDTO gitConfigDTO = (GitConfigDTO) connectorInfo.getConnectorConfig();
    // Use Repo URL from parameter instead of using configured URL
    gitConfigDTO.setUrl(gitRepoURL);
    connectorInfo.setConnectorConfig(gitConfigDTO);

    return validateConnector(connector, connectorDTO, connectorInfo, accountIdentifier, orgIdentifier,
        projectIdentifier, connectorIdentifier);
  }

  private ConnectorValidationResult validateConnector(Connector connector, ConnectorResponseDTO connectorDTO,
      ConnectorInfoDTO connectorInfo, String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String identifier) {
    ConnectorValidationResult validationResult;
    validationResult = validateSafely(connectorInfo, accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    return validationResult;
  }

  public void updateActivityDetailsInTheConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, ConnectorValidationResult connectorValidationResult,
      Long activityTime) {}

  private Connector getConnectorWithIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);

    Optional<Connector> connectorOptional = connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(
        fullyQualifiedIdentifier, projectIdentifier, orgIdentifier, accountIdentifier, true);

    if (connectorOptional.isPresent()) {
      return connectorOptional.get();
    } else {
      throw new InvalidRequestException(connectorErrorMessagesHelper.createConnectorNotFoundMessage(
          accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
    }
  }

  private ConnectorValidationResult validateSafely(ConnectorInfoDTO connectorInfo, String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String identifier) {
    ConnectionValidator connectionValidator = connectionValidatorMap.get(connectorInfo.getConnectorType().toString());
    ConnectorValidationResult validationResult;
    try {
      validationResult = connectionValidator.validate(
          connectorInfo.getConnectorConfig(), accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    } catch (ConnectorValidationException | DelegateServiceDriverException ex) {
      log.error("Test Connection failed for connector with identifier[{}] in account[{}]",
          connectorInfo.getIdentifier(), accountIdentifier, ex);
      ConnectorValidationResultBuilder validationFailureBuilder = ConnectorValidationResult.builder();
      validationFailureBuilder.status(FAILURE).testedAt(System.currentTimeMillis());
      String errorMessage = ex.getMessage();
      if (isNotEmpty(errorMessage)) {
        String errorSummary = ngErrorHelper.getErrorSummary(errorMessage);
        List<ErrorDetail> errorDetail = Collections.singletonList(ngErrorHelper.createErrorDetail(errorMessage));
        validationFailureBuilder.errorSummary(errorSummary).errors(errorDetail);
      }
      return validationFailureBuilder.build();
    } catch (WingsException wingsException) {
      // handle flows which are registered to error handlng framework
      throw wingsException;
    } catch (Exception ex) {
      log.info("Encountered Error while validating the connector {}",
          String.format(CONNECTOR_STRING, connectorInfo.getIdentifier(), accountIdentifier,
              connectorInfo.getOrgIdentifier(), connectorInfo.getProjectIdentifier()),
          ex);
      return createValidationResultWithGenericError(ex);
    }
    return validationResult;
  }

  private ConnectorValidationResult createValidationResultWithGenericError(Exception ex) {
    List<ErrorDetail> errorDetails = Collections.singletonList(ngErrorHelper.getGenericErrorDetail());
    return ConnectorValidationResult.builder()
        .errors(errorDetails)
        .errorSummary(DEFAULT_ERROR_SUMMARY)
        .testedAt(System.currentTimeMillis())
        .status(FAILURE)
        .build();
  }

  @Override
  public ConnectorCatalogueResponseDTO getConnectorCatalogue() {
    return ConnectorCatalogueResponseDTO.builder()
        .catalogue(catalogueHelper.getConnectorTypeToCategoryMapping())
        .build();
  }

  @Override
  public void updateConnectorEntityWithPerpetualtaskId(String accountIdentifier, String connectorOrgIdentifier,
      String connectorProjectIdentifier, String connectorIdentifier, String perpetualTaskId) {
    try {
      String fqn = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
          accountIdentifier, connectorOrgIdentifier, connectorProjectIdentifier, connectorIdentifier);
      Criteria criteria = new Criteria();
      criteria.and(ConnectorKeys.fullyQualifiedIdentifier).is(fqn);
      Update update = new Update();
      update.set(ConnectorKeys.heartbeatPerpetualTaskId, perpetualTaskId);
      connectorRepository.update(
          criteria, update, ChangeType.NONE, connectorProjectIdentifier, connectorOrgIdentifier, accountIdentifier);
    } catch (Exception ex) {
      log.info("{} Exception while saving perpetual task id for the {}", CONNECTOR_HEARTBEAT_LOG_PREFIX,
          String.format(CONNECTOR_STRING, connectorIdentifier, accountIdentifier, connectorOrgIdentifier,
              connectorProjectIdentifier),
          ex);
    }
  }

  @Override
  public ConnectorStatistics getConnectorStatistics(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return connectorStatisticsHelper.getStats(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @Override
  public List<ConnectorResponseDTO> listbyFQN(String accountIdentifier, List<String> connectorFQN) {
    if (isEmpty(connectorFQN)) {
      return emptyList();
    }

    Pageable pageable = PageUtils.getPageRequest(
        PageRequest.builder()
            .pageSize(connectorFQN.size())
            .sortOrders(Collections.singletonList(
                SortOrder.Builder.aSortOrder().withField(ConnectorKeys.createdAt, OrderType.DESC).build()))
            .build());
    Page<Connector> connectors = connectorRepository.findAll(
        Criteria.where(ConnectorKeys.fullyQualifiedIdentifier).in(connectorFQN), pageable, false);
    return connectors.getContent().stream().map(connector -> connectorMapper.writeDTO(connector)).collect(toList());
  }
}
