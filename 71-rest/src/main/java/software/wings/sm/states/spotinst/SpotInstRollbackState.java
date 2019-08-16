package software.wings.sm.states.spotinst;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.spotinst.model.ElastiGroup;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.impl.spotinst.SpotInstCommandRequest.SpotInstCommandRequestBuilder;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotInstRollbackState extends SpotInstDeployState {
  @Inject private transient SpotInstStateHelper spotInstStateHelper;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public SpotInstRollbackState(String name) {
    super(name, StateType.SPOTINST_ROLLBACK.name());
  }

  public boolean isRollback() {
    return true;
  }

  @Override
  protected SpotInstDeployStateExecutionData generateStateExecutionData(
      SpotInstSetupContextElement spotInstSetupContextElement, Activity activity,
      AwsAmiInfrastructureMapping awsAmiInfrastructureMapping, ExecutionContext context, Application app) {
    // Generate CommandRequest to be sent to delegate
    SpotInstCommandRequestBuilder requestBuilder =
        spotInstStateHelper.generateSpotInstCommandRequest(awsAmiInfrastructureMapping, context);
    SpotInstDeployTaskParameters spotInstTaskParameters = generateRollbackTaskParameters(
        context, app, activity.getUuid(), awsAmiInfrastructureMapping, spotInstSetupContextElement);

    SpotInstCommandRequest request = requestBuilder.spotInstTaskParameters(spotInstTaskParameters).build();

    SpotInstDeployStateExecutionData stateExecutionData =
        geDeployStateExecutionData(spotInstSetupContextElement, activity,
            spotInstTaskParameters.getNewElastiGroupWithUpdatedCapacity() != null
                ? spotInstTaskParameters.getNewElastiGroupWithUpdatedCapacity().getCapacity().getTarget()
                : 0,
            spotInstTaskParameters.getOldElastiGroupWithUpdatedCapacity() != null
                ? spotInstTaskParameters.getOldElastiGroupWithUpdatedCapacity().getCapacity().getTarget()
                : 0);

    stateExecutionData.setSpotinstCommandRequest(request);
    return stateExecutionData;
  }

  private SpotInstDeployTaskParameters generateRollbackTaskParameters(ExecutionContext context, Application app,
      String activityId, AwsAmiInfrastructureMapping awsAmiInfrastructureMapping,
      SpotInstSetupContextElement setupContextElement) {
    ElastiGroup oldElastiGroup = spotInstStateHelper.prepareOldElastiGroupConfigForRollback(setupContextElement);
    ElastiGroup newElastiGroup = spotInstStateHelper.prepareNewElastiGroupConfigForRollback(setupContextElement);

    return generateSpotInstDeployTaskParameters(
        app, activityId, awsAmiInfrastructureMapping, context, setupContextElement, oldElastiGroup, newElastiGroup);
  }
}
