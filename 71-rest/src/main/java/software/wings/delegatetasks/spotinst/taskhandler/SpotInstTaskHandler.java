package software.wings.delegatetasks.spotinst.taskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.eraro.ErrorCode.INIT_TIMEOUT;
import static io.harness.spotinst.model.SpotInstConstants.defaultSteadyStateTimeout;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.MINUTES;
import static software.wings.beans.Log.LogLevel.ERROR;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.spotinst.SpotInstHelperServiceDelegate;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupInstanceHealth;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.intfc.aws.delegate.AwsEc2HelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsElbHelperServiceDelegate;

import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class SpotInstTaskHandler {
  @Inject protected DelegateLogService delegateLogService;
  @Inject protected SpotInstHelperServiceDelegate spotInstHelperServiceDelegate;
  @Inject protected AwsElbHelperServiceDelegate awsElbHelperServiceDelegate;
  @Inject protected TimeLimiter timeLimiter;
  @Inject protected AwsEc2HelperServiceDelegate awsEc2HelperServiceDelegate;

  public SpotInstTaskExecutionResponse executeTask(
      SpotInstTaskParameters spotInstTaskParameters, SpotInstConfig spotInstConfig, AwsConfig awsConfig) {
    ExecutionLogCallback logCallback = new ExecutionLogCallback(delegateLogService,
        spotInstTaskParameters.getAccountId(), spotInstTaskParameters.getAppId(),
        spotInstTaskParameters.getActivityId(), spotInstTaskParameters.getCommandName());
    try {
      return executeTaskInternal(spotInstTaskParameters, logCallback, spotInstConfig, awsConfig);
    } catch (Exception ex) {
      String message = ex.getMessage();
      logCallback.saveExecutionLog(message);
      logger.error(format("Exception: [%s] while processing spotinst task: [%s]. Workflow execution id: [%s]", message,
                       spotInstTaskParameters.getCommandType().name(), spotInstTaskParameters.getWorkflowExecutionId()),
          ex);
      return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }
  }

  protected void updateElastiGroupAndWait(String spotInstToken, String spotInstAccountId, ElastiGroup elastiGroup,
      ExecutionLogCallback logCallback, String workflowExecutionId, int steadyStateTimeOut) throws Exception {
    Optional<ElastiGroup> elastiGroupIntialOptional =
        spotInstHelperServiceDelegate.getElastiGroupById(spotInstToken, spotInstAccountId, elastiGroup.getId());
    if (!elastiGroupIntialOptional.isPresent()) {
      String message = format("Did not find elasti group with id: [%s]. Workflow execution: [%s]", elastiGroup.getId(),
          workflowExecutionId);
      logger.error(message);
      logCallback.saveExecutionLog(message);
      throw new InvalidRequestException(message);
    }
    ElastiGroup elastiGroupIntial = elastiGroupIntialOptional.get();
    logCallback.saveExecutionLog(format(
        "Current state of Elasti group: [%s], min: [%d], max: [%d], desired: [%d], Id: [%s]", elastiGroupIntial.getId(),
        elastiGroupIntial.getCapacity().getMinimum(), elastiGroupIntial.getCapacity().getMaximum(),
        elastiGroupIntial.getCapacity().getTarget(), elastiGroupIntial.getId()));
    int minInstances = elastiGroup.getCapacity().getMinimum();
    int maxInstances = elastiGroup.getCapacity().getMaximum();
    int targetInstances = elastiGroup.getCapacity().getTarget();
    String elastiGroupId = elastiGroup.getId();
    logCallback.saveExecutionLog(
        format("Sending request to Spot Inst to update elasti group: [%s] with min: [%d], max: [%d] and target: [%d]",
            elastiGroupId, minInstances, maxInstances, targetInstances));
    spotInstHelperServiceDelegate.updateElastiGroup(spotInstToken, spotInstAccountId, elastiGroupId, elastiGroup);
    logCallback.saveExecutionLog(format("Waiting for elasti group: [%s] to reach steady state", elastiGroupId));
    try {
      timeLimiter.callWithTimeout(() -> {
        while (true) {
          List<ElastiGroupInstanceHealth> instanceHealths =
              spotInstHelperServiceDelegate.listElastiGroupInstancesHealth(
                  spotInstToken, spotInstAccountId, elastiGroupId);
          int currentTotalCount = isEmpty(instanceHealths) ? 0 : instanceHealths.size();
          int currentHealthyCount = isEmpty(instanceHealths)
              ? 0
              : (int) instanceHealths.stream().filter(health -> "HEALTHY".equals(health.getHealthStatus())).count();
          if (targetInstances == 0) {
            if (currentTotalCount == 0) {
              logCallback.saveExecutionLog(format("Elasti group: [%s] does not have any instances.", elastiGroupId));
              return true;
            } else {
              logCallback.saveExecutionLog(format("Elasti group: [%s] still has [%d] total and [%d] healthy instances",
                  elastiGroupId, currentTotalCount, currentHealthyCount));
            }
          } else {
            logCallback.saveExecutionLog(
                format("Desired instances: [%d], Total instances: [%d], Healthy instances: [%d] for Elasti Group: [%s]",
                    targetInstances, currentTotalCount, currentHealthyCount, elastiGroupId));
            if (targetInstances == currentHealthyCount && targetInstances == currentTotalCount) {
              logCallback.saveExecutionLog(format("Elasti group: [%s] reached steady state", elastiGroupId));
              return true;
            }
          }
          sleep(ofSeconds(10));
        }
      }, steadyStateTimeOut, MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      String errorMessage =
          format("Timed out while waiting for steady state for elasti group: [%s]. Workflow execution: [%s]",
              elastiGroupId, workflowExecutionId);
      logCallback.saveExecutionLog(errorMessage, ERROR);
      throw new WingsException(INIT_TIMEOUT).addParam("message", errorMessage);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(
          format("Timed out while waiting for steady state for elasti group: [%s]. Workflow execution: [%s]",
              elastiGroupId, workflowExecutionId),
          e);
    }
  }

  protected int getTimeOut(int timeOut) {
    return (timeOut > 0) ? timeOut : defaultSteadyStateTimeout;
  }

  protected abstract SpotInstTaskExecutionResponse executeTaskInternal(SpotInstTaskParameters spotInstTaskParameters,
      ExecutionLogCallback logCallback, SpotInstConfig spotInstConfig, AwsConfig awsConfig) throws Exception;
}