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
 *
 */
public class StartState extends State {
  private String serviceId;

  /**
   * @param name
   * @param stateType
   */
  public StartState(String name) {
    super(name, StateType.START.name());
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

  private static final long serialVersionUID = 1L;
}
