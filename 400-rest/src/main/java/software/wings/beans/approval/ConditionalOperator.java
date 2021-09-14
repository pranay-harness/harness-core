/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(CDC)
public enum ConditionalOperator {
  AND {
    @Override
    public boolean applyOperator(List<Boolean> values) {
      return !values.contains(Boolean.FALSE);
    }
  },
  OR {
    @Override
    public boolean applyOperator(List<Boolean> values) {
      return values.contains(Boolean.TRUE);
    }
  };
  public abstract boolean applyOperator(List<Boolean> values);
}
