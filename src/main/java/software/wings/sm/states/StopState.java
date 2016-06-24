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
 * The Class StopState.
 *
 * @author Rishi
 */
public class StopState extends State {
  private String serviceId;

  /**
   * Instantiates a new stop state.
   *
   * @param name the name
   */
  public StopState(String name) {
    super(name, StateType.STOP.name());
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return new ExecutionResponse();
  }

  /**
   * Gets service id.
   *
   * @return the service id
   */
  public String getServiceId() {
    return serviceId;
  }

  /**
   * Sets service id.
   *
   * @param serviceId the service id
   */
  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }
}
