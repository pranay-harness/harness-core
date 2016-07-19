package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

/**
 * A Pause state to pause state machine execution.
 *
 * @author Rishi
 */
public class ApprovalState extends State {
  private String groupName;

  /**
   * Creates pause state with given name.
   *
   * @param name name of the state.
   */
  public ApprovalState(String name) {
    super(name, StateType.APPROVAL.name());
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
   * Handle abort event.
   *
   * @param context the context
   */
  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets group name.
   *
   * @return the group name
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * Sets group name.
   *
   * @param groupName the group name
   */
  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }
}
