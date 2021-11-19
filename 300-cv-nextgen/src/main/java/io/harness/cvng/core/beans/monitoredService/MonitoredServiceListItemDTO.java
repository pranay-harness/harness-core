package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.beans.MonitoredServiceType;
import io.harness.cvng.core.beans.change.ChangeSummaryDTO;

import java.util.List;
import java.util.Map;
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
  String serviceName;
  String environmentName;
  MonitoredServiceType type;
  boolean healthMonitoringEnabled;
  RiskData currentHealthScore;
  List<RiskData> dependentHealthScore;
  HistoricalTrend historicalTrend;
  ChangeSummaryDTO changeSummary;
  Map<String, String> tags;

  public static class MonitoredServiceListItemDTOBuilder {
    public String getServiceRef() {
      return serviceRef;
    }

    public String getEnvironmentRef() {
      return environmentRef;
    }

    public String getIdentifier() {
      return identifier;
    }

    public HistoricalTrend getHistoricalTrend() {
      return historicalTrend;
    }
  }
}
