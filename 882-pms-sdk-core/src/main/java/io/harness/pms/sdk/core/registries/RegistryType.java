/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.registries;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDC)
public enum RegistryType {
  STEP,
  ADVISER,
  RESOLVER,
  FACILITATOR,
  ORCHESTRATION_EVENT,
  ORCHESTRATION_FIELD,
  SDK_FUNCTOR
}
