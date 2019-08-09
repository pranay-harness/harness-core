package software.wings.sm.states.spotinst;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;

import software.wings.beans.Application;
import software.wings.beans.SpotInstInfrastructureMapping;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotInstListenerUpdateRollbackState extends SpotInstListenerUpdateState {
  @Inject private transient SpotInstStateHelper spotInstStateHelper;

  @Override
  protected SpotInstSwapRoutesTaskParameters getTaskParameters(ExecutionContext context, Application app,
      String activityId, SpotInstInfrastructureMapping spotInstInfrastructureMapping,
      SpotInstSetupContextElement setupContextElement) {
    SpotInstSwapRoutesTaskParameters taskParameters =
        super.getTaskParameters(context, app, activityId, spotInstInfrastructureMapping, setupContextElement);
    taskParameters.setNewElastiGroup(spotInstStateHelper.prepareNewElastiGroupConfigForRollback(setupContextElement));
    taskParameters.setOldElastiGroup(spotInstStateHelper.prepareOldElastiGroupConfigForRollback(setupContextElement));
    return taskParameters;
  }

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public SpotInstListenerUpdateRollbackState(String name) {
    super(name, StateType.SPOTINST_LISTENER_UPDATE_ROLLBACK.name());
  }

  public boolean isRollback() {
    return true;
  }
}
