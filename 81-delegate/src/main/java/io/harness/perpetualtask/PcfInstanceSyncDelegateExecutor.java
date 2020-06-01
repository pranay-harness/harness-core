package io.harness.perpetualtask;

import static io.harness.network.SafeHttpCall.execute;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;

import io.harness.delegate.command.CommandExecutionResult;
import io.harness.grpc.utils.AnyUtils;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.PcfInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import io.jsonwebtoken.lang.Collections;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;
import software.wings.beans.PcfConfig;
import software.wings.delegatetasks.pcf.PcfDelegateTaskHelper;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfInstanceSyncRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfInstanceSyncResponse;
import software.wings.service.InstanceSyncConstants;

import java.time.Instant;
import java.util.List;

@Slf4j
@Singleton
public class PcfInstanceSyncDelegateExecutor implements PerpetualTaskExecutor {
  @Inject PcfDelegateTaskHelper pcfDelegateTaskHelper;
  @Inject DelegateAgentManagerClient delegateAgentManagerClient;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    logger.info("Running the InstanceSync perpetual task executor for task id: {}", taskId);
    PcfInstanceSyncPerpetualTaskParams instanceSyncParams =
        AnyUtils.unpack(params.getCustomizedParams(), PcfInstanceSyncPerpetualTaskParams.class);
    String applicationName = instanceSyncParams.getApplicationName();
    String orgName = instanceSyncParams.getOrgName();
    String space = instanceSyncParams.getSpace();

    PcfConfig pcfConfig = (PcfConfig) KryoUtils.asObject(instanceSyncParams.getPcfConfig().toByteArray());

    ByteString encryptedData = instanceSyncParams.getEncryptedData();

    List<EncryptedDataDetail> encryptedDataDetailList =
        (List<EncryptedDataDetail>) KryoUtils.asObject(encryptedData.toByteArray());

    PcfInstanceSyncRequest pcfInstanceSyncRequest =
        PcfInstanceSyncRequest.builder()
            .pcfConfig(pcfConfig)
            .pcfApplicationName(applicationName)
            .organization(orgName)
            .space(space)
            .pcfCommandType(PcfCommandRequest.PcfCommandType.APP_DETAILS)
            .timeoutIntervalInMin(InstanceSyncConstants.TIMEOUT_SECONDS / 60)
            .build();
    PcfCommandExecutionResponse pcfCommandExecutionResponse =
        pcfDelegateTaskHelper.getPcfCommandExecutionResponse(pcfInstanceSyncRequest, encryptedDataDetailList);

    PcfInstanceSyncResponse pcfInstanceSyncResponse =
        (PcfInstanceSyncResponse) pcfCommandExecutionResponse.getPcfCommandResponse();
    try {
      int instanceSize = Collections.size(pcfInstanceSyncResponse.getInstanceIndices());
      logger.info("Found {} number of instances pcf deployment", instanceSize);
      execute(delegateAgentManagerClient.publishInstanceSyncResult(
          taskId.getId(), pcfConfig.getAccountId(), pcfCommandExecutionResponse));
    } catch (Exception ex) {
      logger.error(
          "Failed to publish the instance collection result to manager for application name {} and PerpetualTaskId {}",
          applicationName, taskId.getId(), ex);
    }
    CommandExecutionResult.CommandExecutionStatus commandExecutionStatus =
        pcfCommandExecutionResponse.getCommandExecutionStatus();
    logger.info("Published instanceSync successfully for perp task: {}, state: {}", taskId, commandExecutionStatus);
    return getPerpetualTaskResponse(pcfCommandExecutionResponse, commandExecutionStatus);
  }

  private PerpetualTaskResponse getPerpetualTaskResponse(PcfCommandExecutionResponse pcfCommandExecutionResponse,
      CommandExecutionResult.CommandExecutionStatus commandExecutionStatus) {
    PerpetualTaskState taskState;
    String message;
    if (CommandExecutionResult.CommandExecutionStatus.FAILURE.equals(commandExecutionStatus)) {
      taskState = PerpetualTaskState.TASK_RUN_FAILED;
      message = pcfCommandExecutionResponse.getErrorMessage();
    } else {
      taskState = PerpetualTaskState.TASK_RUN_SUCCEEDED;
      message = PerpetualTaskState.TASK_RUN_SUCCEEDED.name();
    }

    return PerpetualTaskResponse.builder()
        .responseCode(Response.SC_OK)
        .perpetualTaskState(taskState)
        .responseMessage(message)
        .build();
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}
