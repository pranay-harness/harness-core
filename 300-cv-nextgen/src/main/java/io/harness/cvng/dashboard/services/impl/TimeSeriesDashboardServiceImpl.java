package io.harness.cvng.dashboard.services.impl;

import com.google.inject.Inject;

import io.harness.beans.NGPageResponse;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO;
import io.harness.cvng.dashboard.services.api.TimeSeriesDashboardService;
import io.harness.persistence.HPersistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class TimeSeriesDashboardServiceImpl implements TimeSeriesDashboardService {
  @Inject private CVConfigService cvConfigService;
  @Inject private HPersistence hPersistence;
  @Inject private TimeSeriesService timeSeriesService;

  @Override
  public NGPageResponse<TimeSeriesMetricDataDTO> getSortedMetricData(String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, int page, int size) {
    return getMetricData(accountId, projectIdentifier, orgIdentifier, environmentIdentifier, serviceIdentifier,
        monitoringCategory, startTimeMillis, endTimeMillis, false, page, size);
  }

  @Override
  public NGPageResponse<TimeSeriesMetricDataDTO> getSortedAnomalousMetricData(String accountId,
      String projectIdentifier, String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, int page, int size) {
    return getMetricData(accountId, projectIdentifier, orgIdentifier, environmentIdentifier, serviceIdentifier,
        monitoringCategory, startTimeMillis, endTimeMillis, true, page, size);
  }

  private NGPageResponse<TimeSeriesMetricDataDTO> getMetricData(String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, boolean anomalousOnly,
      int page, int size) {
    Instant startTime = Instant.ofEpochMilli(startTimeMillis);
    Instant endTime = Instant.ofEpochMilli(endTimeMillis);

    // get all the cvConfigs that belong to
    List<CVConfig> cvConfigList = cvConfigService.list(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier, monitoringCategory);
    List<String> cvConfigIds = cvConfigList.stream().map(CVConfig::getUuid).collect(Collectors.toList());

    List<TimeSeriesRecord> timeSeriesRecordsfromDB =
        timeSeriesService.getTimeSeriesRecordsForConfigs(cvConfigIds, startTime, endTime, anomalousOnly);

    List<TimeSeriesRecord> timeSeriesRecords = Collections.synchronizedList(new ArrayList<>());
    if (!anomalousOnly) {
      timeSeriesRecords.addAll(timeSeriesRecordsfromDB);
    } else {
      // it is possible that there are some transactions with good risk and some with bad.
      // We want to surface only those with bad. So filter out the good ones.
      // TODO: Move this to executor service once the log PR goes in
      timeSeriesRecordsfromDB.parallelStream().forEach(timeSeriesRecord -> {
        Set<TimeSeriesRecord.TimeSeriesGroupValue> groupValuesWithLowRisk = timeSeriesRecord.getTimeSeriesGroupValues()
                                                                                .stream()
                                                                                .filter(gv -> gv.getRiskScore() <= 0)
                                                                                .collect(Collectors.toSet());
        groupValuesWithLowRisk.stream().map(timeSeriesRecord.getTimeSeriesGroupValues()::remove);
        timeSeriesRecords.add(timeSeriesRecord);
      });
    }

    Map<String, TimeSeriesMetricDataDTO> transactionMetricDataMap = new HashMap<>();

    // make the timeseries records a flat map for transaction+metric combination
    timeSeriesRecords.forEach(record -> {
      String metricName = record.getMetricName();
      record.getTimeSeriesGroupValues().forEach(timeSeriesGroupValue -> {
        String txnName = timeSeriesGroupValue.getGroupName();
        String key = txnName + "." + metricName;
        if (!transactionMetricDataMap.containsKey(key)) {
          transactionMetricDataMap.put(key,
              TimeSeriesMetricDataDTO.builder()
                  .metricName(metricName)
                  .groupName(txnName)
                  .category(monitoringCategory)
                  .environmentIdentifier(environmentIdentifier)
                  .serviceIdentifier(serviceIdentifier)
                  .projectIdentifier(projectIdentifier)
                  .orgIdentifier(orgIdentifier)
                  .build());
        }
        TimeSeriesMetricDataDTO timeSeriesMetricDataDTO = transactionMetricDataMap.get(key);

        timeSeriesMetricDataDTO.addMetricData(timeSeriesGroupValue.getMetricValue(),
            timeSeriesGroupValue.getTimeStamp().toEpochMilli(), timeSeriesGroupValue.getRiskScore());
      });
    });
    SortedSet<TimeSeriesMetricDataDTO> sortedMetricData = new TreeSet<>(transactionMetricDataMap.values());
    return formPageResponse(sortedMetricData, page, size);
  }

  private NGPageResponse<TimeSeriesMetricDataDTO> formPageResponse(
      SortedSet<TimeSeriesMetricDataDTO> sortedMetricdata, int page, int size) {
    List<TimeSeriesMetricDataDTO> returnList = new ArrayList<>();

    int startIndex = page * size;
    Iterator<TimeSeriesMetricDataDTO> iterator = sortedMetricdata.iterator();
    int i = 0;
    while (iterator.hasNext()) {
      TimeSeriesMetricDataDTO metricDataDTO = iterator.next();
      if (i >= startIndex && returnList.size() < size) {
        returnList.add(metricDataDTO);
      }
      i++;
    }

    return NGPageResponse.<TimeSeriesMetricDataDTO>builder()
        .pageSize(size)
        .pageCount(sortedMetricdata.size() / size)
        .itemCount(returnList.size())
        .content(returnList)
        .build();
  }
}
