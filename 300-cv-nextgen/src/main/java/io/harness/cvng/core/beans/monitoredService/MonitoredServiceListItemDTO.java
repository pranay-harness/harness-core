package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.beans.MonitoredServiceType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class MonitoredServiceListItemDTO {
  String name;
  String identifier;
  String serviceRef;
  String environmentRef;
  MonitoredServiceType type;
  boolean healthMonitoringEnabled;
  int currentHealthScore;
  HistoricalTrend historicalTrend;

  public static class MonitoredServiceListItemDTOBuilder {
    public String getServiceRef() {
      return serviceRef;
    }

    public String getEnvironmentRef() {
      return environmentRef;
    }

    public HistoricalTrend getHistoricalTrend() {
      return historicalTrend;
    }
  }
}
