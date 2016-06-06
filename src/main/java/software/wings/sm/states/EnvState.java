package software.wings.sm.states;

import static software.wings.beans.CatalogNames.ENVIRONMENTS;

import com.github.reinert.jjschema.Attributes;
import software.wings.sm.EnumData;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

// TODO: Auto-generated Javadoc

/**
 * A Env state to pause state machine execution.
 *
 * @author Rishi
 */
@Attributes(title = "Env")
public class EnvState extends State {
  private static final long serialVersionUID = 1L;

  @EnumData(catalog = ENVIRONMENTS) @Attributes(required = true) private String envId;

  @Attributes(required = true) private String workflowId;

  /**
   * Creates env state with given name.
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
