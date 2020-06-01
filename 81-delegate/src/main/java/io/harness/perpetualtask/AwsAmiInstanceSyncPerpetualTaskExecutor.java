package io.harness.perpetualtask;

import static io.harness.network.SafeHttpCall.execute;

import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.beans.ExecutionStatus;
import io.harness.grpc.utils.AnyUtils;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.AwsAmiInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsAsgListInstancesResponse;
import software.wings.service.intfc.aws.delegate.AwsAsgHelperServiceDelegate;

import java.time.Instant;
import java.util.List;

@Slf4j
public class AwsAmiInstanceSyncPerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Inject private AwsAsgHelperServiceDelegate awsAsgHelperServiceDelegate;
  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    logger.info("Running the InstanceSync perpetual task executor for task id: {}", taskId);

    final AwsAmiInstanceSyncPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), AwsAmiInstanceSyncPerpetualTaskParams.class);

    final AwsConfig awsConfig = (AwsConfig) KryoUtils.asObject(taskParams.getAwsConfig().toByteArray());

    @SuppressWarnings("unchecked")
    final List<EncryptedDataDetail> encryptedDataDetails =
        (List<EncryptedDataDetail>) KryoUtils.asObject(taskParams.getEncryptedData().toByteArray());

    final AwsAsgListInstancesResponse awsResponse = getAwsResponse(taskParams, awsConfig, encryptedDataDetails);

    try {
      execute(
          delegateAgentManagerClient.publishInstanceSyncResult(taskId.getId(), awsConfig.getAccountId(), awsResponse));
    } catch (Exception e) {
      logger.error(
          String.format("Failed to publish instance sync result for aws ami. asgName [%s] and PerpetualTaskId [%s]",
              taskParams.getAsgName(), taskId.getId()),
          e);
    }
    return getPerpetualTaskResponse(awsResponse);
  }

  private AwsAsgListInstancesResponse getAwsResponse(AwsAmiInstanceSyncPerpetualTaskParams taskParams,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      final List<Instance> instances = awsAsgHelperServiceDelegate.listAutoScalingGroupInstances(
          awsConfig, encryptedDataDetails, taskParams.getRegion(), taskParams.getAsgName());
      return AwsAsgListInstancesResponse.builder()
          .executionStatus(ExecutionStatus.SUCCESS)
          .instances(instances)
          .asgName(taskParams.getAsgName())
          .build();
    } catch (Exception ex) {
      logger.error(String.format("Failed to fetch aws instances for asg: [%s]", taskParams.getAsgName()), ex);
      return AwsAsgListInstancesResponse.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .asgName(taskParams.getAsgName())
          .errorMessage(ex.getMessage())
          .build();
    }
  }

  private PerpetualTaskResponse getPerpetualTaskResponse(AwsAsgListInstancesResponse response) {
    PerpetualTaskState taskState;
    String message;
    if (ExecutionStatus.FAILED == response.getExecutionStatus()) {
      taskState = PerpetualTaskState.TASK_RUN_FAILED;
      message = response.getErrorMessage();
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
