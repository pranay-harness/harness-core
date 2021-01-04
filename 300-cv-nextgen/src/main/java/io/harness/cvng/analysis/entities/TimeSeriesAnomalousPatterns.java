package io.harness.cvng.analysis.entities;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TimeSeriesAnomalousPatternsKeys")
@EqualsAndHashCode(callSuper = false, exclude = {"anomalies"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "timeseriesAnomalousPatterns", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesAnomalousPatterns implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  @FdIndex private String verificationTaskId;
  private List<TimeSeriesAnomalies> anomalies;

  public static List<TimeSeriesAnomalies> convertFromMap(
      Map<String, Map<String, List<TimeSeriesAnomalies>>> txnMetricAnomMap) {
    if (isNotEmpty(txnMetricAnomMap)) {
      List<TimeSeriesAnomalies> anomalyList = new ArrayList<>();
      txnMetricAnomMap.forEach((txn, metricAnomMap) -> {
        if (isNotEmpty(metricAnomMap)) {
          metricAnomMap.forEach((metric, anomalies) -> {
            if (isNotEmpty(anomalies)) {
              anomalies.forEach(anomaly -> {
                anomaly.setTransactionName(txn);
                anomaly.setMetricName(metric);
                anomalyList.add(anomaly);
              });
            }
          });
        }
      });
      return anomalyList;
    }
    return null;
  }

  public Map<String, Map<String, List<TimeSeriesAnomalies>>> convertToMap() {
    if (anomalies == null) {
      return new HashMap<>();
    }

    Map<String, Map<String, List<TimeSeriesAnomalies>>> txnMetricAnomMap = new HashMap<>();
    anomalies.forEach(anomaly -> {
      String txn = anomaly.getTransactionName();
      String metric = anomaly.getMetricName();
      if (!txnMetricAnomMap.containsKey(txn)) {
        txnMetricAnomMap.put(txn, new HashMap<>());
      }

      if (!txnMetricAnomMap.get(txn).containsKey(metric)) {
        txnMetricAnomMap.get(txn).put(metric, new ArrayList<>());
      }

      txnMetricAnomMap.get(txn).get(metric).add(anomaly);
    });

    return txnMetricAnomMap;
  }
}
