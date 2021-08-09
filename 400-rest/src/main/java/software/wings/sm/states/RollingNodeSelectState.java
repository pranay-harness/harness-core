package software.wings.sm.states;

import software.wings.sm.StateType;

import java.util.List;

public class RollingNodeSelectState extends NodeSelectState {
  public RollingNodeSelectState(String name) {
    super(name, StateType.ROLLING_NODE_SELECT.name());
  }

  @Override
  public List<String> getHostNames() {
    return null;
  }

  @Override
  public boolean isSpecificHosts() {
    return false;
  }

  @Override
  public boolean getExcludeSelectedHostsFromFuturePhases() {
    return true;
  }
}
