package io.harness.connector.impl;

import static io.harness.NGConstants.CONNECTOR_HEARTBEAT_LOG_PREFIX;
import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.connector.ConnectivityStatus.FAILURE;
import static io.harness.connector.ConnectivityStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.connector.ConnectorType.GIT;
import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.IdentifierRef;
import io.harness.beans.SortOrder;
import io.harness.beans.SortOrder.OrderType;
import io.harness.connector.ConnectorCategory;
import io.harness.connector.ConnectorConnectivityDetails;
import io.harness.connector.ConnectorConnectivityDetails.ConnectorConnectivityDetailsBuilder;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.apis.dto.ConnectorCatalogueResponseDTO;
import io.harness.connector.apis.dto.ConnectorFilterPropertiesDTO;
import io.harness.connector.apis.dto.stats.ConnectorStatistics;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.connector.helper.CatalogueHelper;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.services.ConnectorFilterService;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.encryption.Scope;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ng.core.services.ProjectService;
import io.harness.repositories.ConnectorRepository;
import io.harness.utils.FullyQualifiedIdentifierHelper;
import io.harness.utils.PageUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
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
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
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

  @Override
  public Optional<ConnectorResponseDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Optional<Connector> connector =
        connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(fullyQualifiedIdentifier, true);
    return connector.map(connectorMapper::writeDTO);
  }

  private String createConnectorNotFoundMessage(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    StringBuilder stringBuilder = new StringBuilder(256);
    stringBuilder.append("No connector exists with the identifier ")
        .append(connectorIdentifier)
        .append(" in account ")
        .append(accountIdentifier);
    if (isNotBlank(orgIdentifier)) {
      stringBuilder.append(", organisation ").append(orgIdentifier);
    }
    if (isNotBlank(projectIdentifier)) {
      stringBuilder.append(", project ").append(projectIdentifier);
    }
    return stringBuilder.toString();
  }

  @Override
  public Page<ConnectorResponseDTO> list(int page, int size, String accountIdentifier,
      ConnectorFilterPropertiesDTO filterProperties, String orgIdentifier, String projectIdentifier,
      String filterIdentifier, String searchTerm, Boolean includeAllConnectorsAccessibleAtScope) {
    Criteria criteria = filterService.createCriteriaFromConnectorListQueryParams(accountIdentifier, orgIdentifier,
        projectIdentifier, filterIdentifier, searchTerm, filterProperties, includeAllConnectorsAccessibleAtScope);
    Pageable pageable = PageUtils.getPageRequest(
        PageRequest.builder()
            .pageIndex(page)
            .pageSize(size)
            .sortOrders(Collections.singletonList(
                SortOrder.Builder.aSortOrder().withField(ConnectorKeys.createdAt, OrderType.DESC).build()))
            .build());
    Page<Connector> connectors = connectorRepository.findAll(criteria, pageable);
    return connectors.map(connector -> connectorMapper.writeDTO(connector));
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
    Page<Connector> connectors = connectorRepository.findAll(criteria, pageable);
    return connectors.map(connector -> connectorMapper.writeDTO(connector));
  }

  @VisibleForTesting
  void assurePredefined(ConnectorDTO connectorDTO, String accountIdentifier) {
    String orgIdentifier = connectorDTO.getConnectorInfo().getOrgIdentifier();
    String projectIdentifier = connectorDTO.getConnectorInfo().getProjectIdentifier();

    if (isNotEmpty(orgIdentifier)) {
      final Optional<Organization> organization = organizationService.get(accountIdentifier, orgIdentifier);
      if (!organization.isPresent()) {
        throw new NotFoundException(String.format("org [%s] not found.", orgIdentifier));
      }
    }
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
    Connector connectorEntity = connectorMapper.toConnector(connectorRequestDTO, accountIdentifier);
    Connector savedConnectorEntity = null;
    try {
      savedConnectorEntity = connectorRepository.save(connectorEntity);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(format("Connector [%s] already exists", connectorEntity.getIdentifier()));
    }
    return connectorMapper.writeDTO(savedConnectorEntity);
  }

  @Override
  public ConnectorResponseDTO update(ConnectorDTO connectorRequest, String accountIdentifier) {
    assurePredefined(connectorRequest, accountIdentifier);

    ConnectorInfoDTO connector = connectorRequest.getConnectorInfo();
    Objects.requireNonNull(connector.getIdentifier());
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, connector.getOrgIdentifier(), connector.getProjectIdentifier(), connector.getIdentifier());
    Optional<Connector> existingConnector =
        connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(fullyQualifiedIdentifier, true);
    if (!existingConnector.isPresent()) {
      throw new InvalidRequestException(
          format("No connector exists with the  Identifier %s", connector.getIdentifier()));
    }
    Connector newConnector = connectorMapper.toConnector(connectorRequest, accountIdentifier);
    newConnector.setId(existingConnector.get().getId());
    newConnector.setVersion(existingConnector.get().getVersion());
    newConnector.setConnectivityDetails(existingConnector.get().getConnectivityDetails());
    newConnector.setCreatedAt(existingConnector.get().getCreatedAt());
    Connector updatedConnector = connectorRepository.save(newConnector);
    return connectorMapper.writeDTO(updatedConnector);
  }

  @Override
  public boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Optional<Connector> existingConnectorOptional =
        connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(fullyQualifiedIdentifier, true);
    if (!existingConnectorOptional.isPresent()) {
      throw new InvalidRequestException(
          createConnectorNotFoundMessage(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
    }
    Connector existingConnector = existingConnectorOptional.get();
    checkThatTheConnectorIsNotUsedByOthers(existingConnector);
    existingConnector.setDeleted(true);
    connectorRepository.save(existingConnector);
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
      isEntityReferenced =
          execute(entitySetupUsageClient.isEntityReferenced(connector.getAccountIdentifier(), referredEntityFQN));
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
    return validateSafely(connector, accountIdentifier, connector.getOrgIdentifier(), connector.getProjectIdentifier());
  }

  public boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    return !connectorRepository.existsByFullyQualifiedIdentifier(fullyQualifiedIdentifier);
  }

  @Override
  public ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    Connector connector =
        getConnectorWithIdentifier(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    ConnectorResponseDTO connectorDTO = connectorMapper.writeDTO(connector);
    ConnectorInfoDTO connectorInfo = connectorDTO.getConnector();
    return validateConnector(
        connector, connectorDTO, connectorInfo, accountIdentifier, orgIdentifier, projectIdentifier);
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
      validationResult = ConnectorValidationResult.builder().valid(false).build();
      return validationResult;
    }

    ConnectorResponseDTO connectorDTO = connectorMapper.writeDTO(connector);
    ConnectorInfoDTO connectorInfo = connectorDTO.getConnector();
    GitConfigDTO gitConfigDTO = (GitConfigDTO) connectorInfo.getConnectorConfig();
    // Use Repo URL from parameter instead of using configured URL
    gitConfigDTO.setUrl(gitRepoURL);
    connectorInfo.setConnectorConfig(gitConfigDTO);

    return validateConnector(
        connector, connectorDTO, connectorInfo, accountIdentifier, orgIdentifier, projectIdentifier);
  }

  private ConnectorValidationResult validateConnector(Connector connector, ConnectorResponseDTO connectorDTO,
      ConnectorInfoDTO connectorInfo, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    ConnectorValidationResult validationResult;
    validationResult = validateSafely(connectorInfo, accountIdentifier, orgIdentifier, projectIdentifier);
    long connectivityTestedAt = System.currentTimeMillis();
    validationResult.setTestedAt(connectivityTestedAt);
    updateConnectivityStatusOfConnector(connector, validationResult, connectivityTestedAt, connectorDTO.getStatus());
    return validationResult;
  }

  private Connector getConnectorWithIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);

    Optional<Connector> connectorOptional =
        connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(fullyQualifiedIdentifier, true);

    if (connectorOptional.isPresent()) {
      return connectorOptional.get();

    } else {
      throw new InvalidRequestException(
          createConnectorNotFoundMessage(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier));
    }
  }

  private ConnectorValidationResult validateSafely(
      ConnectorInfoDTO connectorInfo, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    ConnectionValidator connectionValidator = connectionValidatorMap.get(connectorInfo.getConnectorType().toString());
    ConnectorValidationResult validationResult;
    try {
      validationResult = connectionValidator.validate(
          connectorInfo.getConnectorConfig(), accountIdentifier, orgIdentifier, projectIdentifier);
    } catch (Exception ex) {
      log.info("Test Connection failed for connector with identifier[{}] in account[{}] with error [{}]",
          connectorInfo.getIdentifier(), accountIdentifier, ex.getMessage());
      validationResult = ConnectorValidationResult.builder().valid(false).build();
    }
    return validationResult;
  }

  private void updateConnectivityStatusOfConnector(Connector connector,
      ConnectorValidationResult connectorValidationResult, long connectivityTestedAt,
      ConnectorConnectivityDetails lastStatus) {
    if (connectorValidationResult != null) {
      ConnectorConnectivityDetailsBuilder connectorConnectivityDetailsBuilder =
          ConnectorConnectivityDetails.builder()
              .status(connectorValidationResult.isValid() ? SUCCESS : FAILURE)
              .errorMessage(connectorValidationResult.getErrorMessage())
              .lastTestedAt(connectivityTestedAt);
      if (connectorValidationResult.isValid()) {
        connectorConnectivityDetailsBuilder.lastConnectedAt(connectivityTestedAt);
      } else {
        connectorConnectivityDetailsBuilder.lastConnectedAt(lastStatus == null ? 0 : lastStatus.getLastConnectedAt());
      }
      connector.setConnectivityDetails(connectorConnectivityDetailsBuilder.build());
      connectorRepository.save(connector);
    }
  }

  @Override
  public ConnectorCatalogueResponseDTO getConnectorCatalogue() {
    return ConnectorCatalogueResponseDTO.builder()
        .catalogue(catalogueHelper.getConnectorTypeToCategoryMapping())
        .build();
  }

  @Override
  public void updateConnectorEntityWithPerpetualtaskId(
      String accountIdentifier, ConnectorInfoDTO connector, String perpetualTaskId) {
    try {
      String fqn = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
          accountIdentifier, connector.getOrgIdentifier(), connector.getProjectIdentifier(), connector.getIdentifier());
      Criteria criteria = new Criteria();
      criteria.and(ConnectorKeys.fullyQualifiedIdentifier).is(fqn);
      Update update = new Update();
      update.set(ConnectorKeys.heartbeatPerpetualTaskId, perpetualTaskId);
      connectorRepository.update(new Query(criteria), update);
    } catch (Exception ex) {
      log.info("{} Exception while saving perpetual task id for the {}", CONNECTOR_HEARTBEAT_LOG_PREFIX,
          String.format(CONNECTOR_STRING, connector.getIdentifier(), accountIdentifier, connector.getOrgIdentifier(),
              connector.getProjectIdentifier()),
          ex);
    }
  }

  @Override
  public ConnectorStatistics getConnectorStatistics(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, Scope scope) {
    return connectorStatisticsHelper.getStats(accountIdentifier, orgIdentifier, projectIdentifier, scope);
  }
}
