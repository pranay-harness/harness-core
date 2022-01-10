/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.enforcement.bases;

import io.harness.enforcement.beans.TimeUnit;
import io.harness.enforcement.constants.RestrictionType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DurationRestriction extends Restriction {
  private TimeUnit timeUnit;

  public DurationRestriction(RestrictionType restrictionType, TimeUnit timeUnit) {
    super(restrictionType);
    this.timeUnit = timeUnit;
  }
}
