package io.harness.cvng.metrics.beans;

import io.harness.metrics.AutoMetricContext;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class ApiCallLogMetricContext extends AutoMetricContext {
  public ApiCallLogMetricContext(String accountId, String provider, String verificationType) {
    put("accountId", accountId);
    put("provider", provider);
    put("verificationType", verificationType);
  }
}
