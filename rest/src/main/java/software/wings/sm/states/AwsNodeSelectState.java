package software.wings.sm.states;

import software.wings.sm.StateType;

/**
 * Created by brett on 10/10/17
 */
public class AwsNodeSelectState extends NodeSelectState {
  public AwsNodeSelectState(String name) {
    super(name, StateType.AWS_NODE_SELECT.name());
  }
}
