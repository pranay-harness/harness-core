package io.harness.facilitate.modes.chain;

import io.harness.annotations.Redesign;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
@Redesign
public class AsyncChainResponse {
  boolean finalTask;
  @Singular List<String> correlationIds;
  String delegateTaskId;
}
