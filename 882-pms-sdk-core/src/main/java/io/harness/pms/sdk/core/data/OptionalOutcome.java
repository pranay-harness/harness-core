/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.data;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OptionalOutcome {
  boolean found;
  Outcome outcome;
}
