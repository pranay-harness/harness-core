package io.harness.pms.sdk.core.data;

import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OptionalSweepingOutput {
  boolean found;
  String output;

  public SweepingOutput convertOutputToSweepingOutput() {
    return RecastOrchestrationUtils.fromDocumentJson(output, SweepingOutput.class);
  }
}
