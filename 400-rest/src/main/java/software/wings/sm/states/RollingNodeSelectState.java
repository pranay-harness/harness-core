/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
