/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.adviser;

import io.harness.pms.contracts.execution.failure.FailureType;

import java.util.Set;

public interface WithFailureTypes {
  Set<FailureType> getApplicableFailureTypes();
}
