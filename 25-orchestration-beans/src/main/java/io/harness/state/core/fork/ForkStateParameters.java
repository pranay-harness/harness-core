package io.harness.state.core.fork;

import io.harness.annotations.Redesign;
import io.harness.state.io.StateParameters;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.Size;

@Value
@Builder
@Redesign
public class ForkStateParameters implements StateParameters {
  @Singular @Size(min = 2) List<String> parallelNodeIds;
}