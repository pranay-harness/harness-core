/**
 *
 */

package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

// TODO: Auto-generated Javadoc

/**
 * The Class StartState.
 *
 * @author Rishi
 */
public class StartState extends State {
  private static final long serialVersionUID = 1L;
  private String serviceId;

  /**
   * Instantiates a new start state.
   *
   * @param name the name
   */
  public StartState(String name) {
    super(name, StateType.START.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return new ExecutionResponse();
  }

  public String getServiceId() {
    return serviceId;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }
}
