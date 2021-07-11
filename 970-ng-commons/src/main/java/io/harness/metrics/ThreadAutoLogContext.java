package io.harness.metrics;

import static io.harness.metrics.MetricConstants.METRIC_LABEL_PREFIX;

import java.util.Map;
import org.apache.logging.log4j.ThreadContext;

public class ThreadAutoLogContext implements AutoCloseable {
  Map<String, String> contextMap;

  public ThreadAutoLogContext(Map<String, String> contextMap) {
    this.contextMap = contextMap;
    for (Map.Entry<String, String> entry : contextMap.entrySet()) {
      ThreadContext.put(METRIC_LABEL_PREFIX + entry.getKey(), entry.getValue());
    }
  }

  protected void removeFromContext() {
    for (Map.Entry<String, String> entry : contextMap.entrySet()) {
      ThreadContext.remove(METRIC_LABEL_PREFIX + entry.getKey());
    }
  }

  @Override
  public void close() {
    removeFromContext();
  }
}
