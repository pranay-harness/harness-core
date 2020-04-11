package io.harness.facilitate.modes.async;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Builder
@Redesign
public class AsyncExecutableResponse {
  @NotNull @Singular List<String> callbackIds;
}
