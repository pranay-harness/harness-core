package io.harness.cvng.core.services.impl;

import static io.harness.cvng.core.services.CVNextGenConstants.CV_ANALYSIS_WINDOW_MINUTES;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;
import io.harness.cvng.core.beans.TimeSeriesMetricDefinition;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricCVConfig;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.entities.TimeSeriesRecord.TimeSeriesGroupValue;
import io.harness.cvng.core.entities.TimeSeriesRecord.TimeSeriesRecordKeys;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.MetricPackService;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.UpdateOptions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimeSeriesServiceImpl implements TimeSeriesService {
  @Inject private HPersistence hPersistence;
  @Inject private CVConfigService cvConfigService;
  @Inject private MetricPackService metricPackService;

  @Override
  public boolean save(List<TimeSeriesDataCollectionRecord> dataRecords) {
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);
    TreeBasedTable<Long, String, TimeSeriesRecord> timeSeriesRecordMap = bucketTimeSeriesRecords(dataRecords);
    timeSeriesRecordMap.cellSet().forEach(timeSeriesRecordCell
        -> hPersistence.getDatastore(TimeSeriesRecord.class)
               .update(hPersistence.createQuery(TimeSeriesRecord.class)
                           .filter(TimeSeriesRecordKeys.cvConfigId, timeSeriesRecordCell.getValue().getCvConfigId())
                           .filter(TimeSeriesRecordKeys.bucketStartTime,
                               Instant.ofEpochMilli(timeSeriesRecordCell.getRowKey()))
                           .filter(TimeSeriesRecordKeys.metricName, timeSeriesRecordCell.getColumnKey()),
                   hPersistence.createUpdateOperations(TimeSeriesRecord.class)
                       .set(TimeSeriesRecordKeys.accountId, timeSeriesRecordCell.getValue().getAccountId())
                       .addToSet(TimeSeriesRecordKeys.timeSeriesGroupValues,
                           Lists.newArrayList(timeSeriesRecordCell.getValue().getTimeSeriesGroupValues())),
                   options));
    return true;
  }

  private TreeBasedTable<Long, String, TimeSeriesRecord> bucketTimeSeriesRecords(
      List<TimeSeriesDataCollectionRecord> dataRecords) {
    TreeBasedTable<Long, String, TimeSeriesRecord> rv = TreeBasedTable.create();
    dataRecords.forEach(dataRecord -> {
      long bucketBoundary = dataRecord.getTimeStamp()
          - Math.floorMod(dataRecord.getTimeStamp(), TimeUnit.MINUTES.toMillis(CV_ANALYSIS_WINDOW_MINUTES));
      dataRecord.getMetricValues().forEach(timeSeriesDataRecordMetricValue -> {
        String metricName = timeSeriesDataRecordMetricValue.getMetricName();
        if (!rv.contains(bucketBoundary, metricName)) {
          rv.put(bucketBoundary, metricName,
              TimeSeriesRecord.builder()
                  .accountId(dataRecord.getAccountId())
                  .cvConfigId(dataRecord.getCvConfigId())
                  .accountId(dataRecord.getAccountId())
                  .bucketStartTime(Instant.ofEpochMilli(bucketBoundary))
                  .metricName(metricName)
                  .build());
        }

        timeSeriesDataRecordMetricValue.getTimeSeriesValues().forEach(timeSeriesDataRecordGroupValue
            -> rv.get(bucketBoundary, metricName)
                   .getTimeSeriesGroupValues()
                   .add(TimeSeriesGroupValue.builder()
                            .groupName(timeSeriesDataRecordGroupValue.getGroupName())
                            .timeStamp(Instant.ofEpochMilli(dataRecord.getTimeStamp()))
                            .metricValue(timeSeriesDataRecordGroupValue.getValue())
                            .build()));
      });
    });
    return rv;
  }

  @Override
  public List<TimeSeriesMetricDefinition> getTimeSeriesMetricDefinitions(String cvConfigId) {
    CVConfig cvConfig = cvConfigService.get(cvConfigId);
    Preconditions.checkNotNull(cvConfig, "could not find datasource with id ", cvConfigId);

    MetricCVConfig metricCVConfig = (MetricCVConfig) cvConfig;

    List<TimeSeriesMetricDefinition> timeSeriesMetricDefinitions = new ArrayList<>();
    // add project level thresholds
    List<TimeSeriesThreshold> metricPackThresholds =
        metricPackService.getMetricPackThresholds(metricCVConfig.getAccountId(), metricCVConfig.getProjectIdentifier(),
            metricCVConfig.getMetricPack().getIdentifier(), metricCVConfig.getType());
    metricPackThresholds.forEach(timeSeriesThreshold
        -> timeSeriesMetricDefinitions.add(TimeSeriesMetricDefinition.builder()
                                               .metricName(timeSeriesThreshold.getMetricName())
                                               .metricType(timeSeriesThreshold.getMetricType())
                                               .metricGroupName(timeSeriesThreshold.getMetricGroupName())
                                               .actionType(timeSeriesThreshold.getAction())
                                               .comparisonType(timeSeriesThreshold.getCriteria().getType())
                                               .action(timeSeriesThreshold.getCriteria().getAction())
                                               .occurrenceCount(timeSeriesThreshold.getCriteria().getOccurrenceCount())
                                               .thresholdType(timeSeriesThreshold.getCriteria().getThresholdType())
                                               .value(timeSeriesThreshold.getCriteria().getValue())
                                               .build()));

    // add data source level thresholds
    metricCVConfig.getMetricPack().getMetrics().forEach(metricDefinition -> {
      if (isNotEmpty(metricDefinition.getThresholds())) {
        metricDefinition.getThresholds().forEach(timeSeriesThreshold
            -> timeSeriesMetricDefinitions.add(
                TimeSeriesMetricDefinition.builder()
                    .metricName(metricDefinition.getName())
                    .metricType(metricDefinition.getType())
                    .metricGroupName(timeSeriesThreshold.getMetricGroupName())
                    .actionType(timeSeriesThreshold.getAction())
                    .comparisonType(timeSeriesThreshold.getCriteria().getType())
                    .action(timeSeriesThreshold.getCriteria().getAction())
                    .occurrenceCount(timeSeriesThreshold.getCriteria().getOccurrenceCount())
                    .thresholdType(timeSeriesThreshold.getCriteria().getThresholdType())
                    .value(timeSeriesThreshold.getCriteria().getValue())
                    .build()));
      }
    });
    return timeSeriesMetricDefinitions;
  }
}
