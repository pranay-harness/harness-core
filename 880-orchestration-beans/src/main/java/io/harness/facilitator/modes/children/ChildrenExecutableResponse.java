package io.harness.facilitator.modes.children;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.modes.ExecutableResponse;
import io.harness.state.io.StepTransput;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
@TypeAlias("childrenExecutableResponse")
public class ChildrenExecutableResponse implements ExecutableResponse {
  @Singular List<Child> children;

  @Value
  @Builder
  public static class Child {
    String childNodeId;
    @Singular List<StepTransput> additionalInputs;
  }
}
