/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.harnessmetrics;

import io.harness.event.model.Event;
import io.harness.event.model.EventData;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;

import io.prometheus.client.Collector;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface HarnessMetricsEvent {
  String[] getLabelNames();

  Collector.Type getType();

  EventType getEventType();

  String getMetricHelpDocument();

  void handleEvent(HarnessMetricRegistry registry, Event event);

  void registerMetrics(HarnessMetricRegistry registry);

  default String[] getLabelValues(EventData eventData) {
    final String[] labelNames = getLabelNames();
    if (labelNames != null && labelNames.length > 0) {
      Map<String, String> properties = eventData.getProperties();
      List<String> labelValues = new ArrayList();
      for (String label : labelNames) {
        labelValues.add(properties.get(label));
      }
      return labelValues.toArray(new String[labelValues.size()]);
    }

    return null;
  };
}
