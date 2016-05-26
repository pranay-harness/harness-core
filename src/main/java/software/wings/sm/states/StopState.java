/**
 *
 */
package software.wings.sm.states;

import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;

/**
 * @author Rishi
 */
public class StopState extends State {
  private static final long serialVersionUID = 1L;
  private String serviceId;

  /**
   * @param name
   * @param stateType
   */
  public StopState(String name) {
    super(name, StateType.STOP.name());
  }

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
