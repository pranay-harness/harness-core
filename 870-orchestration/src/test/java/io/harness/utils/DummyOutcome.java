package io.harness.utils;

import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("Dummy3")
public class DummyOutcome implements Outcome {
  String test;

  @Override
  public String getType() {
    return "Dummy3";
  }
}
