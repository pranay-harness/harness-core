package io.harness.cvng.core.services.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import io.harness.cvng.beans.TimeSeriesDataCollectionRecord;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.entities.TimeSeriesRecord.TimeSeriesGroupValue;
import io.harness.cvng.core.entities.TimeSeriesRecord.TimeSeriesRecordKeys;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.UpdateOptions;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TimeSeriesServiceImpl implements TimeSeriesService {
  @Inject private HPersistence hPersistence;

  @Override
  public boolean save(List<TimeSeriesDataCollectionRecord> dataRecords) {
    UpdateOptions options = new UpdateOptions();
    options.upsert(true);
    TreeBasedTable<Long, String, TimeSeriesRecord> timeSeriesRecordMap = bucketTimeSeriesRecords(dataRecords);
    timeSeriesRecordMap.cellSet().forEach(timeSeriesRecordCell
        -> hPersistence.getDatastore(TimeSeriesRecord.class)
               .update(hPersistence.createQuery(TimeSeriesRecord.class)
                           .filter(TimeSeriesRecordKeys.cvConfigId, timeSeriesRecordCell.getValue().getCvConfigId())
                           .filter(TimeSeriesRecordKeys.hourStartBoundary, timeSeriesRecordCell.getRowKey())
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
      long hourBoundary =
          dataRecord.getTimeStamp() - Math.floorMod(dataRecord.getTimeStamp(), TimeUnit.HOURS.toMillis(1));
      dataRecord.getMetricValues().forEach(timeSeriesDataRecordMetricValue -> {
        String metricName = timeSeriesDataRecordMetricValue.getMetricName();
        if (!rv.contains(hourBoundary, metricName)) {
          rv.put(hourBoundary, metricName,
              TimeSeriesRecord.builder()
                  .accountId(dataRecord.getAccountId())
                  .cvConfigId(dataRecord.getCvConfigId())
                  .accountId(dataRecord.getAccountId())
                  .hourStartBoundary(hourBoundary)
                  .metricName(metricName)
                  .build());
        }

        timeSeriesDataRecordMetricValue.getTimeSeriesValues().forEach(timeSeriesDataRecordGroupValue
            -> rv.get(hourBoundary, metricName)
                   .getTimeSeriesGroupValues()
                   .add(TimeSeriesGroupValue.builder()
                            .groupName(timeSeriesDataRecordGroupValue.getGroupName())
                            .timeStamp(dataRecord.getTimeStamp())
                            .metricValue(timeSeriesDataRecordGroupValue.getValue())
                            .build()));
      });
    });
    return rv;
  }
}
