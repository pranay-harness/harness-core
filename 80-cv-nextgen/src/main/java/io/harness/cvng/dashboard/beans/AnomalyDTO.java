package io.harness.cvng.dashboard.beans;

import io.harness.cvng.core.beans.CVMonitoringCategory;
import io.harness.cvng.dashboard.entities.Anomaly.AnomalyStatus;
import lombok.Builder;
import lombok.Value;

import java.util.SortedSet;

@Value
@Builder
public class AnomalyDTO implements Comparable<AnomalyDTO> {
  Long startTimestamp;
  Long endTimestamp;
  CVMonitoringCategory category;
  AnomalyStatus status;
  SortedSet<AnomalyDetailDTO> anomalyDetails;

  @Override
  public int compareTo(AnomalyDTO o) {
    int riskDiff = o.status.compareTo(status);
    if (riskDiff != 0) {
      return riskDiff;
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
    Double riskScore;
    SortedSet<AnomalyTxnDetail> txnDetails;

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
  public static class AnomalyTxnDetail implements Comparable<AnomalyTxnDetail> {
    String groupName;
    String metricName;
    double riskScore;

    @Override
    public int compareTo(AnomalyTxnDetail o) {
      int riskDiff = Double.compare(o.riskScore, riskScore);
      if (riskDiff != 0) {
        return riskDiff;
      }

      int groupDiff = groupName.compareTo(o.groupName);
      if (groupDiff != 0) {
        return groupDiff;
      }

      return metricName.compareTo(o.metricName);
    }
  }
}
