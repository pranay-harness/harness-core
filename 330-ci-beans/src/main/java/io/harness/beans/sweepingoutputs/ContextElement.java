package io.harness.beans.sweepingoutputs;

import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ContextElement extends SweepingOutput {
  String podDetails = "podDetails";
}
