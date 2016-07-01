package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.stencils.EnumData;
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
  @EnumData(expandIntoMultipleEntries = true, enumDataProvider = EnvironmentServiceImpl.class)
  @Attributes(required = true, title = "Environment")
  private String envId;

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

  /**
   * Gets env id.
   *
   * @return the env id
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Sets env id.
   *
   * @param envId the env id
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Gets workflow id.
   *
   * @return the workflow id
   */
  public String getWorkflowId() {
    return workflowId;
  }

  /**
   * Sets workflow id.
   *
   * @param workflowId the workflow id
   */
  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }
}
