package io.harness.cvng.metrics.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CVNGMetricContext implements AutoCloseable {
  private String accountId;
  private String verificationTaskId;

  @Override
  public void close() {}
}
