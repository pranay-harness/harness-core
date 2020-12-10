package io.harness.utils;

import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@JsonTypeName("Dummy1")
public class DummyOutcome implements Outcome {
  String test;

  @Override
  public String getType() {
    return "Dummy1";
  }
}
