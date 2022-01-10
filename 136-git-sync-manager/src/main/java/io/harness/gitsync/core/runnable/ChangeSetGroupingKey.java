/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.core.runnable;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@FieldNameConstants(innerTypeName = "ChangeSetGroupingKeyKeys")
@OwnedBy(DX)
public class ChangeSetGroupingKey {
  String accountId;
  String queueKey;
  int count;

  @Override
  public String toString() {
    return "{"
        + "accountId='" + accountId + '\'' + ", queueKey='" + queueKey + '\'' + ", count=" + count + '}';
  }
}
