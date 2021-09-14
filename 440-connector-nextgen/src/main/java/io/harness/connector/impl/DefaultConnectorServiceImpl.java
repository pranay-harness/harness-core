/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector.impl;

import static io.harness.NGConstants.CONNECTOR_HEARTBEAT_LOG_PREFIX;
import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.connector.ConnectivityStatus.FAILURE;
import static io.harness.connector.ConnectivityStatus.UNKNOWN;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.errorhandling.NGErrorHelper.DEFAULT_ERROR_SUMMARY;
import static io.harness.exception.WingsException.USER;
import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.mongodb.core.query.Criteria.where;

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
import io.harness.connector.events.ConnectorCreateEvent;
import io.harness.connector.events.ConnectorDeleteEvent;
import io.harness.connector.events.ConnectorUpdateEvent;
import io.harness.connector.helper.CatalogueHelper;
import io.harness.connector.helper.HarnessManagedConnectorHelper;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.services.ConnectorFilterService;
import io.harness.connector.services.ConnectorHeartbeatService;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.stats.ConnectorStatistics;
import io.harness.connector.stats.ConnectorStatusStats;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitConnectorDTO;
import io.harness.delegate.beans.connector.scm.awscodecommit.AwsCodeCommitUrlType;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnectorDTO;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.connector.scm.github.GithubConnectorDTO;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnectorDTO;
import io.harness.encryption.SecretRefData;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.ConnectorNotFoundException;
import io.harness.exception.DelegateServiceDriverException;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.WingsException;
import io.harness.exception.ngexception.ConnectorValidationException;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitSyncSdkService;
import io.harness.gitsync.sdk.EntityGitDetailsMapper;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxService;
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
import java.util.function.Supplier;
import javax.ws.rs.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
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
  OutboxService outboxService;

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
  public Optional<ConnectorResponseDTO> getByName(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String name, boolean isDeletedAllowed) {
    /***
     * In db currently we can have multiple connectors with same name
     * Once cleanup/verification is done, this Page can be converted into optional
     *
     */
    Page<Connector> connectorsWithGivenName =
        getConnectorsWithGivenName(accountIdentifier, orgIdentifier, projectIdentifier, name, isDeletedAllowed);
    if (connectorsWithGivenName.getTotalElements() == 0) {
      return Optional.empty();
    }
    Optional<Connector> connectorEntity = connectorsWithGivenName.get().findFirst();
    return connectorEntity.map(connectorMapper::writeDTO);
  }

  @Override
  public Optional<ConnectorResponseDTO> getFromBranch(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifier, String repo, String branch) {
    Criteria criteria =
        createCriteriaToFetchConnector(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    final Optional<Connector> connectorOptional = connectorRepository.findOne(criteria, repo, branch);
    return connectorOptional.map(connectorMapper::writeDTO);
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
      String projectIdentifier, String searchTerm, ConnectorType type, ConnectorCategory category,
      ConnectorCategory sourceCategory) {
    Criteria criteria = filterService.createCriteriaFromConnectorFilter(
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type, category, sourceCategory);
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

  /***
   * Saves a connector
   *
   * Note: Don't add any logic in create function, all logic should go inside createInternal method
   *
   * @param connectorRequestDTO
   * @param accountIdentifier
   * @return
   */
  @Override
  public ConnectorResponseDTO create(ConnectorDTO connectorRequestDTO, String accountIdentifier) {
    return createInternal(connectorRequestDTO, accountIdentifier, ChangeType.ADD);
  }

  @Override
  public ConnectorResponseDTO create(ConnectorDTO connector, String accountIdentifier, ChangeType gitChangeType) {
    return createInternal(connector, accountIdentifier, gitChangeType);
  }

  private ConnectorResponseDTO createInternal(
      ConnectorDTO connectorRequestDTO, String accountIdentifier, ChangeType changeType) {
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
      Supplier<OutboxEvent> supplier = null;
      if (!gitSyncSdkService.isGitSyncEnabled(
              accountIdentifier, connectorInfo.getOrgIdentifier(), connectorInfo.getProjectIdentifier())) {
        supplier = ()
            -> outboxService.save(new ConnectorCreateEvent(accountIdentifier, connectorRequestDTO.getConnectorInfo()));
      }
      savedConnectorEntity = connectorRepository.save(connectorEntity, connectorRequestDTO, changeType, supplier);
      connectorEntityReferenceHelper.createSetupUsageForSecret(
          connectorRequestDTO.getConnectorInfo(), accountIdentifier, false);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format("Connector [%s] already exists", connectorEntity.getIdentifier()));
    }
    return connectorMapper.writeDTO(savedConnectorEntity);
  }

  private void validateThatAConnectorWithThisNameDoesNotExists(
      ConnectorInfoDTO connectorRequestDTO, String accountIdentifier) {
    Page<Connector> connectors = getConnectorsWithGivenName(accountIdentifier, connectorRequestDTO.getOrgIdentifier(),
        connectorRequestDTO.getProjectIdentifier(), connectorRequestDTO.getName(), true);
    if (connectors != null && connectors.getSize() >= 1) {
      throw new InvalidRequestException(
          format("Connector with name [%s] already exists", connectorRequestDTO.getName()));
    }
  }

  private Page<Connector> getConnectorsWithGivenName(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String name, boolean isDeletedAllowed) {
    Criteria criteria = new Criteria()
                            .and(ConnectorKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(ConnectorKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(ConnectorKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(ConnectorKeys.name)
                            .is(name);
    if (isDeletedAllowed) {
      criteria.orOperator(where(ConnectorKeys.deleted).exists(false), where(ConnectorKeys.deleted).is(false));
    }
    return connectorRepository.findAll(
        criteria, Pageable.unpaged(), projectIdentifier, orgIdentifier, accountIdentifier);
  }

  @Override
  public ConnectorResponseDTO update(ConnectorDTO connectorRequest, String accountIdentifier) {
    return update(connectorRequest, accountIdentifier, ChangeType.MODIFY);
  }

  @Override
  public ConnectorResponseDTO update(
      ConnectorDTO connectorRequest, String accountIdentifier, ChangeType gitChangeType) {
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
    final ConnectorResponseDTO oldConnectorDTO = connectorMapper.writeDTO(existingConnector);
    Connector newConnector = connectorMapper.toConnector(connectorRequest, accountIdentifier);
    newConnector.setId(existingConnector.getId());
    newConnector.setVersion(existingConnector.getVersion());
    newConnector.setConnectivityDetails(existingConnector.getConnectivityDetails());
    newConnector.setCreatedAt(existingConnector.getCreatedAt());
    newConnector.setTimeWhenConnectorIsLastUpdated(System.currentTimeMillis());
    newConnector.setActivityDetails(existingConnector.getActivityDetails());
    setGitDetails(existingConnector, newConnector);

    if (existingConnector.getIsFromDefaultBranch() == null || existingConnector.getIsFromDefaultBranch()) {
      if (existingConnector.getHeartbeatPerpetualTaskId() == null
          && !harnessManagedConnectorHelper.isHarnessManagedSecretManager(connector)) {
        PerpetualTaskId connectorHeartbeatTaskId = connectorHeartbeatService.createConnectorHeatbeatTask(
            accountIdentifier, existingConnector.getOrgIdentifier(), existingConnector.getProjectIdentifier(),
            existingConnector.getIdentifier());
        newConnector.setHeartbeatPerpetualTaskId(
            connectorHeartbeatTaskId == null ? null : connectorHeartbeatTaskId.getId());
      } else if (existingConnector.getHeartbeatPerpetualTaskId() != null) {
        connectorHeartbeatService.resetPerpetualTask(
            accountIdentifier, existingConnector.getHeartbeatPerpetualTaskId());
        newConnector.setHeartbeatPerpetualTaskId(existingConnector.getHeartbeatPerpetualTaskId());
      }
    }
    try {
      Supplier<OutboxEvent> supplier = null;
      if (!gitSyncSdkService.isGitSyncEnabled(
              accountIdentifier, connector.getOrgIdentifier(), connector.getProjectIdentifier())) {
        supplier = ()
            -> outboxService.save(new ConnectorUpdateEvent(
                accountIdentifier, oldConnectorDTO.getConnector(), connectorRequest.getConnectorInfo()));
      }
      Connector updatedConnector = connectorRepository.save(newConnector, connectorRequest, gitChangeType, supplier);
      connectorEntityReferenceHelper.createSetupUsageForSecret(connector, accountIdentifier, true);
      return connectorMapper.writeDTO(updatedConnector);

    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format("Connector [%s] already exists", existingConnector.getIdentifier()));
    }
  }

  private Criteria createCriteriaToFetchConnector(
      Object accountIdentifier, Object orgIdentifier, Object projectIdentifier, Object identifier) {
    Criteria criteria = new Criteria();
    criteria.and(ConnectorKeys.accountIdentifier).is(accountIdentifier);
    criteria.and(ConnectorKeys.orgIdentifier).is(orgIdentifier);
    criteria.and(ConnectorKeys.projectIdentifier).is(projectIdentifier);
    criteria.and(ConnectorKeys.identifier).is(identifier);
    criteria.orOperator(where(ConnectorKeys.deleted).exists(false), where(ConnectorKeys.deleted).is(false));
    return criteria;
  }

  private void setGitDetails(Connector existingConnector, Connector newConnector) {
    EntityGitDetailsMapper.copyEntityGitDetails(existingConnector, newConnector);
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
    Supplier<OutboxEvent> supplier = null;
    if (!gitSyncSdkService.isGitSyncEnabled(accountIdentifier, orgIdentifier, projectIdentifier)) {
      supplier = ()
          -> outboxService.save(
              new ConnectorDeleteEvent(accountIdentifier, connectorMapper.writeDTO(existingConnector).getConnector()));
    }

    connectorRepository.save(existingConnector, null, ChangeType.DELETE, supplier);

    return true;
  }

  @Override
  public long count(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Criteria criteria = filterService.createCriteriaFromConnectorFilter(
        accountIdentifier, orgIdentifier, projectIdentifier, null, null, null, null);
    return connectorRepository.count(criteria);
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

    if (connector.getCategories() != null & !connector.getCategories().contains(ConnectorCategory.CODE_REPO)) {
      log.info("Test Connection failed for connector with identifier[{}] in account[{}] with error [{}]",
          connector.getIdentifier(), accountIdentifier, "Non git connector is provided for repo verification");
      validationResult = ConnectorValidationResult.builder().status(FAILURE).build();
      return validationResult;
    }

    ConnectorResponseDTO connectorDTO = connectorMapper.writeDTO(connector);
    ConnectorInfoDTO connectorInfo = connectorDTO.getConnector();
    ConnectorConfigDTO connectorConfig = connectorInfo.getConnectorConfig();
    // Use Repo URL from parameter instead of using configured URL
    if (isNotEmpty(gitRepoURL)) {
      ScmConnector scmConnector = (ScmConnector) connectorConfig;
      setConnectorGitRepo(scmConnector, gitRepoURL);
      connectorInfo.setConnectorConfig(connectorConfig);
    }
    return validateConnector(connector, connectorDTO, connectorInfo, accountIdentifier, orgIdentifier,
        projectIdentifier, connectorIdentifier);
  }

  private void setConnectorGitRepo(ScmConnector scmConnector, String gitRepoURL) {
    scmConnector.setUrl(gitRepoURL);
    if (scmConnector instanceof GitConfigDTO) {
      ((GitConfigDTO) scmConnector).setGitConnectionType(GitConnectionType.REPO);
    } else if (scmConnector instanceof GithubConnectorDTO) {
      ((GithubConnectorDTO) scmConnector).setConnectionType(GitConnectionType.REPO);
    } else if (scmConnector instanceof GitlabConnectorDTO) {
      ((GitlabConnectorDTO) scmConnector).setConnectionType(GitConnectionType.REPO);
    } else if (scmConnector instanceof BitbucketConnectorDTO) {
      ((BitbucketConnectorDTO) scmConnector).setConnectionType(GitConnectionType.REPO);
    } else if (scmConnector instanceof AwsCodeCommitConnectorDTO) {
      ((AwsCodeCommitConnectorDTO) scmConnector).setUrlType(AwsCodeCommitUrlType.REPO);
    }
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
    ConnectorStatistics stats = connectorStatisticsHelper.getStats(accountIdentifier, orgIdentifier, projectIdentifier);
    changeTheNullStatusToUnknown(stats);
    return stats;
  }

  private void changeTheNullStatusToUnknown(ConnectorStatistics stats) {
    if (stats == null) {
      return;
    }
    List<ConnectorStatusStats> statusStats = stats.getStatusStats();
    for (ConnectorStatusStats connectorStatusStats : statusStats) {
      if (connectorStatusStats.getStatus() == null) {
        connectorStatusStats.setStatus(UNKNOWN);
      }
    }
  }

  @Override
  public String getHeartbeatPerpetualTaskId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    Optional<Connector> connectorOptional = connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(
        fullyQualifiedIdentifier, projectIdentifier, orgIdentifier, accountIdentifier, true);

    return connectorOptional
        .filter(connector -> connector.getIsFromDefaultBranch() == null || connector.getIsFromDefaultBranch())
        .map(connector -> {
          if (isEmpty(connector.getHeartbeatPerpetualTaskId())) {
            PerpetualTaskId connectorHeatbeatTaskId = connectorHeartbeatService.createConnectorHeatbeatTask(
                accountIdentifier, orgIdentifier, projectIdentifier, identifier);
            if (connectorHeatbeatTaskId != null) {
              updateConnectorEntityWithPerpetualtaskId(
                  accountIdentifier, orgIdentifier, projectIdentifier, identifier, connectorHeatbeatTaskId.getId());
              return connectorHeatbeatTaskId.getId();
            } else {
              return null;
            }
          } else {
            return connector.getHeartbeatPerpetualTaskId();
          }
        })
        .orElseThrow(
            ()
                -> new ConnectorNotFoundException(
                    format(
                        "No connector found with identifier [%s] with accountidentifier [%s], orgIdentifier [%s] and projectIdentifier [%s]",
                        identifier, accountIdentifier, orgIdentifier, projectIdentifier),
                    USER));
  }

  @Override
  public void resetHeartbeatForReferringConnectors(List<Pair<String, String>> connectorPerpetualTaskInfo) {
    for (Pair<String, String> item : connectorPerpetualTaskInfo) {
      log.info("Resetting perpetual task with id {} in account {}", item.getValue(), item.getKey());
      if (item.getValue() != null) {
        connectorHeartbeatService.resetPerpetualTask(item.getKey(), item.getValue());
      }
    }
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

  @Override
  public void deleteBatch(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, List<String> connectorIdentifiersList) {
    for (String connectorIdentifier : connectorIdentifiersList) {
      String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
          accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
      Optional<Connector> connectorOptional = connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(
          fullyQualifiedIdentifier, projectIdentifier, orgIdentifier, accountIdentifier, true);
      connectorOptional
          .map(item -> {
            String heartbeatTaskId = item.getHeartbeatPerpetualTaskId();
            String connectorFQN = item.getFullyQualifiedIdentifier();
            if (isNotBlank(heartbeatTaskId)) {
              boolean perpetualTaskIsDeleted =
                  connectorHeartbeatService.deletePerpetualTask(accountIdentifier, heartbeatTaskId, connectorFQN);
              if (perpetualTaskIsDeleted == false) {
                log.info("{} The perpetual task could not be deleted {}", CONNECTOR_HEARTBEAT_LOG_PREFIX, connectorFQN);
                return false;
              }
            }
            item.setDeleted(true);
            connectorRepository.save(item, ChangeType.DELETE);
            return true;
          })
          .orElseThrow(()
                           -> new ConnectorNotFoundException(
                               String.format("No connector found with identifier %s", connectorIdentifier), USER));
    }
  }
}
