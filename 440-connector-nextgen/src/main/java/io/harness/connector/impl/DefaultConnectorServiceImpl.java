package io.harness.connector.impl;

import static io.harness.NGConstants.CONNECTOR_HEARTBEAT_LOG_PREFIX;
import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.connector.entities.ConnectivityStatus.FAILURE;
import static io.harness.connector.entities.ConnectivityStatus.SUCCESS;
import static io.harness.utils.RestCallToNGManagerClientUtils.execute;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.beans.IdentifierRef;
import io.harness.connector.ConnectorFilterHelper;
import io.harness.connector.apis.dto.ConnectorCatalogueResponseDTO;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.apis.dto.stats.ConnectorStatistics;
import io.harness.connector.entities.Connector;
import io.harness.connector.entities.Connector.ConnectorKeys;
import io.harness.connector.entities.Connector.Scope;
import io.harness.connector.entities.ConnectorConnectivityDetails;
import io.harness.connector.entities.ConnectorConnectivityDetails.ConnectorConnectivityDetailsBuilder;
import io.harness.connector.helper.CatalogueHelper;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationResult;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.exception.DuplicateFieldException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.repositories.ConnectorRepository;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class DefaultConnectorServiceImpl implements ConnectorService {
  private final ConnectorMapper connectorMapper;
  private final ConnectorRepository connectorRepository;
  private final ConnectorFilterHelper connectorFilterHelper;
  private Map<String, ConnectionValidator> connectionValidatorMap;
  private final CatalogueHelper catalogueHelper;
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
  public Page<ConnectorResponseDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType type, ConnectorCategory category) {
    Criteria criteria = connectorFilterHelper.createCriteriaFromConnectorFilter(
        accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type, category);
    Pageable pageable = getPageRequest(page, size, Sort.by(Sort.Direction.DESC, ConnectorKeys.createdAt));
    Page<Connector> connectors = connectorRepository.findAll(criteria, pageable);
    return connectors.map(connector -> connectorMapper.writeDTO(connector));
  }

  private Pageable getPageRequest(int page, int size, Sort sort) {
    return PageRequest.of(page, size, sort);
  }

  @Override
  public ConnectorResponseDTO create(ConnectorDTO connectorRequestDTO, String accountIdentifier) {
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
    newConnector.setStatus(existingConnector.get().getStatus());
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
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Optional<Connector> connectorOptional =
        connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(fullyQualifiedIdentifier, true);
    if (connectorOptional.isPresent()) {
      ConnectorResponseDTO connectorDTO = connectorMapper.writeDTO(connectorOptional.get());
      ConnectorInfoDTO connectorInfo = connectorDTO.getConnector();
      ConnectorValidationResult validationResult =
          validateSafely(connectorInfo, accountIdentifier, orgIdentifier, projectIdentifier);
      long connectivityTestedAt = System.currentTimeMillis();
      validationResult.setTestedAt(connectivityTestedAt);
      updateConnectivityStatusOfConnector(
          connectorOptional.get(), validationResult, connectivityTestedAt, connectorDTO.getStatus());
      return validationResult;
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
      connector.setStatus(connectorConnectivityDetailsBuilder.build());
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
