package io.harness.cvng.beans;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public enum MonitoredServiceDataSourceType {
  @JsonProperty("AppDynamics") APP_DYNAMICS;

  public static Map<DataSourceType, MonitoredServiceDataSourceType> dataSourceTypeMonitoredServiceDataSourceTypeMap =
      new HashMap<DataSourceType, MonitoredServiceDataSourceType>() {
        { put(DataSourceType.APP_DYNAMICS, APP_DYNAMICS); }
      };
}
