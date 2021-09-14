/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm.states;

import software.wings.sm.StateType;

public class AzureNodeSelectState extends NodeSelectState {
  public AzureNodeSelectState(String name) {
    super(name, StateType.AZURE_NODE_SELECT.name());
  }
}
