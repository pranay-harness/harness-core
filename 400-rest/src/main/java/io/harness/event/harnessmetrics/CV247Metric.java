/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.harnessmetrics;

import io.harness.event.model.Event;
import io.harness.event.model.EventConstants;
import io.harness.event.model.EventType;
import io.harness.metrics.HarnessMetricRegistry;

import com.google.common.collect.ImmutableList;
import io.prometheus.client.Collector.Type;
import java.util.List;

/*
 * Created by Adam Hancock on 7/25/2019
 */
public class CV247Metric implements HarnessMetricsEvent {
  private static final List<String> staticCVMetricLabels = ImmutableList.of(
      EventConstants.ACCOUNT_ID, EventConstants.VERIFICATION_STATE_TYPE, EventConstants.IS_24X7_ENABLED);

  @Override
  public String[] getLabelNames() {
    return staticCVMetricLabels.toArray(new String[] {});
  }

  @Override
  public Type getType() {
    return Type.GAUGE;
  }

  @Override
  public void handleEvent(HarnessMetricRegistry registry, Event event) {
    registry.recordGaugeValue(
        getEventType().name(), getLabelValues(event.getEventData()), event.getEventData().getValue());
  }

  @Override
  public void registerMetrics(HarnessMetricRegistry registry) {
    registry.registerGaugeMetric(getEventType().name(), getLabelNames(), getMetricHelpDocument());
  }

  @Override
  public EventType getEventType() {
    return EventType.CV_META_DATA;
  }

  @Override
  public String getMetricHelpDocument() {
    return "This metric is used to get the 24x7 CV metric data for different accounts";
  }
}
