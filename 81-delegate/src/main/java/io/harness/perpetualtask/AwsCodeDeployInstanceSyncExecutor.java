package io.harness.perpetualtask;

import static io.harness.network.SafeHttpCall.execute;

import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import io.harness.beans.ExecutionStatus;
import io.harness.grpc.utils.AnyUtils;
import io.harness.managerclient.ManagerClient;
import io.harness.perpetualtask.instancesync.AwsCodeDeployInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Response;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsCodeDeployListDeploymentInstancesResponse;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;

import java.time.Instant;
import java.util.List;

@Slf4j
public class AwsCodeDeployInstanceSyncExecutor implements PerpetualTaskExecutor {
  @Inject private AwsEc2HelperServiceDelegate ec2ServiceDelegate;
  @Inject private ManagerClient managerClient;

  @Override
  public PerpetualTaskResponse runOnce(PerpetualTaskId taskId, PerpetualTaskParams params, Instant heartbeatTime) {
    final AwsCodeDeployInstanceSyncPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), AwsCodeDeployInstanceSyncPerpetualTaskParams.class);
    final List<Filter> filters = (List<Filter>) KryoUtils.asObject(taskParams.getFilter().toByteArray());
    final AwsConfig awsConfig = (AwsConfig) KryoUtils.asObject(taskParams.getAwsConfig().toByteArray());
    final List<EncryptedDataDetail> encryptedDataDetails =
        (List<EncryptedDataDetail>) KryoUtils.asObject(taskParams.getEncryptedData().toByteArray());

    AwsCodeDeployListDeploymentInstancesResponse instancesListResponse =
        getCodeDeployResponse(taskParams.getRegion(), filters, awsConfig, encryptedDataDetails);

    try {
      execute(managerClient.publishInstanceSyncResult(taskId.getId(), awsConfig.getAccountId(), instancesListResponse));
    } catch (Exception ex) {
      logger.error("Failed to publish instance sync result for task {}", taskId.getId(), ex);
    }

    return getPerpetualTaskResponse(instancesListResponse);
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskParams params) {
    return false;
  }

  private AwsCodeDeployListDeploymentInstancesResponse getCodeDeployResponse(
      String region, List<Filter> filters, AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      List<Instance> ec2InstancesList =
          ec2ServiceDelegate.listEc2Instances(awsConfig, encryptedDataDetails, region, filters);

      return AwsCodeDeployListDeploymentInstancesResponse.builder()
          .executionStatus(ExecutionStatus.SUCCESS)
          .instances(ec2InstancesList)
          .build();
    } catch (Exception ex) {
      logger.error("Error occurred while fetching instances list", ex);
      return AwsCodeDeployListDeploymentInstancesResponse.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(ex.getMessage())
          .build();
    }
  }

  private PerpetualTaskResponse getPerpetualTaskResponse(AwsResponse response) {
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
}
