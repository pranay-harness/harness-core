package software.wings.delegatetasks.spotinst.taskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.spotinst.model.SpotInstConstants.PROD_ELASTI_GROUP_NAME_SUFFIX;
import static io.harness.spotinst.model.SpotInstConstants.STAGE_ELASTI_GROUP_NAME_SUFFIX;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.google.inject.Singleton;

import com.amazonaws.services.elasticloadbalancingv2.model.Action;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersResult;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.spotinst.model.ElastiGroup;
import io.harness.spotinst.model.ElastiGroupCapacity;
import io.harness.spotinst.model.ElastiGroupRenameRequest;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.Optional;

@Slf4j
@Singleton
@NoArgsConstructor
public class SpotInstSwapRoutesTaskHandler extends SpotInstTaskHandler {
  protected SpotInstTaskExecutionResponse executeTaskInternal(SpotInstTaskParameters spotInstTaskParameters,
      ExecutionLogCallback logCallback, SpotInstConfig spotInstConfig, AwsConfig awsConfig) throws Exception {
    if (!(spotInstTaskParameters instanceof SpotInstSwapRoutesTaskParameters)) {
      String message =
          format("Parameters of unrecognized class: [%s] found while executing setup step. Workflow execution: [%s]",
              spotInstTaskParameters.getClass().getSimpleName(), spotInstTaskParameters.getWorkflowExecutionId());
      logger.error(message);
      return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }
    String spotInstAccountId = spotInstConfig.getSpotInstAccountId();
    String spotInstToken = String.valueOf(spotInstConfig.getSpotInstToken());
    SpotInstSwapRoutesTaskParameters swapRoutesParameters = (SpotInstSwapRoutesTaskParameters) spotInstTaskParameters;
    if (swapRoutesParameters.isRollback()) {
      return executeRollback(spotInstAccountId, spotInstToken, awsConfig, swapRoutesParameters,
          swapRoutesParameters.getWorkflowExecutionId(), logCallback);
    } else {
      return executeDeploy(spotInstAccountId, spotInstToken, awsConfig, swapRoutesParameters,
          swapRoutesParameters.getWorkflowExecutionId(), logCallback);
    }
  }

  private SpotInstTaskExecutionResponse executeDeploy(String spotInstAccountId, String spotInstToken,
      AwsConfig awsConfig, SpotInstSwapRoutesTaskParameters swapRoutesParameters, String workflowExecutionId,
      ExecutionLogCallback logCallback) throws Exception {
    String prodElastiGroupName =
        format("%s__%s", swapRoutesParameters.getElastiGroupNamePrefix(), PROD_ELASTI_GROUP_NAME_SUFFIX);
    String stageElastiGroupName =
        format("%s__%s", swapRoutesParameters.getElastiGroupNamePrefix(), STAGE_ELASTI_GROUP_NAME_SUFFIX);
    ElastiGroup newElastiGroup = swapRoutesParameters.getNewElastiGroup();
    String newElastiGroupId = (newElastiGroup != null) ? newElastiGroup.getId() : EMPTY;
    ElastiGroup oldElastiGroup = swapRoutesParameters.getOldElastiGroup();
    String oldElastiGroupId = (oldElastiGroup != null) ? oldElastiGroup.getId() : EMPTY;

    if (isNotEmpty(newElastiGroupId)) {
      logCallback.saveExecutionLog(format(
          "Sending request to rename Elasti Group with Id: [%s] to [%s]", newElastiGroupId, prodElastiGroupName));
      spotInstHelperServiceDelegate.updateElastiGroup(spotInstToken, spotInstAccountId, newElastiGroupId,
          ElastiGroupRenameRequest.builder().name(prodElastiGroupName).build());
    }
    if (isNotEmpty(oldElastiGroupId)) {
      logCallback.saveExecutionLog(
          format("Sending request to rename Elasti Group with Id: [%s] to [%s]", oldElastiGroup, stageElastiGroupName));
      spotInstHelperServiceDelegate.updateElastiGroup(spotInstToken, spotInstAccountId, oldElastiGroupId,
          ElastiGroupRenameRequest.builder().name(stageElastiGroupName).build());
    }
    awsElbHelperServiceDelegate.updateListenersForEcsBG(awsConfig, emptyList(),
        swapRoutesParameters.getProdListenerArn(), swapRoutesParameters.getStageListenerArn(),
        swapRoutesParameters.getAwsRegion());
    if (swapRoutesParameters.isDownsizeOldElastiGroup() && isNotEmpty(oldElastiGroupId)) {
      ElastiGroup temp = ElastiGroup.builder()
                             .id(oldElastiGroupId)
                             .name(stageElastiGroupName)
                             .capacity(ElastiGroupCapacity.builder().minimum(0).maximum(0).target(0).build())
                             .build();
      int steadyStateTimeOut = getTimeOut(swapRoutesParameters.getSteadyStateTimeOut());
      updateElastiGroupAndWait(
          spotInstToken, spotInstAccountId, temp, logCallback, workflowExecutionId, steadyStateTimeOut);
    }
    return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(SUCCESS).build();
  }

