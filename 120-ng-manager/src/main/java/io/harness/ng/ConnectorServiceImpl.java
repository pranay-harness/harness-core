package io.harness.ng;

import static io.harness.NGConstants.CONNECTOR_HEARTBEAT_LOG_PREFIX;
import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.connector.ConnectivityStatus.SUCCESS;
import static io.harness.connector.ConnectorCategory.SECRET_MANAGER;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.exception.WingsException.USER;
import static io.harness.ng.NextGenModule.SECRET_MANAGER_CONNECTOR_SERVICE;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.connector.ConnectorCatalogueResponseDTO;
import io.harness.connector.ConnectorCategory;
import io.harness.connector.ConnectorConnectivityDetails;
import io.harness.connector.ConnectorConnectivityDetails.ConnectorConnectivityDetailsBuilder;
import io.harness.connector.ConnectorDTO;
import io.harness.connector.ConnectorFilterPropertiesDTO;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorRegistryFactory;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.entities.Connector;
import io.harness.connector.impl.ConnectorErrorMessagesHelper;
import io.harness.connector.services.ConnectorActivityService;
import io.harness.connector.services.ConnectorHeartbeatService;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.stats.ConnectorStatistics;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.activityhistory.NGActivityType;
import io.harness.repositories.ConnectorRepository;
import io.harness.utils.FullyQualifiedIdentifierHelper;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;

@Slf4j
@Singleton
public class ConnectorServiceImpl implements ConnectorService {
  private final ConnectorService defaultConnectorService;
  private final ConnectorService secretManagerConnectorService;
  private final ConnectorActivityService connectorActivityService;
  private final ConnectorHeartbeatService connectorHeartbeatService;
  private final ConnectorRepository connectorRepository;
  private final Producer eventProducer;
  private final ExecutorService executorService;
  private final ConnectorErrorMessagesHelper connectorErrorMessagesHelper;

  @Inject
  public ConnectorServiceImpl(@Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService defaultConnectorService,
      @Named(SECRET_MANAGER_CONNECTOR_SERVICE) ConnectorService secretManagerConnectorService,
      ConnectorActivityService connectorActivityService, ConnectorHeartbeatService connectorHeartbeatService,
      ConnectorRepository connectorRepository, @Named(EventsFrameworkConstants.ENTITY_CRUD) Producer eventProducer,
      ExecutorService executorService, ConnectorErrorMessagesHelper connectorErrorMessagesHelper) {
    this.defaultConnectorService = defaultConnectorService;
    this.secretManagerConnectorService = secretManagerConnectorService;
    this.connectorActivityService = connectorActivityService;
    this.connectorHeartbeatService = connectorHeartbeatService;
    this.connectorRepository = connectorRepository;
    this.eventProducer = eventProducer;
    this.executorService = executorService;
    this.connectorErrorMessagesHelper = connectorErrorMessagesHelper;
  }

  private ConnectorService getConnectorService(ConnectorType connectorType) {
    if (ConnectorRegistryFactory.getConnectorCategory(connectorType).equals(SECRET_MANAGER)) {
      return secretManagerConnectorService;
    }
    return defaultConnectorService;
  }

