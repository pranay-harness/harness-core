package io.harness.utils;

import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
@JsonTypeName("dummySweepingOutput")
public class DummySweepingOutput implements ExecutionSweepingOutput {
  String test;
  @Singular Map<String, Object> keyValuePairs;

  @Override
  public String getType() {
    return "dummySweepingOutput";
  }
}
