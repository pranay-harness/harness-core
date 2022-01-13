/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;

import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.PipelineStageExecution;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class SkipConditionMetadata {
  String assertion;
  boolean skipped;

  static SkipConditionMetadata fromPipelineStageExecution(
      PipelineStageExecution pipelineStageExecution, PipelineStage pipelineStage) {
    if (pipelineStageExecution == null || pipelineStage == null || isEmpty(pipelineStage.getPipelineStageElements())) {
      return null;
    }

    List<PipelineStageElement> pipelineStageElements = pipelineStage.getPipelineStageElements();
    pipelineStageElements.forEach(pse -> {
      if (pse.getDisableAssertion() != null) {
        pse.setDisableAssertion(pse.getDisableAssertion().trim());
      }
    });
    String assertion =
        pipelineStageElements.stream()
            .filter(pse -> pse.getDisableAssertion() != null && !pse.getDisableAssertion().equals("false"))
            .findFirst()
            .map(PipelineStageElement::getDisableAssertion)
            .orElse(null);
    if (assertion == null) {
      return null;
    }

    assertion = assertion.trim();
    if (assertion.equals("true")) {
      assertion = "SKIP_ALWAYS";
    }

    return SkipConditionMetadata.builder()
        .assertion(assertion)
        .skipped(pipelineStageExecution.getStatus() == ExecutionStatus.SKIPPED)
        .build();
  }
}
