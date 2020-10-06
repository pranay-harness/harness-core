package io.harness.rule;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TestMetadata {
  String className;
  String methodName;
  String developer;
}
