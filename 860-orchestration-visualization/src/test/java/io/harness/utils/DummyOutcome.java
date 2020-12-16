package io.harness.utils;

import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@AllArgsConstructor
@JsonTypeName("Dummy1")
public class DummyOutcome implements Outcome {
  String test;

  @Override
  public String getType() {
    return "Dummy1";
  }
}
