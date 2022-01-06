/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
