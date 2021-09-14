/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

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
