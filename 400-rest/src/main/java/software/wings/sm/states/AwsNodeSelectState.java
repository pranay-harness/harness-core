/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
