package io.harness.beans.sweepingoutputs;

import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ContextElement extends ExecutionSweepingOutput {
  String podDetails = "podDetails";
  String stageDetails = "stageDetails";
}
