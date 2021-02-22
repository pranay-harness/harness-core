package io.harness.ng;

import static io.harness.NGConstants.CONNECTOR_HEARTBEAT_LOG_PREFIX;
import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.connector.ConnectivityStatus.SUCCESS;
import static io.harness.connector.ConnectorCategory.SECRET_MANAGER;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;
import static io.harness.exception.WingsException.USER;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.ng.NextGenModule.SECRET_MANAGER_CONNECTOR_SERVICE;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.NgAutoLogContext;
import io.harness.connector.ConnectorActivityDetails;
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
import io.harness.connector.helper.ConnectorLogContext;
import io.harness.connector.impl.ConnectorErrorMessagesHelper;
import io.harness.connector.services.ConnectorActivityService;
import io.harness.connector.services.ConnectorHeartbeatService;
import io.harness.connector.services.ConnectorService;
import io.harness.connector.stats.ConnectorStatistics;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.AutoLogContext;
import io.harness.ng.core.activityhistory.NGActivityType;
import io.harness.perpetualtask.PerpetualTaskId;
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
    PerpetualTaskId connectorHeartbeatTaskId = null;
    try (AutoLogContext ignore1 = new NgAutoLogContext(connector.getConnectorInfo().getProjectIdentifier(),
             connector.getConnectorInfo().getOrgIdentifier(), accountIdentifier, OVERRIDE_ERROR);
         AutoLogContext ignore2 =
             new ConnectorLogContext(connector.getConnectorInfo().getIdentifier(), OVERRIDE_ERROR)) {
      ConnectorInfoDTO connectorInfo = connector.getConnectorInfo();
      boolean isHarnessManagedSecretManager = connectorHeartbeatService.isHarnessManagedSecretManager(connectorInfo);
      if (!isHarnessManagedSecretManager) {
        connectorHeartbeatTaskId = connectorHeartbeatService.createConnectorHeatbeatTask(accountIdentifier,
            connectorInfo.getOrgIdentifier(), connectorInfo.getProjectIdentifier(), connectorInfo.getIdentifier());
      }
      if (connectorHeartbeatTaskId != null || isHarnessManagedSecretManager) {
        ConnectorResponseDTO connectorResponse =
            getConnectorService(connectorInfo.getConnectorType()).create(connector, accountIdentifier);
        if (connectorResponse != null) {
          ConnectorInfoDTO savedConnector = connectorResponse.getConnector();
          createConnectorCreationActivity(accountIdentifier, savedConnector);
          runTestConnectionAsync(connector, accountIdentifier);
          if (connectorHeartbeatTaskId != null) {
            defaultConnectorService.updateConnectorEntityWithPerpetualtaskId(accountIdentifier,
                connectorInfo.getOrgIdentifier(), connectorInfo.getProjectIdentifier(), connectorInfo.getIdentifier(),
                connectorHeartbeatTaskId.getId());
          }
        }
        return connectorResponse;
      } else {
        throw new InvalidRequestException("Connector could not be created because we could not create the heartbeat");
      }
    } catch (Exception ex) {
      if (connectorHeartbeatTaskId != null) {
        ConnectorInfoDTO connectorInfo = connector.getConnectorInfo();
        String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(accountIdentifier,
            connectorInfo.getOrgIdentifier(), connectorInfo.getProjectIdentifier(), connectorInfo.getIdentifier());
        deleteConnectorHeartbeatTask(accountIdentifier, fullyQualifiedIdentifier, connectorHeartbeatTaskId.getId());
      }
      throw ex;
    }
  }

  private void runTestConnectionAsync(ConnectorDTO connectorRequestDTO, String accountIdentifier) {
    // User can create a connector without test connection, in this flow we won't have
    // a status for the connector, to solve this issue we will do one test connection
    // asynchronously
    executorService.submit(() -> validate(connectorRequestDTO, accountIdentifier));
  }

  private void createConnectorCreationActivity(String accountIdentifier, ConnectorInfoDTO connector) {
    try (AutoLogContext ignore1 = new NgAutoLogContext(
             connector.getProjectIdentifier(), connector.getOrgIdentifier(), accountIdentifier, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new ConnectorLogContext(connector.getIdentifier(), OVERRIDE_ERROR);) {
      connectorActivityService.create(accountIdentifier, connector, NGActivityType.ENTITY_CREATION);
    } catch (Exception ex) {
      log.info("Error while creating connector creation activity", ex);
    }
  }

  @Override
  public ConnectorResponseDTO update(@NotNull ConnectorDTO connector, String accountIdentifier) {
    try (AutoLogContext ignore1 = new NgAutoLogContext(connector.getConnectorInfo().getProjectIdentifier(),
             connector.getConnectorInfo().getOrgIdentifier(), accountIdentifier, OVERRIDE_ERROR);
         AutoLogContext ignore2 =
             new ConnectorLogContext(connector.getConnectorInfo().getIdentifier(), OVERRIDE_ERROR)) {
      ConnectorInfoDTO connectorInfo = connector.getConnectorInfo();
      ConnectorResponseDTO connectorResponse =
          getConnectorService(connectorInfo.getConnectorType()).update(connector, accountIdentifier);
      ConnectorInfoDTO savedConnector = connectorResponse.getConnector();
      createConnectorUpdateActivity(accountIdentifier, savedConnector);
      publishEventForConnectorUpdate(accountIdentifier, savedConnector);
      return connectorResponse;
    }
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
    try (AutoLogContext ignore1 =
             new NgAutoLogContext(projectIdentifier, orgIdentifier, accountIdentifier, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new ConnectorLogContext(connectorIdentifier, OVERRIDE_ERROR)) {
      String fullyQualifiedIdentifier = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
          accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
      Optional<Connector> connectorOptional =
          connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(fullyQualifiedIdentifier, true);
      if (connectorOptional.isPresent()) {
        Connector connector = connectorOptional.get();
        boolean isConnectorHeartbeatDeleted = deleteConnectorHeartbeatTask(
            accountIdentifier, fullyQualifiedIdentifier, connector.getHeartbeatPerpetualTaskId());
        if (isConnectorHeartbeatDeleted || connector.getHeartbeatPerpetualTaskId() == null) {
          boolean isConnectorDeleted =
              getConnectorService(connector.getType())
                  .delete(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
          if (isConnectorDeleted) {
            return true;
          } else {
            PerpetualTaskId perpetualTaskId = connectorHeartbeatService.createConnectorHeatbeatTask(
                accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
            updateConnectorEntityWithPerpetualtaskId(
                accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier, perpetualTaskId.getId());
            return false;
          }
        }
        throw new InvalidRequestException("Could not delete connector because heartbeat could not be deleted", USER);
      }
      throw new InvalidRequestException("No such connector found", USER);
    }
  }

  private void deleteConnectorActivities(String accountIdentifier, String connectorFQN) {
    try {
      connectorActivityService.deleteAllActivities(accountIdentifier, connectorFQN);
    } catch (Exception ex) {
      log.info("Error while deleting connector activity", ex);
    }
  }

  private boolean deleteConnectorHeartbeatTask(String accountIdentifier, String connectorFQN, String heartbeatTaskId) {
    if (isNotBlank(heartbeatTaskId)) {
      boolean perpetualTaskIsDeleted =
          connectorHeartbeatService.deletePerpetualTask(accountIdentifier, heartbeatTaskId, connectorFQN);
      if (perpetualTaskIsDeleted == false) {
        log.info("{} The perpetual task could not be deleted {}", CONNECTOR_HEARTBEAT_LOG_PREFIX, connectorFQN);
        return false;
      }
    } else {
      log.info("{} The perpetual task id is empty for the connector {}", CONNECTOR_HEARTBEAT_LOG_PREFIX, connectorFQN);
      return false;
    }
    log.info(
        "{} Deleted the heartbeat perpetual task for the connector {}", CONNECTOR_HEARTBEAT_LOG_PREFIX, connectorFQN);
    return true;
  }

  @Override
  public ConnectorValidationResult validate(@NotNull ConnectorDTO connector, String accountIdentifier) {
    try (AutoLogContext ignore1 = new NgAutoLogContext(connector.getConnectorInfo().getProjectIdentifier(),
             connector.getConnectorInfo().getOrgIdentifier(), accountIdentifier, OVERRIDE_ERROR);
         AutoLogContext ignore2 =
             new ConnectorLogContext(connector.getConnectorInfo().getIdentifier(), OVERRIDE_ERROR)) {
      ConnectorValidationResult validationResult = defaultConnectorService.validate(connector, accountIdentifier);
      ConnectorInfoDTO connectorInfoDTO = connector.getConnectorInfo();
      updateTheConnectorValidationResultInTheEntity(validationResult, accountIdentifier,
          connectorInfoDTO.getOrgIdentifier(), connectorInfoDTO.getProjectIdentifier(),
          connectorInfoDTO.getIdentifier());
      return validationResult;
    }
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
    try (AutoLogContext ignore1 =
             new NgAutoLogContext(projectIdentifier, orgIdentifier, accountIdentifier, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new ConnectorLogContext(connectorIdentifier, OVERRIDE_ERROR)) {
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
  }

  private void updateTheConnectorValidationResultInTheEntity(ConnectorValidationResult connectorValidationResult,
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    Connector connector =
        getConnectorWithIdentifier(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    setConnectivityStatusInConnector(connector, connectorValidationResult, connector.getConnectivityDetails());
    connectorRepository.save(connector);
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

  private void setConnectivityStatusInConnector(Connector connector,
      ConnectorValidationResult connectorValidationResult, ConnectorConnectivityDetails lastStatus) {
    setLastUpdatedTimeIfNotPresent(connector);
    if (connectorValidationResult != null) {
      long connectivityTestedAt = getCurrentTimeIfActivityTimeIsNull(connectorValidationResult.getTestedAt());
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
  public void updateConnectorEntityWithPerpetualtaskId(String accountIdentifier, String connectorOrgIdentifier,
      String connectorProjectIdentifier, String connectorIdentifier, String perpetualTaskId) {
    try (AutoLogContext ignore1 = new NgAutoLogContext(
             connectorProjectIdentifier, connectorOrgIdentifier, accountIdentifier, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new ConnectorLogContext(connectorIdentifier, OVERRIDE_ERROR);) {
      defaultConnectorService.updateConnectorEntityWithPerpetualtaskId(
          accountIdentifier, connectorOrgIdentifier, connectorProjectIdentifier, connectorIdentifier, perpetualTaskId);
    }
  }

  @Override
  public void updateActivityDetailsInTheConnector(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String identifier, ConnectorValidationResult connectorValidationResult,
      Long activityTime) {
    Connector connector = getConnectorWithIdentifier(accountIdentifier, orgIdentifier, projectIdentifier, identifier);
    if (activityTime != null) {
      setActivityDetailInTheConnector(connector, activityTime);
    }
    if (connectorValidationResult != null) {
      setConnectivityStatusInConnector(connector, connectorValidationResult, connector.getConnectivityDetails());
    }
    connectorRepository.save(connector);
  }

  private void setActivityDetailInTheConnector(Connector connector, long activityTime) {
    long lastActivityTime = getCurrentTimeIfActivityTimeIsNull(activityTime);
    ConnectorActivityDetails connectorActivityDetails =
        ConnectorActivityDetails.builder().lastActivityTime(lastActivityTime).build();
    connector.setActivityDetails(connectorActivityDetails);
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
