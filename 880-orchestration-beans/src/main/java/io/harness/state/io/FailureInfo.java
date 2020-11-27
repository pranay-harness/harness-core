package io.harness.state.io;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.FailureType;

import java.util.EnumSet;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("FailureInfo")
public class FailureInfo {
  String errorMessage;
  @Builder.Default EnumSet<FailureType> failureTypes = EnumSet.noneOf(FailureType.class);
}
