/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
