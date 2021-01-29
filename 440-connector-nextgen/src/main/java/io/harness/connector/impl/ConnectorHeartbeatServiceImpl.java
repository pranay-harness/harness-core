package io.harness.connector.impl;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.CONNECTOR_IDENTIFIER_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.NGConstants.CONNECTOR_HEARTBEAT_LOG_PREFIX;
import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.heartbeat.ConnectorValidationParamsProvider;
import io.harness.connector.services.ConnectorHeartbeatService;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.AccountId;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.gcpkmsconnector.GcpKmsConnectorDTO;
import io.harness.delegate.beans.connector.localconnector.LocalConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.grpc.DelegateServiceGrpcClient;
import io.harness.perpetualtask.PerpetualTaskClientContextDetails;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskSchedule;
import io.harness.perpetualtask.TaskClientParams;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.util.Durations;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;

@Singleton
public class ConnectorHeartbeatServiceImpl implements ConnectorHeartbeatService {
  private static final Logger log = org.slf4j.LoggerFactory.getLogger(ConnectorHeartbeatServiceImpl.class);
  private final long CONNECTIVITY_CHECK_INTERVAL = 10;
  private final long CONNECTIVITY_CHECK_TIMEOUT = 2;
  DelegateServiceGrpcClient delegateServiceGrpcClient;
  ConnectorService connectorService;
  private Map<String, ConnectorValidationParamsProvider> connectorValidationParamsProviderMap;

  @Inject
  public ConnectorHeartbeatServiceImpl(DelegateServiceGrpcClient delegateServiceGrpcClient,
      @Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService connectorService,
      Map<String, ConnectorValidationParamsProvider> connectorValidationParamsProviderMap) {
    this.delegateServiceGrpcClient = delegateServiceGrpcClient;
    this.connectorService = connectorService;
    this.connectorValidationParamsProviderMap = connectorValidationParamsProviderMap;
  }

  private PerpetualTaskId createPerpetualTask(ConnectorInfoDTO connector, String accountIdentifier) {
    try {
      AccountId accountId = AccountId.newBuilder().setId(accountIdentifier).build();
      PerpetualTaskSchedule perpetualTaskSchedule = PerpetualTaskSchedule.newBuilder()
                                                        .setInterval(Durations.fromMinutes(CONNECTIVITY_CHECK_INTERVAL))
                                                        .setTimeout(Durations.fromMinutes(CONNECTIVITY_CHECK_TIMEOUT))
                                                        .build();
      Map<String, String> clientParamsMap = new HashMap<>();
      clientParamsMap.put(ACCOUNT_KEY, accountIdentifier);
      if (isNotBlank(connector.getOrgIdentifier())) {
        clientParamsMap.put(ORG_KEY, connector.getOrgIdentifier());
      }
      if (isNotBlank(connector.getProjectIdentifier())) {
        clientParamsMap.put(PROJECT_KEY, connector.getProjectIdentifier());
      }
      clientParamsMap.put(CONNECTOR_IDENTIFIER_KEY, connector.getIdentifier());
      TaskClientParams clientParams = TaskClientParams.newBuilder().putAllParams(clientParamsMap).build();
      PerpetualTaskClientContextDetails taskContext =
          PerpetualTaskClientContextDetails.newBuilder().setTaskClientParams(clientParams).build();
      return delegateServiceGrpcClient.createPerpetualTask(accountId, "CONNECTOR_TEST_CONNECTION",
          perpetualTaskSchedule, taskContext, false, "Connector Test Connection Task");
    } catch (Exception ex) {
      log.info("{} Error Creating Perpetual task for the connector {} in account {}, org {} with taskId {}",
          CONNECTOR_HEARTBEAT_LOG_PREFIX, connector.getIdentifier(), connector.getOrgIdentifier(),
          connector.getProjectIdentifier(), ex);
      return null;
    }
  }

  @Override
  public PerpetualTaskId createConnectorHeatbeatTask(String accountIdentifier, ConnectorInfoDTO connector) {
    if (isHarnessManagedSecretManager(connector)) {
      return null;
    }
    PerpetualTaskId perpetualTaskId = createPerpetualTask(connector, accountIdentifier);
    if (perpetualTaskId == null || perpetualTaskId.getId() == null) {
      log.info("{} Error in creating Perpetual task for the {}", CONNECTOR_HEARTBEAT_LOG_PREFIX,
          String.format(CONNECTOR_STRING, connector.getIdentifier(), accountIdentifier, connector.getOrgIdentifier(),
              connector.getProjectIdentifier()));
      return null;
    }
    connectorService.updateConnectorEntityWithPerpetualtaskId(accountIdentifier, connector, perpetualTaskId.getId());
    log.info("{} Created perpetual task for the {} with the id {}", CONNECTOR_HEARTBEAT_LOG_PREFIX,
        String.format(CONNECTOR_STRING, connector.getIdentifier(), accountIdentifier, connector.getOrgIdentifier(),
            connector.getProjectIdentifier()),
        perpetualTaskId.getId());
    return perpetualTaskId;
  }

  private boolean isHarnessManagedSecretManager(ConnectorInfoDTO connector) {
    switch (connector.getConnectorType()) {
      case GCP_KMS:
        return ((GcpKmsConnectorDTO) connector.getConnectorConfig()).isHarnessManaged();
      case LOCAL:
        return ((LocalConnectorDTO) connector.getConnectorConfig()).isHarnessManaged();
      default:
        return false;
    }
  }

  @Override
  public void deletePerpetualTask(String accountIdentifier, String perpetualTaskId, String connectorFQN) {
    try {
      delegateServiceGrpcClient.deletePerpetualTask(AccountId.newBuilder().setId(accountIdentifier).build(),
          PerpetualTaskId.newBuilder().setId(perpetualTaskId).build());
    } catch (Exception ex) {
      log.info("{} Exception while deleting the heartbeat task for the connector {}", CONNECTOR_HEARTBEAT_LOG_PREFIX,
          connectorFQN);
    }
  }

  @Override
  public ConnectorValidationParams getConnectorValidationParams(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifier) {
    final Optional<ConnectorResponseDTO> connectorResponseDTO =
        connectorService.get(accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    return connectorResponseDTO
        .map(connectorResponse -> {
          final ConnectorType connectorType = connectorResponse.getConnector().getConnectorType();
          return connectorValidationParamsProviderMap.get(connectorType.getDisplayName())
              .getConnectorValidationParams(connectorResponse.getConnector(),
                  connectorResponse.getConnector().getName(), accountIdentifier, orgIdentifier, projectIdentifier);
        })
        .orElseThrow(() -> new InvalidRequestException("Connector doesn't exist"));
  }
}