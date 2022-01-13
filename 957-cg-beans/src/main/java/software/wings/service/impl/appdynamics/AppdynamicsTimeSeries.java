/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.impl.appdynamics;

import static software.wings.metrics.MetricType.RESP_TIME;

import software.wings.metrics.MetricType;
import software.wings.metrics.appdynamics.AppdynamicsConstants;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by rsingh on 3/20/18.
 */
public enum AppdynamicsTimeSeries {
  RESPONSE_TIME_95(AppdynamicsConstants.RESPONSE_TIME_95, AppdynamicsConstants.RESPONSE_TIME_95, RESP_TIME),
  ERRORS_PER_MINUTE(AppdynamicsConstants.ERRORS_PER_MINUTE, AppdynamicsConstants.ERRORS_PER_MINUTE, MetricType.ERROR),
  STALL_COUNT(AppdynamicsConstants.STALL_COUNT, AppdynamicsConstants.STALL_COUNT, MetricType.ERROR),
  NUMBER_OF_SLOW_CALLS(
      AppdynamicsConstants.NUMBER_OF_SLOW_CALLS, AppdynamicsConstants.NUMBER_OF_SLOW_CALLS, MetricType.ERROR),
  CALLS_PER_MINUTE(AppdynamicsConstants.CALLS_PER_MINUTE, AppdynamicsConstants.CALLS_PER_MINUTE, MetricType.THROUGHPUT),
  AVG_RESPONSE_TIME(AppdynamicsConstants.AVG_RESPONSE_TIME, AppdynamicsConstants.AVG_RESPONSE_TIME, RESP_TIME);

  private final String metricName;
  private final String variableName;
  private final MetricType metricType;

  AppdynamicsTimeSeries(String metricName, String variableName, MetricType metricType) {
    this.metricName = metricName;
    this.variableName = variableName;
    this.metricType = metricType;
  }

  public String getMetricName() {
    return metricName;
  }

  public String getVariableName() {
    return variableName;
  }

  public MetricType getMetricType() {
    return metricType;
  }

  public static Set<String> getMetricsToTrack() {
    Set<String> rv = new HashSet<>();
    for (AppdynamicsTimeSeries timeSeries : AppdynamicsTimeSeries.values()) {
      rv.add(timeSeries.metricName);
    }
    return rv;
  }

  public static String getVariableName(String metricName) {
    for (AppdynamicsTimeSeries timeSeries : AppdynamicsTimeSeries.values()) {
      if (timeSeries.getMetricName().equals(metricName)) {
        return timeSeries.getVariableName();
      }
    }

    throw new IllegalStateException(metricName + " not defined in the time series");
  }

  public static Set<String> getErrorMetrics() {
    Set<String> errorMetrics = new HashSet<>();
    for (AppdynamicsTimeSeries metric : AppdynamicsTimeSeries.values()) {
      if (metric.getMetricType() == MetricType.ERROR) {
        errorMetrics.add(metric.getMetricName());
      }
    }
    return errorMetrics;
  }
}
