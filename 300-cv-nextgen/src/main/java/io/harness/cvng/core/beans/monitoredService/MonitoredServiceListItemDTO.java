package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.beans.MonitoredServiceType;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder(buildMethodName = "unsafeBuild")
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

    public MonitoredServiceListItemDTO build() {
      return this.currentHealthScore(recentHealthScore(getHistoricalTrend())).unsafeBuild();
    }

    private int recentHealthScore(HistoricalTrend historicalTrend) {
      int recentHealthScore = -1;
      int iteratorFromLast = historicalTrend.getHealthScores().size() - 1;
      while (iteratorFromLast >= 0 && historicalTrend.getHealthScores().get(iteratorFromLast) == -1) {
        iteratorFromLast--;
      }
      if (iteratorFromLast >= 0) {
        recentHealthScore = historicalTrend.getHealthScores().get(iteratorFromLast).intValue();
      }
      return recentHealthScore;
    }
  }
}
