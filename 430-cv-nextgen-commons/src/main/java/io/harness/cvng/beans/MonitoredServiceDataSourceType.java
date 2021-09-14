/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
  @JsonProperty("Splunk") SPLUNK;

  public static Map<DataSourceType, MonitoredServiceDataSourceType> dataSourceTypeMonitoredServiceDataSourceTypeMap =
      new HashMap<DataSourceType, MonitoredServiceDataSourceType>() {
        {
          put(DataSourceType.APP_DYNAMICS, APP_DYNAMICS);
          put(DataSourceType.NEW_RELIC, NEW_RELIC);
          put(DataSourceType.STACKDRIVER_LOG, STACKDRIVER_LOG);
          put(DataSourceType.STACKDRIVER, STACKDRIVER);
          put(DataSourceType.PROMETHEUS, PROMETHEUS);
          put(DataSourceType.SPLUNK, SPLUNK);
        }
      };
}
