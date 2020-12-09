package io.harness.perpetualtask;

import static io.harness.network.SafeHttpCall.execute;

import io.harness.grpc.utils.AnyUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.PcfInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;

import software.wings.beans.PcfConfig;
import software.wings.delegatetasks.pcf.PcfDelegateTaskHelper;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfInstanceSyncRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;
import software.wings.helpers.ext.pcf.response.PcfInstanceSyncResponse;
import software.wings.service.InstanceSyncConstants;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import io.jsonwebtoken.lang.Collections;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;

@Slf4j
@Singleton
public class PcfInstanceSyncDelegateExecutor implements PerpetualTaskExecutor {
  @Inject PcfDelegateTaskHelper pcfDelegateTaskHelper;
  @Inject DelegateAgentManagerClient delegateAgentManagerClient;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    log.info("Running the InstanceSync perpetual task executor for task id: {}", taskId);
    PcfInstanceSyncPerpetualTaskParams instanceSyncParams =
        AnyUtils.unpack(params.getCustomizedParams(), PcfInstanceSyncPerpetualTaskParams.class);
    String applicationName = instanceSyncParams.getApplicationName();
    String orgName = instanceSyncParams.getOrgName();
    String space = instanceSyncParams.getSpace();

    PcfConfig pcfConfig = (PcfConfig) kryoSerializer.asObject(instanceSyncParams.getPcfConfig().toByteArray());

    ByteString encryptedData = instanceSyncParams.getEncryptedData();

    List<EncryptedDataDetail> encryptedDataDetailList =
        (List<EncryptedDataDetail>) kryoSerializer.asObject(encryptedData.toByteArray());

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
        pcfDelegateTaskHelper.getPcfCommandExecutionResponse(pcfInstanceSyncRequest, encryptedDataDetailList, true);

    PcfInstanceSyncResponse pcfInstanceSyncResponse =
        (PcfInstanceSyncResponse) pcfCommandExecutionResponse.getPcfCommandResponse();
    try {
      if (pcfInstanceSyncResponse == null) {
        pcfInstanceSyncResponse = PcfInstanceSyncResponse.builder()
                                      .commandExecutionStatus(CommandExecutionStatus.FAILURE)
                                      .name(applicationName)
                                      .organization(orgName)
                                      .space(space)
                                      .output("Null pcfInstanceSyncResponse returned")
                                      .build();
        pcfCommandExecutionResponse.setPcfCommandResponse(pcfInstanceSyncResponse);
      } else {
        int instanceSize = Collections.size(pcfInstanceSyncResponse.getInstanceIndices());
        log.info("Found {} number of instances pcf deployment", instanceSize);
      }
      execute(delegateAgentManagerClient.publishInstanceSyncResult(
          taskId.getId(), pcfConfig.getAccountId(), pcfCommandExecutionResponse));
    } catch (Exception ex) {
      log.error(
          "Failed to publish the instance collection result to manager for application name {} and PerpetualTaskId {}",
          applicationName, taskId.getId(), ex);
    }
    CommandExecutionStatus commandExecutionStatus = pcfCommandExecutionResponse.getCommandExecutionStatus();
    log.info("Published instanceSync successfully for perp task: {}, state: {}", taskId, commandExecutionStatus);
    return getPerpetualTaskResponse(pcfCommandExecutionResponse, commandExecutionStatus);
  }

  private PerpetualTaskResponse getPerpetualTaskResponse(
      PcfCommandExecutionResponse pcfCommandExecutionResponse, CommandExecutionStatus commandExecutionStatus) {
    String message = "success";
    if (CommandExecutionStatus.FAILURE == commandExecutionStatus) {
      message = pcfCommandExecutionResponse.getErrorMessage();
    }

    return PerpetualTaskResponse.builder().responseCode(Response.SC_OK).responseMessage(message).build();
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}
