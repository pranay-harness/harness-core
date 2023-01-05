/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public enum MonitoredServiceDataSourceType {
  @JsonProperty("AppDynamics") APP_DYNAMICS,
  @JsonProperty("NewRelic") NEW_RELIC,
  @JsonProperty("StackdriverLog") STACKDRIVER_LOG,
  @JsonProperty("Stackdriver") STACKDRIVER,
  @JsonProperty("Prometheus") PROMETHEUS,
  @JsonProperty("Splunk") SPLUNK,
  @JsonProperty("DatadogMetrics") DATADOG_METRICS,
  @JsonProperty("DatadogLog") DATADOG_LOG,
  @JsonProperty("Dynatrace") DYNATRACE,
  @JsonProperty("ErrorTracking") ERROR_TRACKING,
  @JsonProperty("CustomHealthMetric") CUSTOM_HEALTH_METRIC,
  @JsonProperty("CustomHealthLog") CUSTOM_HEALTH_LOG,
  @JsonProperty("SplunkMetric") SPLUNK_METRIC,
  @JsonProperty("ElasticSearch") ELASTICSEARCH,
  @JsonProperty("CloudWatchMetrics") CLOUDWATCH_METRICS,
  @JsonProperty("AwsPrometheus") AWS_PROMETHEUS,
  @JsonProperty("NextGenHealthSource") NEXT_GEN_HEALTH_SOURCE;
  public static final Map<DataSourceType, MonitoredServiceDataSourceType>
      dataSourceTypeMonitoredServiceDataSourceTypeMap = new HashMap<>();
  static {
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.APP_DYNAMICS, APP_DYNAMICS);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.NEW_RELIC, NEW_RELIC);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.STACKDRIVER_LOG, STACKDRIVER_LOG);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.STACKDRIVER, STACKDRIVER);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.PROMETHEUS, PROMETHEUS);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.SPLUNK, SPLUNK);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.DATADOG_METRICS, DATADOG_METRICS);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.DATADOG_LOG, DATADOG_LOG);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.DYNATRACE, DYNATRACE);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.CUSTOM_HEALTH_METRIC, CUSTOM_HEALTH_METRIC);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.CUSTOM_HEALTH_LOG, CUSTOM_HEALTH_LOG);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.ERROR_TRACKING, ERROR_TRACKING);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.SPLUNK_METRIC, SPLUNK_METRIC);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.ELASTICSEARCH, ELASTICSEARCH);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.CLOUDWATCH_METRICS, CLOUDWATCH_METRICS);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.AWS_PROMETHEUS, AWS_PROMETHEUS);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.SUMOLOGIC_LOG, NEXT_GEN_HEALTH_SOURCE);
    dataSourceTypeMonitoredServiceDataSourceTypeMap.put(DataSourceType.SUMOLOGIC_METRICS, NEXT_GEN_HEALTH_SOURCE);
  }
}