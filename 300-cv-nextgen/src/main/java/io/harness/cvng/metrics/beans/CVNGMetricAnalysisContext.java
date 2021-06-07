package io.harness.cvng.metrics.beans;

import static io.harness.cvng.metrics.CVNGMetricsUtils.METRIC_LABEL_PREFIX;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.logging.log4j.ThreadContext;

@Data
@AllArgsConstructor
public class CVNGMetricAnalysisContext extends CVNGMetricContext {
  private String verificationTaskId;

  public CVNGMetricAnalysisContext(String accountId, String verificationTaskId) {
    super(accountId);
    ThreadContext.put(METRIC_LABEL_PREFIX + "verificationTaskId", verificationTaskId);
  }

  @Override
  public void close() {
    super.close();
    removeFromContext(CVNGMetricAnalysisContext.class);
  }
}
