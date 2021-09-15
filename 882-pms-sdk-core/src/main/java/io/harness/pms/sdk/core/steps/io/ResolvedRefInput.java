/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.steps.io;

import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.sdk.core.data.StepTransput;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
public class ResolvedRefInput {
  StepTransput transput;
  @NonNull RefObject refObject;
}
