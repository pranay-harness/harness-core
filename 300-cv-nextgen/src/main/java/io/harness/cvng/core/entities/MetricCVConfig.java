/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.core.entities;

import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_SERVICE_GUARD_DATA_LENGTH;
import static io.harness.cvng.core.utils.ErrorMessageUtils.generateErrorMessageFromParam;

import static com.google.common.base.Preconditions.checkNotNull;

import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.beans.TimeSeriesThresholdActionType;
import io.harness.cvng.beans.TimeSeriesThresholdCriteria;
import io.harness.cvng.beans.TimeSeriesThresholdType;
import io.harness.cvng.core.beans.TimeRange;
import io.harness.cvng.core.utils.DateTimeUtils;
import io.harness.cvng.models.VerificationType;

import com.google.gson.Gson;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.UpdateOperations;

@Data
@SuperBuilder
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "MetricCVConfigKeys")
@EqualsAndHashCode(callSuper = true)
public abstract class MetricCVConfig extends CVConfig {
  private MetricPack metricPack;
  public TimeRange getFirstTimeDataCollectionTimeRange() {
    Instant endTime = DateTimeUtils.roundDownTo5MinBoundary(Instant.ofEpochMilli(this.getCreatedAt()));
    return TimeRange.builder()
        .startTime(endTime.minus(TIMESERIES_SERVICE_GUARD_DATA_LENGTH, ChronoUnit.MINUTES))
        .endTime(endTime)
        .build();
  }
  @Override
  public VerificationType getVerificationType() {
    return VerificationType.TIME_SERIES;
  }

  @Override
  public void validate() {
    super.validate();
    checkNotNull(metricPack, generateErrorMessageFromParam(MetricCVConfigKeys.metricPack));
  }

  @Override
  public boolean queueAnalysisForPreDeploymentTask() {
    return false;
  }

  public abstract static class MetricCVConfigUpdatableEntity<T extends MetricCVConfig, D extends MetricCVConfig>
      extends CVConfigUpdatableEntity<T, D> {
    protected void setCommonOperations(UpdateOperations<T> updateOperations, D metricCVConfig) {
      super.setCommonOperations(updateOperations, metricCVConfig);
      updateOperations.set(MetricCVConfigKeys.metricPack, metricCVConfig.getMetricPack());
    }
  }

  public Set<TimeSeriesThreshold> getThresholdsToCreateOnSaveForCustomProviders(
      String metricName, TimeSeriesMetricType metricType, List<TimeSeriesThresholdType> thresholdTypes) {
    Set<TimeSeriesThreshold> thresholds = new HashSet<>();
    metricType.getThresholds().forEach(threshold -> {
      thresholdTypes.forEach(type -> {
        Gson gson = new Gson();
        TimeSeriesThresholdCriteria criteria = gson.fromJson(gson.toJson(threshold), TimeSeriesThresholdCriteria.class);
        criteria.setThresholdType(type);
        thresholds.add(TimeSeriesThreshold.builder()
                           .accountId(getAccountId())
                           .projectIdentifier(getProjectIdentifier())
                           .dataSourceType(getType())
                           .metricType(metricType)
                           .metricName(metricName)
                           .action(TimeSeriesThresholdActionType.IGNORE)
                           .criteria(criteria)
                           .build());
      });
    });
    return thresholds;
  }

  public List<TimeSeriesThresholdType> getThresholdTypeOfMetric(String metricName, MetricCVConfig cvConfig) {
    Set<TimeSeriesThresholdType> thresholdTypes = new HashSet<>();
    for (MetricPack.MetricDefinition metricDefinition : cvConfig.getMetricPack().getMetrics()) {
      if (metricDefinition.getName().equals(metricName)) {
        metricDefinition.getThresholds().forEach(
            threshold -> thresholdTypes.add(threshold.getCriteria().getThresholdType()));
      }
    }
    return new ArrayList<>(thresholdTypes);
  }
}
