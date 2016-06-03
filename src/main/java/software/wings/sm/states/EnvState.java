package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

// TODO: Auto-generated Javadoc

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
public class EnvState extends State {
  private static final long serialVersionUID = 1L;

  private String envId;
  private String workflowId;

  /**
   * Creates pause state with given name.
   *
   * @param name name of the state.
   */
  public EnvState(String name) {
    super(name, StateType.ENV_STATE.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    Misc.quietSleep(2000);
    return new ExecutionResponse();
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }
}
