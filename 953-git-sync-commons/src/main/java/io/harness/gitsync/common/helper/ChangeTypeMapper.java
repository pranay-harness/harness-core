/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.ChangeType;

import lombok.experimental.UtilityClass;

@OwnedBy(DX)
@UtilityClass
public class ChangeTypeMapper {
  public ChangeType toProto(io.harness.git.model.ChangeType changeType) {
    if (changeType == io.harness.git.model.ChangeType.ADD) {
      return ChangeType.ADD;
    }
    if (changeType == io.harness.git.model.ChangeType.DELETE) {
      return ChangeType.DELETE;
    }
    if (changeType == io.harness.git.model.ChangeType.MODIFY) {
      return ChangeType.MODIFY;
    }
    return ChangeType.UNRECOGNIZED;
  }
}
