/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.adviser.ignore;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.advisers.IgnoreFailureAdvise;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.execution.utils.StatusUtils;
import io.harness.pms.sdk.core.adviser.Adviser;
import io.harness.pms.sdk.core.adviser.AdvisingEvent;
import io.harness.pms.sdk.core.adviser.OrchestrationAdviserTypes;
import io.harness.serializer.KryoSerializer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.util.Collections;
import javax.validation.constraints.NotNull;

@OwnedBy(PIPELINE)
public class IgnoreAdviser implements Adviser {
  public static final AdviserType ADVISER_TYPE =
      AdviserType.newBuilder().setType(OrchestrationAdviserTypes.IGNORE.name()).build();

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public AdviserResponse onAdviseEvent(AdvisingEvent advisingEvent) {
    return AdviserResponse.newBuilder()
        .setIgnoreFailureAdvise(IgnoreFailureAdvise.newBuilder().build())
        .setType(AdviseType.IGNORE_FAILURE)
        .build();
  }

  @Override
  public boolean canAdvise(AdvisingEvent advisingEvent) {
    IgnoreAdviserParameters parameters = extractParameters(advisingEvent);
    boolean canAdvise = StatusUtils.brokeStatuses().contains(advisingEvent.getToStatus());
    FailureInfo failureInfo = advisingEvent.getFailureInfo();
    if (failureInfo != null && !isEmpty(failureInfo.getFailureTypesList())) {
      return canAdvise
          && !Collections.disjoint(parameters.getApplicableFailureTypes(), failureInfo.getFailureTypesList());
    }
    return canAdvise;
  }

  @NotNull
  private IgnoreAdviserParameters extractParameters(AdvisingEvent advisingEvent) {
    return (IgnoreAdviserParameters) Preconditions.checkNotNull(
        kryoSerializer.asObject(advisingEvent.getAdviserParameters()));
  }
}
