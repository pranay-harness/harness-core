package io.harness.perpetualtask.connector;

import static io.harness.NGConstants.CONNECTOR_HEARTBEAT_LOG_PREFIX;
import static io.harness.NGConstants.CONNECTOR_STRING;
import static io.harness.annotations.dev.HarnessTeam.DX;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.network.SafeHttpCall.execute;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.NoOpConnectorValidationHandler;
import io.harness.delegate.beans.connector.ConnectorHeartbeatDelegateResponse;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.NoOpConnectorValidationParams;
import io.harness.connector.ConnectorValidationHandler;
import io.harness.grpc.utils.AnyUtils;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.perpetualtask.PerpetualTaskExecutionParams;
import io.harness.perpetualtask.PerpetualTaskExecutor;
import io.harness.perpetualtask.PerpetualTaskId;
import io.harness.perpetualtask.PerpetualTaskResponse;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(DX)
public class ConnectorHeartbeatPerpetualTaskExecutor implements PerpetualTaskExecutor {
  Map<String, ConnectorValidationHandler> connectorTypeToConnectorValidationHandlerMap;
  private KryoSerializer kryoSerializer;
  private DelegateAgentManagerClient delegateAgentManagerClient;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    final ConnectorHeartbeatTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), ConnectorHeartbeatTaskParams.class);
    String accountId = taskParams.getAccountIdentifier();
    String orgIdentifier = taskParams.getOrgIdentifier().getValue();
    String projectIdentifier = taskParams.getProjectIdentifier().getValue();
    String connectorIdentifier = taskParams.getConnectorIdentifier();
    final ConnectorValidationParams connectorValidationParams =
        (ConnectorValidationParams) kryoSerializer.asObject(taskParams.getConnectorValidationParams().toByteArray());
    String connectorMessage =
        String.format(CONNECTOR_STRING, connectorIdentifier, accountId, orgIdentifier, projectIdentifier);

    ConnectorValidationHandler connectorValidationHandler;
    if (connectorValidationParams instanceof NoOpConnectorValidationParams) {
      connectorValidationHandler = new NoOpConnectorValidationHandler();
    } else {
      connectorValidationHandler = connectorTypeToConnectorValidationHandlerMap.get(
          connectorValidationParams.getConnectorType().getDisplayName());
    }
    if (connectorValidationHandler == null || connectorValidationHandler instanceof NoOpConnectorValidationHandler) {
      log.info("The connector validation handler is not registered for the connector.");
      return getPerpetualTaskResponse(null);
    }
    ConnectorValidationResult connectorValidationResult =
        connectorValidationHandler.validate(connectorValidationParams, accountId);
    connectorValidationResult.setTestedAt(System.currentTimeMillis());
    try {
      execute(delegateAgentManagerClient.publishConnectorHeartbeatResult(taskId.getId(), accountId,
          createHeartbeatResponse(accountId, orgIdentifier, projectIdentifier, connectorIdentifier,
              connectorValidationParams, connectorValidationResult)));
    } catch (Exception ex) {
      log.error("{} Failed to publish connector heartbeat result for task [{}] for the connector:[{}]", taskId.getId(),
          CONNECTOR_HEARTBEAT_LOG_PREFIX, connectorMessage, ex);
    }
    log.info("Completed validation task for {}", connectorMessage);
    return getPerpetualTaskResponse(connectorValidationResult);
  }

  private ConnectorHeartbeatDelegateResponse createHeartbeatResponse(String accountId, String orgId, String projectId,
      String identifier, ConnectorValidationParams connectorValidationParams,
      ConnectorValidationResult validationResult) {
    return ConnectorHeartbeatDelegateResponse.builder()
        .accountIdentifier(accountId)
        .orgIdentifier(orgId)
        .projectIdentifier(projectId)
        .identifier(identifier)
        .connectorValidationResult(validationResult)
        .name(connectorValidationParams.getConnectorName())
        .build();
  }

  private PerpetualTaskResponse getPerpetualTaskResponse(ConnectorValidationResult connectorValidationResult) {
    String message = "success";
    if (connectorValidationResult == null) {
      message = "Got Null connector validation result";
    } else if (connectorValidationResult.getStatus() != ConnectivityStatus.SUCCESS) {
      message = connectorValidationResult.getErrorSummary();
    }
    return PerpetualTaskResponse.builder().responseCode(Response.SC_OK).responseMessage(message).build();
  }

  private String getErrorMessage(List<ErrorDetail> errors) {
    if (isNotEmpty(errors) && errors.size() == 1) {
      return errors.get(0).getMessage();
    }
    return "Invalid Credentials";
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}