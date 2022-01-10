/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.CDC)
public class ExecutionDataValue {
  private String displayName;
  private Object value;

  /**
   * Static Factory Method
   * @param displayName
   * @param value
   * @return new ExecutionDataValue
   */
  public static ExecutionDataValue executionDataValue(String displayName, Object value) {
    return new ExecutionDataValue(displayName, value);
  }
}
