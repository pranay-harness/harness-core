package software.wings.delegatetasks.spotinst.taskhandler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.eraro.ErrorCode.INIT_TIMEOUT;
import static io.harness.spotinst.model.SpotInstConstants.DEPLOYMENT_ERROR;
import static io.harness.spotinst.model.SpotInstConstants.defaultSteadyStateTimeout;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
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
    try {
      SpotInstTaskExecutionResponse response = executeTaskInternal(spotInstTaskParameters, spotInstConfig, awsConfig);
      if (!spotInstTaskParameters.isSyncTask()) {
        ExecutionLogCallback logCallback = getLogCallBack(spotInstTaskParameters, DEPLOYMENT_ERROR);
        logCallback.saveExecutionLog("No deployment error. Execution success", INFO, SUCCESS);
      }
      return response;
    } catch (Exception ex) {
      if (spotInstTaskParameters.isSyncTask()) {
        throw new InvalidRequestException(ex.getMessage(), ex);
      } else {
        String message = ex.getMessage();
        ExecutionLogCallback logCallback = getLogCallBack(spotInstTaskParameters, DEPLOYMENT_ERROR);
        logCallback.saveExecutionLog(message, ERROR, FAILURE);
        logger.error(
            format("Exception: [%s] while processing spotinst task: [%s]. Workflow execution id: [%s]", message,
                spotInstTaskParameters.getCommandType().name(), spotInstTaskParameters.getWorkflowExecutionId()),
            ex);
        return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
      }
    }
  }

  protected ExecutionLogCallback getLogCallBack(SpotInstTaskParameters parameters, String commandUnit) {
    return new ExecutionLogCallback(
        delegateLogService, parameters.getAccountId(), parameters.getAppId(), parameters.getActivityId(), commandUnit);
  }

  protected void updateElastiGroupAndWait(String spotInstToken, String spotInstAccountId, ElastiGroup elastiGroup,
      int steadyStateTimeOut, SpotInstTaskParameters parameters, String scaleCommandUnitName,
      String waitCommandUnitName) throws Exception {
    String workflowExecutionId = parameters.getWorkflowExecutionId();
    ExecutionLogCallback logCallback = getLogCallBack(parameters, scaleCommandUnitName);
    Optional<ElastiGroup> elastiGroupIntialOptional =
        spotInstHelperServiceDelegate.getElastiGroupById(spotInstToken, spotInstAccountId, elastiGroup.getId());
    if (!elastiGroupIntialOptional.isPresent()) {
      String message = format(
          "Did not find Elastigroup with id: [%s]. Workflow execution: [%s]", elastiGroup.getId(), workflowExecutionId);
      logger.error(message);
      logCallback.saveExecutionLog(message);
      throw new InvalidRequestException(message);
    }
    ElastiGroup elastiGroupIntial = elastiGroupIntialOptional.get();
    logCallback.saveExecutionLog(format(
        "Current state of Elastigroup: [%s], min: [%d], max: [%d], desired: [%d], Id: [%s]", elastiGroupIntial.getId(),
        elastiGroupIntial.getCapacity().getMinimum(), elastiGroupIntial.getCapacity().getMaximum(),
        elastiGroupIntial.getCapacity().getTarget(), elastiGroupIntial.getId()));
    int minInstances = elastiGroup.getCapacity().getMinimum();
    int maxInstances = elastiGroup.getCapacity().getMaximum();
    int targetInstances = elastiGroup.getCapacity().getTarget();
    String elastiGroupId = elastiGroup.getId();
    logCallback.saveExecutionLog(
        format("Sending request to Spotinst to update Elastigroup: [%s] with min: [%d], max: [%d] and target: [%d]",
            elastiGroupId, minInstances, maxInstances, targetInstances));
    spotInstHelperServiceDelegate.updateElastiGroupCapacity(
        spotInstToken, spotInstAccountId, elastiGroupId, elastiGroup);
    logCallback.saveExecutionLog("Request Sent to update Elastigroup", INFO, SUCCESS);

    ExecutionLogCallback waitLogCallback = getLogCallBack(parameters, waitCommandUnitName);
    waitLogCallback.saveExecutionLog(format("Waiting for Elastigroup: [%s] to reach steady state", elastiGroupId));
    try {
      timeLimiter.callWithTimeout(() -> {
        while (true) {
          if (allInstancesHealthy(spotInstToken, spotInstAccountId, elastiGroupId, waitLogCallback, targetInstances)) {
            return true;
          }
          sleep(ofSeconds(20));
        }
      }, steadyStateTimeOut, MINUTES, true);
    } catch (UncheckedTimeoutException e) {
      String errorMessage =
          format("Timed out while waiting for steady state for Elastigroup: [%s]. Workflow execution: [%s]",
              elastiGroupId, workflowExecutionId);
      waitLogCallback.saveExecutionLog(errorMessage, ERROR);
      throw new WingsException(INIT_TIMEOUT).addParam("message", errorMessage);
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      throw new InvalidRequestException(
          format("Timed out while waiting for steady state for Elastigroup: [%s]. Workflow execution: [%s]",
              elastiGroupId, workflowExecutionId),
          e);
    }
  }

  @VisibleForTesting
  boolean allInstancesHealthy(String spotInstToken, String spotInstAccountId, String elastiGroupId,
      ExecutionLogCallback waitLogCallback, int targetInstances) throws Exception {
    List<ElastiGroupInstanceHealth> instanceHealths =
        spotInstHelperServiceDelegate.listElastiGroupInstancesHealth(spotInstToken, spotInstAccountId, elastiGroupId);
    int currentTotalCount = isEmpty(instanceHealths) ? 0 : instanceHealths.size();
    int currentHealthyCount = isEmpty(instanceHealths)
        ? 0
        : (int) instanceHealths.stream().filter(health -> "HEALTHY".equals(health.getHealthStatus())).count();
    if (targetInstances == 0) {
      if (currentTotalCount == 0) {
        waitLogCallback.saveExecutionLog(
            format("Elastigroup: [%s] does not have any instances.", elastiGroupId), INFO, SUCCESS);
        return true;
      } else {
        waitLogCallback.saveExecutionLog(format("Elastigroup: [%s] still has [%d] total and [%d] healthy instances",
            elastiGroupId, currentTotalCount, currentHealthyCount));
      }
    } else {
      waitLogCallback.saveExecutionLog(
          format("Desired instances: [%d], Total instances: [%d], Healthy instances: [%d] for Elastigroup: [%s]",
              targetInstances, currentTotalCount, currentHealthyCount, elastiGroupId));
      if (targetInstances == currentHealthyCount && targetInstances == currentTotalCount) {
        waitLogCallback.saveExecutionLog(
            format("Elastigroup: [%s] reached steady state", elastiGroupId), INFO, SUCCESS);
        return true;
      }
    }
    return false;
  }

  @VisibleForTesting
  int getTimeOut(int timeOut) {
    return (timeOut > 0) ? timeOut : defaultSteadyStateTimeout;
  }

  @VisibleForTesting
  List<Instance> getAllEc2InstancesOfElastiGroup(AwsConfig awsConfig, String awsRegion, String spotInstToken,
      String spotInstAccountId, String elastiGroupId) throws Exception {
    List<ElastiGroupInstanceHealth> instanceHealths =
        spotInstHelperServiceDelegate.listElastiGroupInstancesHealth(spotInstToken, spotInstAccountId, elastiGroupId);
    if (isEmpty(instanceHealths)) {
      return emptyList();
    }
    List<String> instanceIds = instanceHealths.stream().map(ElastiGroupInstanceHealth::getInstanceId).collect(toList());
    return awsEc2HelperServiceDelegate.listEc2Instances(awsConfig, emptyList(), instanceIds, awsRegion);
  }

  @VisibleForTesting
  void createAndFinishEmptyExecutionLog(
      SpotInstTaskParameters taskParameters, String upScaleCommandUnit, String message) {
    ExecutionLogCallback logCallback;
    logCallback = getLogCallBack(taskParameters, upScaleCommandUnit);
    logCallback.saveExecutionLog(message, INFO, SUCCESS);
  }

  protected abstract SpotInstTaskExecutionResponse executeTaskInternal(SpotInstTaskParameters spotInstTaskParameters,
      SpotInstConfig spotInstConfig, AwsConfig awsConfig) throws Exception;
}