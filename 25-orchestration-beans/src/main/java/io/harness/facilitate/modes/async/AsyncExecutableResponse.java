package io.harness.facilitate.modes.async;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Redesign
public class AsyncExecutableResponse {
  @NonNull @Singular List<String> callbackIds;
}
