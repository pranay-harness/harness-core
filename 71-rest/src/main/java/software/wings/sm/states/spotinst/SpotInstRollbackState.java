package software.wings.sm.states.spotinst;

import static io.harness.spotinst.model.SpotInstConstants.ELASTI_GROUP_ALL_PHASE_ROLLBACK;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.Scope;
import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.serializer.KryoSerializer;
import io.harness.spotinst.model.ElastiGroup;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.InstanceUnitType;
import software.wings.service.impl.spotinst.SpotInstCommandRequest;
import software.wings.service.impl.spotinst.SpotInstCommandRequest.SpotInstCommandRequestBuilder;
import software.wings.service.impl.spotinst.SpotinstAllPhaseRollbackData;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotInstRollbackState extends SpotInstDeployState {
  @Inject private transient SpotInstStateHelper spotInstStateHelper;
  @Inject private transient SweepingOutputService sweepingOutputService;
  @Inject private KryoSerializer kryoSerializer;

  public SpotInstRollbackState(String name) {
    super(name, StateType.SPOTINST_ROLLBACK.name());
  }

  @Override
  @SchemaIgnore
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

  @Override
  protected boolean allPhaseRollbackDone(ExecutionContext context) {
    SweepingOutputInquiry sweepingOutputInquiry =
        context.prepareSweepingOutputInquiryBuilder().name(ELASTI_GROUP_ALL_PHASE_ROLLBACK).build();
    SweepingOutputInstance result = sweepingOutputService.find(sweepingOutputInquiry);
    if (result == null) {
      return false;
    }
    return ((SpotinstAllPhaseRollbackData) kryoSerializer.asInflatedObject(result.getOutput()))
        .isAllPhaseRollbackDone();
  }

  @Override
  protected void markAllPhaseRollbackDone(ExecutionContext context) {
    sweepingOutputService.save(context.prepareSweepingOutputBuilder(Scope.WORKFLOW)
                                   .name(ELASTI_GROUP_ALL_PHASE_ROLLBACK)
                                   .output(kryoSerializer.asDeflatedBytes(
                                       SpotinstAllPhaseRollbackData.builder().allPhaseRollbackDone(true).build()))
                                   .build());
  }

  @Override
  @SchemaIgnore
  public Integer getInstanceCount() {
    return super.getInstanceCount();
  }

  @Override
  @SchemaIgnore
  public InstanceUnitType getInstanceUnitType() {
    return super.getInstanceUnitType();
  }

  @Override
  @SchemaIgnore
  public Integer getDownsizeInstanceCount() {
    return super.getDownsizeInstanceCount();
  }

  @Override
  @SchemaIgnore
  public InstanceUnitType getDownsizeInstanceUnitType() {
    return super.getDownsizeInstanceUnitType();
  }
}