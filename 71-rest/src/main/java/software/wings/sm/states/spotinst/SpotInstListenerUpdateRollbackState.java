package software.wings.sm.states.spotinst;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotInstListenerUpdateRollbackState extends SpotInstListenerUpdateState {
  @Inject private transient SpotInstStateHelper spotInstStateHelper;

  @Override
  protected SpotInstSwapRoutesTaskParameters getTaskParameters(ExecutionContext context, Application app,
      String activityId, AwsAmiInfrastructureMapping awsAmiInfrastructureMapping,
      SpotInstSetupContextElement setupContextElement) {
    SpotInstSwapRoutesTaskParameters taskParameters =
        super.getTaskParameters(context, app, activityId, awsAmiInfrastructureMapping, setupContextElement);
    taskParameters.setNewElastiGroup(spotInstStateHelper.prepareNewElastiGroupConfigForRollback(setupContextElement));
    taskParameters.setOldElastiGroup(spotInstStateHelper.prepareOldElastiGroupConfigForRollback(setupContextElement));
    return taskParameters;
  }

  public SpotInstListenerUpdateRollbackState(String name) {
    super(name, StateType.SPOTINST_LISTENER_UPDATE_ROLLBACK.name());
  }

  @Override
  @SchemaIgnore
  public boolean isRollback() {
    return true;
  }

  @Override
  @SchemaIgnore
  public boolean isDownsizeOldElastiGroup() {
    return super.isDownsizeOldElastiGroup();
  }
}
