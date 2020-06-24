package software.wings.delegatetasks.spotinst.taskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static io.harness.spotinst.model.SpotInstConstants.DELETE_NEW_ELASTI_GROUP;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.UP_SCALE_COMMAND_UNIT;
import static io.harness.spotinst.model.SpotInstConstants.UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstTaskParameters;
import io.harness.delegate.task.spotinst.response.SpotInstDeployTaskResponse;
import io.harness.delegate.task.spotinst.response.SpotInstTaskExecutionResponse;
import io.harness.spotinst.model.ElastiGroup;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AwsConfig;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.command.ExecutionLogCallback;

import java.util.List;

@Singleton
@NoArgsConstructor
@Slf4j
public class SpotInstDeployTaskHandler extends SpotInstTaskHandler {
  @VisibleForTesting
  void scaleElastigroup(ElastiGroup elastiGroup, String spotInstToken, String spotInstAccountId, int steadyStateTimeOut,
      SpotInstDeployTaskParameters deployTaskParameters, String scaleCommandUnit, String waitCommandUnit)
      throws Exception {
    if (elastiGroup != null) {
      updateElastiGroupAndWait(spotInstToken, spotInstAccountId, elastiGroup, steadyStateTimeOut, deployTaskParameters,
          scaleCommandUnit, waitCommandUnit);
    } else {
      createAndFinishEmptyExecutionLog(deployTaskParameters, scaleCommandUnit, "No Elastigroup eligible for scaling");
      createAndFinishEmptyExecutionLog(deployTaskParameters, waitCommandUnit, "No Elastigroup eligible for scaling");
    }
  }

  @Override
  protected SpotInstTaskExecutionResponse executeTaskInternal(SpotInstTaskParameters spotInstTaskParameters,
      SpotInstConfig spotInstConfig, AwsConfig awsConfig) throws Exception {
    if (!(spotInstTaskParameters instanceof SpotInstDeployTaskParameters)) {
      String message =
          format("Parameters of unrecognized class: [%s] found while executing setup step. Workflow execution: [%s]",
              spotInstTaskParameters.getClass().getSimpleName(), spotInstTaskParameters.getWorkflowExecutionId());
      logger.error(message);
      return SpotInstTaskExecutionResponse.builder().commandExecutionStatus(FAILURE).errorMessage(message).build();
    }

    String spotInstAccountId = spotInstConfig.getSpotInstAccountId();
    String spotInstToken = String.valueOf(spotInstConfig.getSpotInstToken());
    SpotInstDeployTaskParameters deployTaskParameters = (SpotInstDeployTaskParameters) spotInstTaskParameters;
    ElastiGroup newElastiGroup = deployTaskParameters.getNewElastiGroupWithUpdatedCapacity();
    ElastiGroup oldElastiGroup = deployTaskParameters.getOldElastiGroupWithUpdatedCapacity();
    boolean resizeNewFirst = deployTaskParameters.isResizeNewFirst();
    int steadyStateTimeOut = getTimeOut(deployTaskParameters.getTimeoutIntervalInMin());

    if (deployTaskParameters.isBlueGreen()) {
      // B/G
      scaleElastigroup(newElastiGroup, spotInstToken, spotInstAccountId, steadyStateTimeOut, deployTaskParameters,
          UP_SCALE_COMMAND_UNIT, UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
    } else {
      // Canary OR Basic
      if (deployTaskParameters.isRollback()) {
        // Roll back, always resize the old one first
        scaleElastigroup(oldElastiGroup, spotInstToken, spotInstAccountId, steadyStateTimeOut, deployTaskParameters,
            UP_SCALE_COMMAND_UNIT, UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
        scaleElastigroup(newElastiGroup, spotInstToken, spotInstAccountId, steadyStateTimeOut, deployTaskParameters,
            DOWN_SCALE_COMMAND_UNIT, DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
      } else {
        // Deploy
        if (resizeNewFirst) {
          scaleElastigroup(newElastiGroup, spotInstToken, spotInstAccountId, steadyStateTimeOut, deployTaskParameters,
              UP_SCALE_COMMAND_UNIT, UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
          scaleElastigroup(oldElastiGroup, spotInstToken, spotInstAccountId, steadyStateTimeOut, deployTaskParameters,
              DOWN_SCALE_COMMAND_UNIT, DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
        } else {
          scaleElastigroup(oldElastiGroup, spotInstToken, spotInstAccountId, steadyStateTimeOut, deployTaskParameters,
              DOWN_SCALE_COMMAND_UNIT, DOWN_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
          scaleElastigroup(newElastiGroup, spotInstToken, spotInstAccountId, steadyStateTimeOut, deployTaskParameters,
              UP_SCALE_COMMAND_UNIT, UP_SCALE_STEADY_STATE_WAIT_COMMAND_UNIT);
        }
      }
    }

    if (deployTaskParameters.isRollback() && newElastiGroup != null) {
      ExecutionLogCallback logCallback = getLogCallBack(deployTaskParameters, DELETE_NEW_ELASTI_GROUP);
      logCallback.saveExecutionLog(format(
          "Sending request to Spotinst to delete newly created Elastigroup with id: [%s]", newElastiGroup.getId()));
      spotInstHelperServiceDelegate.deleteElastiGroup(spotInstToken, spotInstAccountId, newElastiGroup.getId());
      logCallback.saveExecutionLog(
          format("Elastigroup: [%s] deleted successfully", newElastiGroup.getId()), INFO, SUCCESS);

      // Set it to null to that we do not look for instances
      newElastiGroup = null;
    }

    List<Instance> newElastiGroupInstances = newElastiGroup != null
        ? getAllEc2InstancesOfElastiGroup(
              awsConfig, deployTaskParameters.getAwsRegion(), spotInstToken, spotInstAccountId, newElastiGroup.getId())
        : emptyList();

    List<Instance> ec2InstancesForOlderElastiGroup = oldElastiGroup != null
        ? getAllEc2InstancesOfElastiGroup(
              awsConfig, deployTaskParameters.getAwsRegion(), spotInstToken, spotInstAccountId, oldElastiGroup.getId())
        : emptyList();

    return SpotInstTaskExecutionResponse.builder()
        .commandExecutionStatus(SUCCESS)
        .spotInstTaskResponse(SpotInstDeployTaskResponse.builder()
                                  .ec2InstancesAdded(newElastiGroupInstances)
                                  .ec2InstancesExisting(ec2InstancesForOlderElastiGroup)
                                  .build())
        .build();
  }
}