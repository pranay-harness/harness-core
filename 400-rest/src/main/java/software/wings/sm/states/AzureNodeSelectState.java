package software.wings.sm.states;

import software.wings.sm.StateType;

public class AzureNodeSelectState extends NodeSelectState {
  public AzureNodeSelectState(String name) {
    super(name, StateType.AZURE_NODE_SELECT.name());
  }
}
