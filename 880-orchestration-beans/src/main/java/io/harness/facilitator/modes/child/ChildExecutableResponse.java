package io.harness.facilitator.modes.child;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.modes.ExecutableResponse;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@TypeAlias("childExecutableResponse")
public class ChildExecutableResponse implements ExecutableResponse {
  String childNodeId;
}
