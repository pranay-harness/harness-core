package io.harness.metrics;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.ThreadContext;

public class AutoMetricContext implements AutoCloseable {
  private List<String> fields;
  protected AutoMetricContext() {
    fields = new ArrayList<>();
  }

  protected void put(String label, String value) {
    String field = MetricConstants.METRIC_LABEL_PREFIX + label;
    ThreadContext.put(field, value);
    fields.add(field);
  }

  @Override
  public void close() {
    ThreadContext.removeAll(fields);
  }
}
