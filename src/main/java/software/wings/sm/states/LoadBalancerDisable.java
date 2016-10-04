package software.wings.sm.states;

import software.wings.sm.StateType;

/**
 * Created by peeyushaggarwal on 10/3/16.
 */
public class LoadBalancerDisable extends LoadBalancerState {
  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public LoadBalancerDisable(String name) {
    super(name, StateType.LOAD_BALANCER_DISABLE.getName());
    setEnable(false);
  }
}
