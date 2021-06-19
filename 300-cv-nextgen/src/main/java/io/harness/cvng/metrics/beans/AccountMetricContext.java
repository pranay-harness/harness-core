package io.harness.cvng.metrics.beans;

import io.harness.metrics.AutoMetricContext;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class AccountMetricContext extends AutoMetricContext {
  public AccountMetricContext(String accountId) {
    put("accountId", accountId);
  }
}
