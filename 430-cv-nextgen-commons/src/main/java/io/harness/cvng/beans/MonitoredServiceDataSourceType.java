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
  @JsonProperty("ErrorTracking") ERROR_TRACKING,
  @JsonProperty("CustomHealth") CUSTOM_HEALTH;

  public static Map<DataSourceType, MonitoredServiceDataSourceType> dataSourceTypeMonitoredServiceDataSourceTypeMap =
      new HashMap<DataSourceType, MonitoredServiceDataSourceType>() {
        {
          put(DataSourceType.APP_DYNAMICS, APP_DYNAMICS);
          put(DataSourceType.NEW_RELIC, NEW_RELIC);
          put(DataSourceType.STACKDRIVER_LOG, STACKDRIVER_LOG);
          put(DataSourceType.STACKDRIVER, STACKDRIVER);
          put(DataSourceType.PROMETHEUS, PROMETHEUS);
          put(DataSourceType.SPLUNK, SPLUNK);
          put(DataSourceType.DATADOG_METRICS, DATADOG_METRICS);
          put(DataSourceType.DATADOG_LOG, DATADOG_LOG);
          put(DataSourceType.CUSTOM_HEALTH, CUSTOM_HEALTH);
          put(DataSourceType.ERROR_TRACKING, ERROR_TRACKING);
        }
      };
}