  @Override
  public Optional<ConnectorResponseDTO> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    return defaultConnectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
  }

  @Override
  public ConnectorResponseDTO create(@NotNull ConnectorDTO connector, String accountIdentifier) {
    ConnectorInfoDTO connectorInfo = connector.getConnectorInfo();
    ConnectorResponseDTO connectorResponse =
        getConnectorService(connectorInfo.getConnectorType()).create(connector, accountIdentifier);
    ConnectorInfoDTO savedConnector = connectorResponse.getConnector();
    createConnectorCreationActivity(accountIdentifier, savedConnector);
    connectorHeartbeatService.createConnectorHeatbeatTask(accountIdentifier, savedConnector);
    runTestConnectionAsync(connector, accountIdentifier);
    return connectorResponse;
  }

  private void runTestConnectionAsync(ConnectorDTO connectorRequestDTO, String accountIdentifier) {
    // User can create a connector without test connection, in this flow we won't have
    // a status for the connector, to solve this issue we will do one test connection
    // asynchronously
    executorService.submit(() -> validate(connectorRequestDTO, accountIdentifier));
  }

  private void createConnectorCreationActivity(String accountIdentifier, ConnectorInfoDTO connector) {
    try {
      connectorActivityService.create(accountIdentifier, connector, NGActivityType.ENTITY_CREATION);
    } catch (Exception ex) {
      log.info("Error while creating connector creation activity", ex);
    }
  }

  @Override
  public ConnectorResponseDTO update(@NotNull ConnectorDTO connector, String accountIdentifier) {
    ConnectorInfoDTO connectorInfo = connector.getConnectorInfo();
    ConnectorResponseDTO connectorResponse =
        getConnectorService(connectorInfo.getConnectorType()).update(connector, accountIdentifier);
    ConnectorInfoDTO savedConnector = connectorResponse.getConnector();
    createConnectorUpdateActivity(accountIdentifier, savedConnector);
    publishEventForConnectorUpdate(accountIdentifier, savedConnector);
    return connectorResponse;
  }

  private void publishEventForConnectorUpdate(String accountIdentifier, ConnectorInfoDTO savedConnector) {
    try {
      EntityChangeDTO.Builder connectorUpdateDTOBuilder =
          EntityChangeDTO.newBuilder()
              .setAccountIdentifier(StringValue.of(accountIdentifier))
              .setIdentifier(StringValue.of(savedConnector.getIdentifier()));
      if (isNotBlank(savedConnector.getOrgIdentifier())) {
        connectorUpdateDTOBuilder.setOrgIdentifier(StringValue.of(savedConnector.getOrgIdentifier()));
      }
      if (isNotBlank(savedConnector.getProjectIdentifier())) {
        connectorUpdateDTOBuilder.setProjectIdentifier(StringValue.of(savedConnector.getProjectIdentifier()));
      }
      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", accountIdentifier,
                  EventsFrameworkMetadataConstants.ENTITY_TYPE, EventsFrameworkMetadataConstants.CONNECTOR_ENTITY,
                  EventsFrameworkMetadataConstants.ACTION, EventsFrameworkMetadataConstants.UPDATE_ACTION))
              .setData(connectorUpdateDTOBuilder.build().toByteString())
              .build());
    } catch (Exception ex) {
      log.info("Exception while publishing the event of connector update for {}",
          String.format(CONNECTOR_STRING, savedConnector.getIdentifier(), accountIdentifier,
              savedConnector.getOrgIdentifier(), savedConnector.getProjectIdentifier()));
    }
  }

  private void createConnectorUpdateActivity(String accountIdentifier, ConnectorInfoDTO connector) {
    try {
      connectorActivityService.create(accountIdentifier, connector, NGActivityType.ENTITY_UPDATE);
    } catch (Exception ex) {
      log.info("Error while creating connector update activity", ex);
    }
  }

  @Override
  public boolean delete(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    Optional<Connector> connectorOptional =
        connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(fullyQualifiedIdentifier, true);
    if (connectorOptional.isPresent()) {
      Connector connector = connectorOptional.get();
      boolean isConnectorDeleted =
          getConnectorService(connector.getType())
              .delete(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
      deleteConnectorHeartbeatTask(
          accountIdentifier, fullyQualifiedIdentifier, connector.getHeartbeatPerpetualTaskId());
      deleteConnectorActivities(accountIdentifier, fullyQualifiedIdentifier);
      return isConnectorDeleted;
    }
    throw new InvalidRequestException("No such connector found", USER);
  }

  private void deleteConnectorActivities(String accountIdentifier, String connectorFQN) {
    try {
      connectorActivityService.deleteAllActivities(accountIdentifier, connectorFQN);
    } catch (Exception ex) {
      log.info("Error while deleting connector activity", ex);
    }
  }

  private void deleteConnectorHeartbeatTask(String accountIdentifier, String connectorFQN, String heartbeatTaskId) {
    if (isNotBlank(heartbeatTaskId)) {
      connectorHeartbeatService.deletePerpetualTask(accountIdentifier, heartbeatTaskId, connectorFQN);
    } else {
      log.info("{} The perpetual task id is empty for the connector {}", CONNECTOR_HEARTBEAT_LOG_PREFIX, connectorFQN);
    }
    log.info(
        "{} Deleted the heartbeat perpetual task for the connector {}", CONNECTOR_HEARTBEAT_LOG_PREFIX, connectorFQN);
  }

  @Override
  public ConnectorValidationResult validate(@NotNull ConnectorDTO connector, String accountIdentifier) {
    return defaultConnectorService.validate(connector, accountIdentifier);
  }

  @Override
  public boolean validateTheIdentifierIsUnique(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    return defaultConnectorService.validateTheIdentifierIsUnique(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
  }

  @Override
  public ConnectorValidationResult testConnection(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    Optional<ConnectorResponseDTO> connectorDTO =
        get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    if (connectorDTO.isPresent()) {
      ConnectorResponseDTO connectorResponse = connectorDTO.get();
      ConnectorInfoDTO connectorInfoDTO = connectorResponse.getConnector();
      ConnectorValidationResult connectorValidationResult =
          getConnectorService(connectorInfoDTO.getConnectorType())
              .testConnection(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
      updateTheConnectorValidationResultInTheEntity(
          connectorValidationResult, accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
      return connectorValidationResult;
    } else {
      throw new InvalidRequestException(connectorErrorMessagesHelper.createConnectorNotFoundMessage(
                                            accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier),
          USER);
    }
  }

  private void updateTheConnectorValidationResultInTheEntity(ConnectorValidationResult connectorValidationResult,
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    Connector connector =
        getConnectorWithIdentifier(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    long connectivityTestedAt = getCurrentTimeIfActivityTimeIsNull(connectorValidationResult.getTestedAt());
    connectorValidationResult.setTestedAt(connectivityTestedAt);
    setAndSaveNewConnectivityStatusInConnector(
        connector, connectorValidationResult, connectivityTestedAt, connector.getConnectivityDetails());
  }

  private Connector getConnectorWithIdentifier(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);

    Optional<Connector> connectorOptional =
        connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(fullyQualifiedIdentifier, true);

    return connectorOptional.orElseThrow(
        ()
            -> new InvalidRequestException(connectorErrorMessagesHelper.createConnectorNotFoundMessage(
                accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier)));
  }

  private void setAndSaveNewConnectivityStatusInConnector(Connector connector,
      ConnectorValidationResult connectorValidationResult, long connectivityTestedAt,
      ConnectorConnectivityDetails lastStatus) {
    setLastUpdatedTimeIfNotPresent(connector);
    if (connectorValidationResult != null) {
      ConnectorConnectivityDetailsBuilder connectorConnectivityDetailsBuilder =
          ConnectorConnectivityDetails.builder()
              .status(connectorValidationResult.getStatus())
              .testedAt(connectivityTestedAt);
      if (connectorValidationResult.getStatus() == SUCCESS) {
        connectorConnectivityDetailsBuilder.lastConnectedAt(connectivityTestedAt);
      } else {
        connectorConnectivityDetailsBuilder.lastConnectedAt(lastStatus == null ? 0 : lastStatus.getLastConnectedAt())
            .errorSummary(connectorValidationResult.getErrorSummary())
            .errors(connectorValidationResult.getErrors());
      }
      connector.setConnectivityDetails(connectorConnectivityDetailsBuilder.build());
      connectorRepository.save(connector);
    }
  }

  private void setLastUpdatedTimeIfNotPresent(Connector connector) {
    if (connector.getTimeWhenConnectorIsLastUpdated() == null) {
      connector.setTimeWhenConnectorIsLastUpdated(connector.getCreatedAt());
    }
  }

  private long getCurrentTimeIfActivityTimeIsNull(long activityTime) {
    if (activityTime == 0L) {
      return System.currentTimeMillis();
    }
    return activityTime;
  }

  @Override
  public void updateConnectorEntityWithPerpetualtaskId(
      String accountIdentifier, ConnectorInfoDTO connector, String perpetualTaskId) {
    defaultConnectorService.updateConnectorEntityWithPerpetualtaskId(accountIdentifier, connector, perpetualTaskId);
  }

  @Override
  public void updateConnectivityDetailOfTheConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, ConnectorValidationResult connectorValidationResult) {
    if (connectorValidationResult == null) {
      log.info("Got null validation result for the {}",
          String.format(CONNECTOR_STRING, identifier, accountIdentifier, orgIdentifier, projectIdentifier));
      return;
    }
    long testingTime = connectorValidationResult.getTestedAt() != 0L ? connectorValidationResult.getTestedAt()
                                                                     : System.currentTimeMillis();
    Connector connector = getConnectorWithIdentifier(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    setAndSaveNewConnectivityStatusInConnector(
        connector, connectorValidationResult, testingTime, connector.getConnectivityDetails());
  }

  @Override
  public ConnectorCatalogueResponseDTO getConnectorCatalogue() {
    return defaultConnectorService.getConnectorCatalogue();
  }

  @Override
  public ConnectorValidationResult testGitRepoConnection(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String connectorIdentifier, String gitRepoURL) {
    return defaultConnectorService.testGitRepoConnection(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, gitRepoURL);
  }

  @Override
  public ConnectorStatistics getConnectorStatistics(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return defaultConnectorService.getConnectorStatistics(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  @Override
  public Page<ConnectorResponseDTO> list(int page, int size, String accountIdentifier,
      ConnectorFilterPropertiesDTO filterProperties, String orgIdentifier, String projectIdentifier,
      String filterIdentifier, String searchTerm, Boolean includeAllConnectorsAccessibleAtScope) {
    return defaultConnectorService.list(page, size, accountIdentifier, filterProperties, orgIdentifier,
        projectIdentifier, filterIdentifier, searchTerm, includeAllConnectorsAccessibleAtScope);
  }

  @Override
  public Page<ConnectorResponseDTO> list(int page, int size, String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String searchTerm, ConnectorType type, ConnectorCategory category) {
    return defaultConnectorService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, searchTerm, type, category);
  }

  @Override
  public List<ConnectorResponseDTO> listbyFQN(String accountIdentifier, List<String> connectorFQN) {
    return defaultConnectorService.listbyFQN(accountIdentifier, connectorFQN);
  }
}
