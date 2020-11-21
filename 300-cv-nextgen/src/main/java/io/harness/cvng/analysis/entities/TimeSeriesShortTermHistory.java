package io.harness.cvng.analysis.entities;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotation.HarnessEntity;
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
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TimeSeriesShortTermHistoryKeys")
@EqualsAndHashCode(callSuper = false, exclude = {"transactionMetricHistories"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "timeseriesShortTermHistory", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesShortTermHistory implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  @NotEmpty @FdIndex private String verificationTaskId;
  List<TransactionMetricHistory> transactionMetricHistories;

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "TransactionMetricHistoryKeys")
  public static class TransactionMetricHistory {
    private String transactionName;
    private List<MetricHistory> metricHistoryList;
  }

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "MetricHistoryKeys")
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class MetricHistory {
    private String metricName;
    private List<Double> value;
  }

  public static List<TransactionMetricHistory> convertFromMap(Map<String, Map<String, List<Double>>> txnMetricHistory) {
    if (txnMetricHistory == null) {
      return null;
    }
    List<TransactionMetricHistory> transactionMetricHistories = new ArrayList<>();
    txnMetricHistory.forEach((txn, metricHistory) -> {
      TransactionMetricHistory transactionMetricHistory =
          TransactionMetricHistory.builder().transactionName(txn).metricHistoryList(new ArrayList<>()).build();
      metricHistory.forEach((metric, values) -> {
        MetricHistory shortTermHistory = MetricHistory.builder().metricName(metric).value(values).build();
        transactionMetricHistory.getMetricHistoryList().add(shortTermHistory);
      });
      transactionMetricHistories.add(transactionMetricHistory);
    });
    return transactionMetricHistories;
  }

  public Map<String, Map<String, List<Double>>> convertToMap() {
    if (transactionMetricHistories == null) {
      return new HashMap<>();
    }
    Map<String, Map<String, List<Double>>> txnMetricHistoryMap = new HashMap<>();
    transactionMetricHistories.forEach(transactionMetricHistory -> {
      String txn = transactionMetricHistory.getTransactionName();
      if (!txnMetricHistoryMap.containsKey(txn)) {
        txnMetricHistoryMap.put(txn, new HashMap<>());
      }

      if (isNotEmpty(transactionMetricHistory.getMetricHistoryList())) {
        transactionMetricHistory.getMetricHistoryList().forEach(metricHistory -> {
          String metricName = metricHistory.getMetricName();
          if (!txnMetricHistoryMap.get(txn).containsKey(metricName)) {
            txnMetricHistoryMap.get(txn).put(metricName, new ArrayList<>());
          }

          txnMetricHistoryMap.get(txn).get(metricName).addAll(metricHistory.getValue());
        });
      }
    });
    return txnMetricHistoryMap;
  }
}
