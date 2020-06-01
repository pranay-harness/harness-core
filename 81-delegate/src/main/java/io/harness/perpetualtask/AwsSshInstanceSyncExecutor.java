package io.harness.perpetualtask;

import static io.harness.network.SafeHttpCall.execute;

import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import io.harness.beans.ExecutionStatus;
import io.harness.grpc.utils.AnyUtils;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.AwsSshInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsEc2ListInstancesResponse;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;

import java.time.Instant;
import java.util.List;

@Slf4j
public class AwsSshInstanceSyncExecutor implements PerpetualTaskExecutor {
  @Inject private AwsEc2HelperServiceDelegate ec2ServiceDelegate;
  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    final AwsSshInstanceSyncPerpetualTaskParams instanceSyncParams =
        AnyUtils.unpack(params.getCustomizedParams(), AwsSshInstanceSyncPerpetualTaskParams.class);

    final String region = instanceSyncParams.getRegion();

    final AwsConfig awsConfig = (AwsConfig) KryoUtils.asObject(instanceSyncParams.getAwsConfig().toByteArray());
    final List<EncryptedDataDetail> encryptedDataDetails =
        cast(KryoUtils.asObject(instanceSyncParams.getEncryptedData().toByteArray()));
    final List<Filter> filters = cast(KryoUtils.asObject(instanceSyncParams.getFilter().toByteArray()));

    final AwsEc2ListInstancesResponse awsResponse = getInstances(region, awsConfig, encryptedDataDetails, filters);
    try {
      execute(
          delegateAgentManagerClient.publishInstanceSyncResult(taskId.getId(), awsConfig.getAccountId(), awsResponse));
    } catch (Exception e) {
      logger.error(
          String.format("Failed to publish the instance collection result to manager for aws ssh for taskId [%s]",
              taskId.getId()),
          e);
    }

    return getPerpetualTaskResponse(awsResponse);
  }

  private AwsEc2ListInstancesResponse getInstances(
      String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, List<Filter> filters) {
    try {
      final List<Instance> instances =
          ec2ServiceDelegate.listEc2Instances(awsConfig, encryptedDataDetails, region, filters);
      return AwsEc2ListInstancesResponse.builder()
          .instances(instances)
          .executionStatus(ExecutionStatus.SUCCESS)
          .build();
    } catch (Exception e) {
      logger.error("Error occured while fetching instances from AWS", e);
      return AwsEc2ListInstancesResponse.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(e.getMessage())
          .build();
    }
  }

  private PerpetualTaskResponse getPerpetualTaskResponse(AwsEc2ListInstancesResponse awsResponse) {
    PerpetualTaskState taskState;
    String message;
    if (ExecutionStatus.FAILED == awsResponse.getExecutionStatus()) {
      taskState = PerpetualTaskState.TASK_RUN_FAILED;
      message = awsResponse.getErrorMessage();
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

  @SuppressWarnings("unchecked")
  private static <T extends List<?>> T cast(Object obj) {
    return (T) obj;
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }
}
