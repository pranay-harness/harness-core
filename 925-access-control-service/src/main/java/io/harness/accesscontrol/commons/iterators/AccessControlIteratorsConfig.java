/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.commons.iterators;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class AccessControlIteratorsConfig {
  IteratorConfig resourceGroupIteratorConfig;
  IteratorConfig userGroupIteratorConfig;
  IteratorConfig userIteratorConfig;
  IteratorConfig serviceAccountIteratorConfig;
  IteratorConfig supportPreferenceIteratorConfig;
  IteratorConfig scopeIteratorConfig;
}
