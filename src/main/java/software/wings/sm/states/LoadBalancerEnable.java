package software.wings.sm.states;

import software.wings.sm.StateType;

/**
 * Created by peeyushaggarwal on 10/3/16.
 */
public class LoadBalancerEnable extends LoadBalancerState {
  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public LoadBalancerEnable(String name) {
    super(name);
    setEnable(true);
    setStateType(StateType.LOAD_BALANCER_ENABLE.name());
  }
}
