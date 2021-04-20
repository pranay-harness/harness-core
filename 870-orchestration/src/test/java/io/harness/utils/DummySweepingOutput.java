package io.harness.utils;

import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("dummySweepingOutput")
public class DummySweepingOutput implements ExecutionSweepingOutput {
  String test;

  @Override
  public String getType() {
    return "dummySweepingOutput";
  }
}
