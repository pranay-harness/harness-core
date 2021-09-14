/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
