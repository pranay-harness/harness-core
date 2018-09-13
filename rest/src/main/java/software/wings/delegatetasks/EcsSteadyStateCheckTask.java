package software.wings.delegatetasks;

import static com.amazonaws.util.CollectionUtils.isNullOrEmpty;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.Service;
import io.harness.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.container.EcsSteadyStateCheckParams;
import software.wings.beans.container.EcsSteadyStateCheckResponse;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.sm.ExecutionStatus;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EcsSteadyStateCheckTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(EcsSteadyStateCheckTask.class);
  @Inject private AwsHelperService awsHelperService;
  @Inject private DelegateLogService delegateLogService;
  @Inject private EcsContainerService ecsContainerService;

  public EcsSteadyStateCheckTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  private Service getService(String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String clusterName, String serviceName) {
    List<Service> services = awsHelperService
                                 .describeServices(region, awsConfig, encryptedDataDetails,
                                     new DescribeServicesRequest().withCluster(clusterName).withServices(serviceName))
                                 .getServices();
    if (isNullOrEmpty(services)) {
      throw new InvalidRequestException(
          String.format("No service in cluster: %s with name: %s", clusterName, serviceName));
    }
    return services.get(0);
  }

  @Override
  public EcsSteadyStateCheckResponse run(Object[] parameters) {
    EcsSteadyStateCheckParams params = (EcsSteadyStateCheckParams) parameters[0];
    ExecutionLogCallback executionLogCallback = new ExecutionLogCallback(
        delegateLogService, params.getAccountId(), params.getAppId(), params.getActivityId(), params.getCommandName());
    try {
      // First poll to see what is the service state right now.
      executionLogCallback.saveExecutionLog(String.format("Getting details about Ecs service: %s in Ecs cluster: %s.",
          params.getServiceName(), params.getClusterName()));
      Service service = getService(params.getRegion(), params.getAwsConfig(), params.getEncryptionDetails(),
          params.getClusterName(), params.getServiceName());

      // Now wait for runningCount == desiredCount
      executionLogCallback.saveExecutionLog(
          String.format("Waiting for running count to reach: %d for Ecs service: %s in Ecs cluster: %s.",
              service.getDesiredCount(), params.getServiceName(), params.getClusterName()));
      ecsContainerService.waitForTasksToBeInRunningStateButDontThrowException(params.getRegion(), params.getAwsConfig(),
          params.getEncryptionDetails(), params.getClusterName(), params.getServiceName(), executionLogCallback,
          service.getDesiredCount());

      // Now poll for events API to notify of steady state
      executionLogCallback.saveExecutionLog(
          String.format("Starting to wait for steady state for Ecs service: %s in Ecs cluster: %s.",
              params.getServiceName(), params.getClusterName()));
      ecsContainerService.waitForServiceToReachSteadyState(params.getRegion(), params.getAwsConfig(),
          params.getEncryptionDetails(), params.getClusterName(), params.getServiceName(),
          (int) TimeUnit.MILLISECONDS.toMinutes(params.getTimeoutInMs()), executionLogCallback);
      executionLogCallback.saveExecutionLog(String.format(
          "Completed wait for steady state for Ecs service: %s in Ecs cluster: %s. Now getting all container infos.",
          params.getServiceName(), params.getClusterName()));

      // Now get all the container infos
      List<ContainerInfo> containerInfos = ecsContainerService.getContainerInfosAfterEcsWait(params.getRegion(),
          params.getAwsConfig(), params.getEncryptionDetails(), params.getClusterName(), params.getServiceName(),
          new ArrayList<>(), executionLogCallback, false);
      executionLogCallback.saveExecutionLog("Got all the container infos", LogLevel.INFO);
      return EcsSteadyStateCheckResponse.builder()
          .executionStatus(ExecutionStatus.SUCCESS)
          .containerInfoList(containerInfos)
          .build();
    } catch (Exception ex) {
      String errorMessage = String.format(
          "Exception: %s while waiting for ECS steady state for activity: %s", ex.getMessage(), params.getActivityId());
      executionLogCallback.saveExecutionLog(errorMessage, LogLevel.ERROR);
      logger.error(errorMessage, ex);
      return EcsSteadyStateCheckResponse.builder()
          .executionStatus(ExecutionStatus.FAILED)
          .errorMessage(errorMessage)
          .build();
    }
  }
}
