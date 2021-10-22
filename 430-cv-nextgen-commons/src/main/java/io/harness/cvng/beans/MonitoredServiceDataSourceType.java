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
  @JsonProperty("DatadogMetric") DATADOG_METRIC,
  @JsonProperty("DatadogLog") DATADOG_LOG;

  public static Map<DataSourceType, MonitoredServiceDataSourceType> dataSourceTypeMonitoredServiceDataSourceTypeMap =
      new HashMap<DataSourceType, MonitoredServiceDataSourceType>() {
        {
          put(DataSourceType.APP_DYNAMICS, APP_DYNAMICS);
          put(DataSourceType.NEW_RELIC, NEW_RELIC);
          put(DataSourceType.STACKDRIVER_LOG, STACKDRIVER_LOG);
          put(DataSourceType.STACKDRIVER, STACKDRIVER);
          put(DataSourceType.PROMETHEUS, PROMETHEUS);
          put(DataSourceType.SPLUNK, SPLUNK);
          put(DataSourceType.DATADOG_METRICS, DATADOG_METRIC);
          put(DataSourceType.DATADOG_LOG, DATADOG_LOG);
        }
      };
}
