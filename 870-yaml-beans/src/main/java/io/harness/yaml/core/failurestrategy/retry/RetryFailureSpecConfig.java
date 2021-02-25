package io.harness.yaml.core.failurestrategy.retry;

import io.harness.yaml.core.timeout.Timeout;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RetryFailureSpecConfig {
  @NotNull int retryCount;
  @NotNull List<Timeout> retryIntervals;
  @NotNull OnRetryFailureConfig onRetryFailure;
}
