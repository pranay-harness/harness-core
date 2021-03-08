package io.harness.engine.skip;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SkipCheck {
  String skipCondition;
  Boolean evaluatedSkipCondition;
  boolean isSuccessful;
  String errorMessage;
}
