package io.harness.perpetualtask;

import static io.harness.network.SafeHttpCall.execute;

import com.google.inject.Inject;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.spotinst.request.SpotInstListElastigroupInstancesParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.grpc.utils.AnyUtils;
import io.harness.managerclient.ManagerClient;
import io.harness.perpetualtask.instancesync.SpotinstAmiInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
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
  @Inject private ManagerClient managerClient;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    logger.info("Running the InstanceSync perpetual task executor for task id: {}", taskId);

    final SpotinstAmiInstanceSyncPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), SpotinstAmiInstanceSyncPerpetualTaskParams.class);
    final AwsConfig awsConfig = (AwsConfig) KryoUtils.asObject(taskParams.getAwsConfig().toByteArray());
    final SpotInstConfig spotInstConfig =
        (SpotInstConfig) KryoUtils.asObject(taskParams.getSpotinstConfig().toByteArray());

    SpotInstTaskExecutionResponse instanceSyncResponse = executeSyncTask(taskParams, awsConfig, spotInstConfig);

    try {
      logger.info("Publish instance sync result to manager for elastigroup id {} and perpetual task {}",
          taskParams.getElastigroupId(), taskId.getId());
      execute(
          managerClient.publishInstanceSyncResult(taskId.getId(), spotInstConfig.getAccountId(), instanceSyncResponse));
    } catch (Exception ex) {
      logger.error(
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
        (List<EncryptedDataDetail>) KryoUtils.asObject(taskParams.getAwsEncryptedData().toByteArray());
    final List<EncryptedDataDetail> spotinstEncryptedDataDetails =
        (List<EncryptedDataDetail>) KryoUtils.asObject(taskParams.getSpotinstEncryptedData().toByteArray());

    encryptionService.decrypt(awsConfig, awsEncryptedDataDetails);
    encryptionService.decrypt(spotInstConfig, spotinstEncryptedDataDetails);

    SpotInstListElastigroupInstancesParameters params = SpotInstListElastigroupInstancesParameters.builder()
                                                            .elastigroupId(taskParams.getElastigroupId())
                                                            .awsRegion(taskParams.getRegion())
                                                            .build();

    try {
      return taskHandler.executeTask(params, spotInstConfig, awsConfig);
    } catch (Exception ex) {
      logger.error("Failed to execute instance sync task for elastigroup id {}", taskParams.getElastigroupId(), ex);
      return SpotInstTaskExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(ex.getMessage())
          .build();
    }
  }

  private PerpetualTaskResponse getPerpetualTaskResponse(SpotInstTaskExecutionResponse executionResponse) {
    PerpetualTaskState taskState;
    String message;
    if (CommandExecutionStatus.FAILURE == executionResponse.getCommandExecutionStatus()) {
      taskState = PerpetualTaskState.TASK_RUN_FAILED;
      message = executionResponse.getErrorMessage();
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
}
