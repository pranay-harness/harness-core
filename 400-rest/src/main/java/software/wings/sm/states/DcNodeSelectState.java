/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
