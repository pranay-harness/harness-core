package io.harness.perpetualtask;

import static io.harness.network.SafeHttpCall.execute;

import com.google.inject.Inject;

import io.harness.delegate.task.spotinst.request.SpotInstListElastigroupInstancesParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.grpc.utils.AnyUtils;
import io.harness.logging.CommandExecutionStatus;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.SpotinstAmiInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.delegatetasks.spotinst.taskhandler.SpotInstSyncTaskHandler;
import software.wings.service.intfc.security.EncryptionService;

import java.time.Instant;
import java.util.List;

@Slf4j
public class SpotinstAmiInstanceSyncDelegateExecutor implements PerpetualTaskExecutor {
  @Inject private EncryptionService encryptionService;
  @Inject private SpotInstSyncTaskHandler taskHandler;
  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    log.info("Running the InstanceSync perpetual task executor for task id: {}", taskId);

    final SpotinstAmiInstanceSyncPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), SpotinstAmiInstanceSyncPerpetualTaskParams.class);
    final AwsConfig awsConfig = (AwsConfig) kryoSerializer.asObject(taskParams.getAwsConfig().toByteArray());
    final SpotInstConfig spotInstConfig =
        (SpotInstConfig) kryoSerializer.asObject(taskParams.getSpotinstConfig().toByteArray());

    SpotInstTaskExecutionResponse instanceSyncResponse = executeSyncTask(taskParams, awsConfig, spotInstConfig);

    try {
      log.info("Publish instance sync result to manager for elastigroup id {} and perpetual task {}",
          taskParams.getElastigroupId(), taskId.getId());
      execute(delegateAgentManagerClient.publishInstanceSyncResult(
          taskId.getId(), spotInstConfig.getAccountId(), instanceSyncResponse));
    } catch (Exception ex) {
      log.error(
          "Failed to publish the instance sync collection result to manager for elastigroup id {} and perpetual task {}",
          taskParams.getElastigroupId(), taskId.getId(), ex);
    }

    return getPerpetualTaskResponse(instanceSyncResponse);
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }

  private SpotInstTaskExecutionResponse executeSyncTask(
      SpotinstAmiInstanceSyncPerpetualTaskParams taskParams, AwsConfig awsConfig, SpotInstConfig spotInstConfig) {
    final List<EncryptedDataDetail> awsEncryptedDataDetails =
        (List<EncryptedDataDetail>) kryoSerializer.asObject(taskParams.getAwsEncryptedData().toByteArray());
    final List<EncryptedDataDetail> spotinstEncryptedDataDetails =
        (List<EncryptedDataDetail>) kryoSerializer.asObject(taskParams.getSpotinstEncryptedData().toByteArray());

    encryptionService.decrypt(awsConfig, awsEncryptedDataDetails, false);
    encryptionService.decrypt(spotInstConfig, spotinstEncryptedDataDetails, false);

    SpotInstListElastigroupInstancesParameters params = SpotInstListElastigroupInstancesParameters.builder()
                                                            .elastigroupId(taskParams.getElastigroupId())
                                                            .awsRegion(taskParams.getRegion())
                                                            .build();

    try {
      return taskHandler.executeTask(params, spotInstConfig, awsConfig);
    } catch (Exception ex) {
      log.error("Failed to execute instance sync task for elastigroup id {}", taskParams.getElastigroupId(), ex);
      return SpotInstTaskExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ex.getMessage())
          .build();
    }
  }

  private PerpetualTaskResponse getPerpetualTaskResponse(SpotInstTaskExecutionResponse executionResponse) {
    String message = "success";
    if (CommandExecutionStatus.FAILURE == executionResponse.getCommandExecutionStatus()) {
      message = executionResponse.getErrorMessage();
    }

    return PerpetualTaskResponse.builder().responseCode(Response.SC_OK).responseMessage(message).build();
  }
}
