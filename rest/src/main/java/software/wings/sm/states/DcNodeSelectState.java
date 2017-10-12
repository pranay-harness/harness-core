package software.wings.sm.states;

import software.wings.sm.StateType;

/**
 * Created by brett on 10/10/17
 */
public class DcNodeSelectState extends NodeSelectState {
  public DcNodeSelectState(String name) {
    super(name, StateType.DC_NODE_SELECT.name());
  }
}
