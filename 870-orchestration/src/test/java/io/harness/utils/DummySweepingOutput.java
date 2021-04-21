package io.harness.utils;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

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
