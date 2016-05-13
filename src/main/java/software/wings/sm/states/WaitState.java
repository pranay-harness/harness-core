package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * dummy implementation for wait state.
 *
 * @author Rishi
 */
public class WaitState extends State {
  private static final long serialVersionUID = 1L;

  private long duration;

  /**
   * Creates a new wait state.
   *
   * @param name state name
   */
  public WaitState(String name) {
    super(name, StateType.WAIT.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return new ExecutionResponse();
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }
}
