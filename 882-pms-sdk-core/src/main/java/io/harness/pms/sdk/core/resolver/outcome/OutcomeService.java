/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.resolver.outcome;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.refobjects.RefObject;
import io.harness.pms.sdk.core.data.OptionalOutcome;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.resolver.Resolver;

import java.util.List;
import lombok.NonNull;

@OwnedBy(CDC)
public interface OutcomeService extends Resolver<Outcome> {
  List<Outcome> findAllByRuntimeId(String planExecutionId, String runtimeId);

  List<Outcome> fetchOutcomes(List<String> outcomeInstanceIds);

  Outcome fetchOutcome(@NonNull String outcomeInstanceId);

  OptionalOutcome resolveOptional(Ambiance ambiance, RefObject refObject);
}
