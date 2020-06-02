package io.harness.state.io;

import io.harness.exception.FailureType;
import lombok.Builder;
import lombok.Value;

import java.util.EnumSet;

@Value
@Builder
public class FailureInfo {
  String errorMessage;
  @Builder.Default EnumSet<FailureType> failureTypes = EnumSet.noneOf(FailureType.class);
}
