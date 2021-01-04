package io.harness.utils;

import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("dummySweepingOutput")
public class DummySweepingOutput implements SweepingOutput {
  String test;

  @Override
  public String getType() {
    return "dummySweepingOutput";
  }
}
