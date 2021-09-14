/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.adviser;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviserResponse;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface Adviser {
  @NotNull AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent);

  boolean canAdvise(AdvisingEvent advisingEvent);
}
