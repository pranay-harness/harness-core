package io.harness.connector.impl;

import static io.harness.NGCommonEntityConstants.ACCOUNT_KEY;
import static io.harness.NGCommonEntityConstants.CONNECTOR_IDENTIFIER_KEY;
import static io.harness.NGCommonEntityConstants.ORG_KEY;
import static io.harness.NGCommonEntityConstants.PROJECT_KEY;
import static io.harness.NGConstants.CONNECTOR_HEARTBEAT_LOG_PREFIX;
import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.services.ConnectorHeartbeatService;
import io.harness.connector.services.ConnectorService;
import io.harness.delegate.AccountId;
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
import org.slf4j.Logger;

@Singleton
public class ConnectorHeartbeatServiceImpl implements ConnectorHeartbeatService {
  private static final Logger log = org.slf4j.LoggerFactory.getLogger(ConnectorHeartbeatServiceImpl.class);
  private final long CONNECTIVITY_CHECK_INTERVAL = 30;
  private final long CONNECTIVITY_CHECK_TIMEOUT = 2;
  DelegateServiceGrpcClient delegateServiceGrpcClient;
  ConnectorService connectorService;

  @Inject
  public ConnectorHeartbeatServiceImpl(DelegateServiceGrpcClient delegateServiceGrpcClient,
      @Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService connectorService) {
    this.delegateServiceGrpcClient = delegateServiceGrpcClient;
    this.connectorService = connectorService;
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
}