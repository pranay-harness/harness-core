/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.metrics;

import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.ThreadContext;

public class AutoMetricContext implements AutoCloseable {
  private final List<String> fields = new ArrayList<>();
  protected AutoMetricContext() {}

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
