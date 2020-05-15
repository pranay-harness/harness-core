package io.harness.state.core.fork;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.state.io.StateParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.Size;

@OwnedBy(CDC)
@Value
@Builder
@Redesign
public class ForkStateParameters implements StateParameters {
  @Singular @Size(min = 2) List<String> parallelNodeIds;
}