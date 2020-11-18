package io.harness.yaml.core.failurestrategy.retry;

import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.List;

@Value
@Builder
public class RetryFailureSpecConfig {
  @NotNull int retryCount;
  @NotNull List<String> retryInterval;
  @NotNull OnRetryFailureConfig onRetryFailure;
}
