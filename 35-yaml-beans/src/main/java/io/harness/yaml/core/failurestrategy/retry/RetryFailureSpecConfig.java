package io.harness.yaml.core.failurestrategy.retry;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RetryFailureSpecConfig {
  @NotNull int retryCount;
  @NotNull List<String> retryInterval;
  @NotNull OnRetryFailureConfig onRetryFailure;
}
