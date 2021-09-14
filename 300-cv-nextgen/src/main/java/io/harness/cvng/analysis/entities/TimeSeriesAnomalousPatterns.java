/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.analysis.entities;

import static io.harness.data.encoding.EncodingUtils.compressString;
import static io.harness.data.encoding.EncodingUtils.deCompressString;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.cvng.analysis.beans.TimeSeriesAnomalies;
import io.harness.mongo.index.FdIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import io.harness.serializer.JsonUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
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
@StoreIn(DbAliases.CVNG)
public final class TimeSeriesAnomalousPatterns implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  @FdIndex private long createdAt;
  @FdIndex private long lastUpdatedAt;
  @FdIndex private String verificationTaskId;
  private List<TimeSeriesAnomalies> anomalies;
  private byte[] compressedAnomalies;

  public byte[] getCompressedAnomalies() {
    if (isEmpty(compressedAnomalies)) {
      return new byte[0];
    }

    return compressedAnomalies;
  }

  public void compressAnomalies() {
    if (isNotEmpty(anomalies)) {
      try {
        setCompressedAnomalies(compressString(JsonUtils.asJson(anomalies)));
        setAnomalies(null);
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  public void deCompressAnomalies() {
    if (isNotEmpty(compressedAnomalies)) {
      try {
        String decompressedAnomalies = deCompressString(compressedAnomalies);
        setAnomalies(JsonUtils.asObject(decompressedAnomalies, new TypeReference<List<TimeSeriesAnomalies>>() {}));
        setCompressedAnomalies(null);
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    }
  }

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