  private SpotInstTaskExecutionResponse executeRollback(String spotInstAccountId, String spotInstToken,
      AwsConfig awsConfig, SpotInstSwapRoutesTaskParameters swapRoutesParameters, String workflowExecutionId,
      ExecutionLogCallback logCallback) throws Exception {
    String prodElastiGroupName =
        format("%s__%s", swapRoutesParameters.getElastiGroupNamePrefix(), PROD_ELASTI_GROUP_NAME_SUFFIX);
    String stageElastiGroupName =
        format("%s__%s", swapRoutesParameters.getElastiGroupNamePrefix(), STAGE_ELASTI_GROUP_NAME_SUFFIX);
    ElastiGroup newElastiGroup = swapRoutesParameters.getNewElastiGroup();
    String newElastiGroupId = (newElastiGroup != null) ? newElastiGroup.getId() : EMPTY;
    ElastiGroup oldElastiGroup = swapRoutesParameters.getOldElastiGroup();
    String oldElastiGroupId = (oldElastiGroup != null) ? oldElastiGroup.getId() : EMPTY;
    int steadyStateTimeOut = getTimeOut(swapRoutesParameters.getSteadyStateTimeOut());

    if (isNotEmpty(oldElastiGroupId)) {
      ElastiGroup temp = ElastiGroup.builder()
                             .id(oldElastiGroupId)
                             .name(prodElastiGroupName)
                             .capacity(oldElastiGroup.getCapacity())
                             .build();
      logCallback.saveExecutionLog(
          format("Updating Elasti group with id: [%s] to name: [%s] and capacity: [min: %d, max: %d, target: [%d]]",
              oldElastiGroupId, prodElastiGroupName, oldElastiGroup.getCapacity().getMinimum(),
              oldElastiGroup.getCapacity().getMaximum(), oldElastiGroup.getCapacity().getTarget()));
      updateElastiGroupAndWait(
          spotInstToken, spotInstAccountId, temp, logCallback, workflowExecutionId, steadyStateTimeOut);
      logCallback.saveExecutionLog(
          format("Renaming Elasti group with id: [%s] to name: [%s]", oldElastiGroupId, prodElastiGroupName));
      spotInstHelperServiceDelegate.updateElastiGroup(spotInstToken, spotInstAccountId, oldElastiGroupId,
          ElastiGroupRenameRequest.builder().name(prodElastiGroupName).build());
    }

    DescribeListenersResult result = awsElbHelperServiceDelegate.describeListenerResult(
        awsConfig, emptyList(), swapRoutesParameters.getProdListenerArn(), swapRoutesParameters.getAwsRegion());
    Optional<Action> optionalAction =
        result.getListeners()
            .get(0)
            .getDefaultActions()
            .stream()
            .filter(action -> "forward".equalsIgnoreCase(action.getType()) && isNotEmpty(action.getTargetGroupArn()))
            .findFirst();
    if (optionalAction.isPresent()
        && optionalAction.get().getTargetGroupArn().equals(swapRoutesParameters.getTargetGroupArnForNewElastiGroup())) {
      logCallback.saveExecutionLog(format("Listener: [%s] is forwarding traffic to: [%s]. Swap routes in rollback",
          swapRoutesParameters.getProdListenerArn(), swapRoutesParameters.getTargetGroupArnForNewElastiGroup()));
      awsElbHelperServiceDelegate.updateListenersForEcsBG(awsConfig, emptyList(),
          swapRoutesParameters.getProdListenerArn(), swapRoutesParameters.getStageListenerArn(),
          swapRoutesParameters.getAwsRegion());
    }

    if (isNotEmpty(newElastiGroupId)) {
      ElastiGroup temp = ElastiGroup.builder()
                             .id(newElastiGroupId)
                             .name(stageElastiGroupName)
                             .capacity(ElastiGroupCapacity.builder().minimum(0).maximum(0).target(0).build())
                             .build();
      logCallback.saveExecutionLog(
          format("Updating Elasti group with id: [%s] to name: [%s] and capacity: [min: %d, max: %d, target: [%d]]",
              newElastiGroupId, stageElastiGroupName, 0, 0, 0));
      updateElastiGroupAndWait(
          spotInstToken, spotInstAccountId, temp, logCallback, workflowExecutionId, steadyStateTimeOut);
      logCallback.saveExecutionLog(
          format("Renaming Elasti group with id: [%s] to name: [%s]", newElastiGroupId, stageElastiGroupName));
      spotInstHelperServiceDelegate.updateElastiGroup(spotInstToken, spotInstAccountId, newElastiGroupId,
          ElastiGroupRenameRequest.builder().name(stageElastiGroupName).build());
    }

    return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(SUCCESS).build();
  }
}