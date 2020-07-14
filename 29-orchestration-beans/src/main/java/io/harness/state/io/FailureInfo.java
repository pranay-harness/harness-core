package io.harness.state.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.FailureType;
import lombok.Builder;
import lombok.Value;

import java.util.EnumSet;

@OwnedBy(CDC)
@Value
@Builder
public class FailureInfo {
  String errorMessage;
  @Builder.Default EnumSet<FailureType> failureTypes = EnumSet.noneOf(FailureType.class);
}
