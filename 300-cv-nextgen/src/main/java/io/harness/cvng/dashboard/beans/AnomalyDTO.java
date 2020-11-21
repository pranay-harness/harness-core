package io.harness.cvng.dashboard.beans;

import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalyStatus;

import java.util.SortedSet;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AnomalyDTO implements Comparable<AnomalyDTO> {
  String serviceName;
  String envName;
  Double riskScore;
  Long startTimestamp;
  Long endTimestamp;
  CVMonitoringCategory category;
  AnomalyStatus status;
  SortedSet<AnomalyDetailDTO> anomalyDetails;

  @Override
  public int compareTo(AnomalyDTO o) {
    int statusDiff = o.status.compareTo(status);
    if (statusDiff != 0) {
      return statusDiff;
    }

    int startTimeDiff = o.startTimestamp.compareTo(startTimestamp);
    if (startTimeDiff != 0) {
      return startTimeDiff;
    }

    return o.category.compareTo(category);
  }

  @Value
  @Builder
  public static class AnomalyDetailDTO implements Comparable<AnomalyDetailDTO> {
    String cvConfigId;
    double riskScore;
    SortedSet<AnomalyMetricDetail> metricDetails;

    @Override
    public int compareTo(AnomalyDetailDTO o) {
      int riskDiff = Double.compare(o.riskScore, riskScore);
      if (riskDiff != 0) {
        return riskDiff;
      }

      return o.cvConfigId.compareTo(cvConfigId);
    }
  }

  @Value
  @Builder
  public static class AnomalyMetricDetail implements Comparable<AnomalyMetricDetail> {
    String metricName;
    double riskScore;
    SortedSet<AnomalyTxnDetail> txnDetails;

    @Override
    public int compareTo(AnomalyMetricDetail o) {
      int riskDiff = Double.compare(o.riskScore, riskScore);
      if (riskDiff != 0) {
        return riskDiff;
      }

      return metricName.compareTo(o.metricName);
    }
  }

  @Value
  @Builder
  public static class AnomalyTxnDetail implements Comparable<AnomalyTxnDetail> {
    String groupName;
    double riskScore;

    @Override
    public int compareTo(AnomalyTxnDetail o) {
      int riskDiff = Double.compare(o.riskScore, riskScore);
      if (riskDiff != 0) {
        return riskDiff;
      }

      return groupName.compareTo(o.groupName);
    }
  }
}
