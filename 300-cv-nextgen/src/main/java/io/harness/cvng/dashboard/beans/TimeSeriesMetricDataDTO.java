package io.harness.cvng.dashboard.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.cvng.beans.CVMonitoringCategory;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class TimeSeriesMetricDataDTO implements Comparable<TimeSeriesMetricDataDTO> {
  String projectIdentifier;
  String orgIdentifier;
  String environmentIdentifier;
  String serviceIdentifier;

  CVMonitoringCategory category;

  String groupName;
  String metricName;

  @Builder.Default Integer totalRisk = 0;

  List<MetricData> metricDataList;

  @JsonIgnore
  public Integer getTotalRisk() {
    return totalRisk;
  }

  public void addMetricData(double value, long timestamp, Double risk) {
    if (metricDataList == null) {
      metricDataList = new ArrayList<>();
    }
    totalRisk += risk.intValue();
    metricDataList.add(MetricData.builder()
                           .timestamp(timestamp)
                           .value(value)
                           .risk(TimeSeriesRisk.getRiskFromScore(risk.intValue()))
                           .build());
  }

  @Override
  public int compareTo(@NotNull TimeSeriesMetricDataDTO o) {
    if (totalRisk != o.getTotalRisk()) {
      return o.getTotalRisk().compareTo(totalRisk);
    }
    if (!groupName.equals(o.getGroupName())) {
      return groupName.compareTo(o.getGroupName());
    }
    return metricName.compareTo(o.getMetricName());
  }

  @Data
  @Builder
  public static class MetricData {
    private long timestamp;
    private double value;
    TimeSeriesRisk risk;
  }

  public enum TimeSeriesRisk {
    NO_DATA,
    NO_ANALYSIS,
    LOW_RISK,
    MEDIUM_RISK,
    HIGH_RISK;

    public static TimeSeriesRisk getRiskFromScore(int risk) {
      switch (risk) {
        case -2:
          return NO_ANALYSIS;
        case -1:
          return NO_DATA;
        case 0:
          return LOW_RISK;
        case 1:
          return MEDIUM_RISK;
        case 2:
          return HIGH_RISK;
        default:
          throw new UnsupportedOperationException("Unknown risk score: " + risk);
      }
    }
  }
}
