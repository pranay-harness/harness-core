package io.harness.cvng.metrics.beans;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CVNGMetricAnalysisContext extends AccountMetricContext {
  public CVNGMetricAnalysisContext(String accountId, String verificationTaskId) {
    super(accountId);
    put("verificationTaskId", verificationTaskId);
  }
}
