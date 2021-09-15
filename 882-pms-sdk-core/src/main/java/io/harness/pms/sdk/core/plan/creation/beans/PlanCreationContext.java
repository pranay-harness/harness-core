/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core.plan.creation.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.async.AsyncCreatorContext;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.contracts.plan.PlanCreationContextValue;
import io.harness.pms.yaml.YamlField;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;

@OwnedBy(HarnessTeam.PIPELINE)
@Data
@Builder
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlanCreationContext implements AsyncCreatorContext {
  YamlField currentField;
  @Singular("globalContext") Map<String, PlanCreationContextValue> globalContext;

  public static PlanCreationContext cloneWithCurrentField(PlanCreationContext planCreationContext, YamlField field) {
    return PlanCreationContext.builder()
        .currentField(field)
        .globalContext(planCreationContext.getGlobalContext())
        .build();
  }

  public void mergeContextFromPlanCreationResponse(PlanCreationResponse planCreationResponse) {
    if (EmptyPredicate.isEmpty(getGlobalContext())) {
      this.setGlobalContext(new HashMap<>());
    }
    this.getGlobalContext().putAll(planCreationResponse.getContextMap());
  }

  public PlanCreationContextValue getMetadata() {
    return globalContext == null ? null : globalContext.get("metadata");
  }

  @Override
  public ByteString getGitSyncBranchContext() {
    return null;
  }
}
