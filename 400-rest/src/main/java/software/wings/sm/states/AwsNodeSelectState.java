package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.sm.StateType;

/**
 * Created by brett on 10/10/17
 */
@OwnedBy(CDP)
public class AwsNodeSelectState extends NodeSelectState {
  public AwsNodeSelectState(String name) {
    super(name, StateType.AWS_NODE_SELECT.name());
  }
}
