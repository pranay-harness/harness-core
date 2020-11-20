package io.harness.cvng.dashboard.services.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.TimeSeriesService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.core.utils.CVParallelExecutor;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO;
import io.harness.cvng.dashboard.services.api.TimeSeriesDashboardService;
import io.harness.ng.beans.PageResponse;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class TimeSeriesDashboardServiceImpl implements TimeSeriesDashboardService {
  @Inject private CVConfigService cvConfigService;
  @Inject private TimeSeriesService timeSeriesService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private CVParallelExecutor cvParallelExecutor;
  @Inject private ActivityService activityService;

  @Override
  public PageResponse<TimeSeriesMetricDataDTO> getSortedMetricData(String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, int page, int size) {
    return getMetricData(accountId, projectIdentifier, orgIdentifier, environmentIdentifier, serviceIdentifier,
        monitoringCategory, startTimeMillis, endTimeMillis, false, page, size);
  }

  @Override
  public PageResponse<TimeSeriesMetricDataDTO> getSortedAnomalousMetricData(String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, int page, int size) {
    return getMetricData(accountId, projectIdentifier, orgIdentifier, environmentIdentifier, serviceIdentifier,
        monitoringCategory, startTimeMillis, endTimeMillis, true, page, size);
  }

  @Override
  public PageResponse<TimeSeriesMetricDataDTO> getActivityMetrics(String activityId, String accountId,
      String projectIdentifier, String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      Long startTimeMillis, Long endTimeMillis, boolean anomalousOnly, int page, int size) {
    Activity activity = activityService.get(activityId);
    Preconditions.checkState(activity != null, "Invalid activityID");
    List<String> verificationJobInstanceIds = activity.getVerificationJobInstanceIds();
    Set<String> verificationTaskIds =
        verificationJobInstanceIds.stream()
            .map(verificationJobInstanceId
                -> verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    List<String> cvConfigIds = verificationTaskIds.stream()
                                   .map(verificationTaskId -> verificationTaskService.getCVConfigId(verificationTaskId))
                                   .collect(Collectors.toList());
    return getMetricData(cvConfigIds, projectIdentifier, orgIdentifier, environmentIdentifier, serviceIdentifier, null,
        Instant.ofEpochMilli(startTimeMillis), Instant.ofEpochMilli(endTimeMillis), anomalousOnly, page, size);
  }

  private PageResponse<TimeSeriesMetricDataDTO> getMetricData(String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, boolean anomalousOnly,
      int page, int size) {
    Instant startTime = Instant.ofEpochMilli(startTimeMillis);
    Instant endTime = Instant.ofEpochMilli(endTimeMillis);

    // get all the cvConfigs that belong to
    List<CVConfig> cvConfigList = cvConfigService.getConfigsOfProductionEnvironments(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier, monitoringCategory);
    List<String> cvConfigIds = cvConfigList.stream().map(CVConfig::getUuid).collect(Collectors.toList());

    return getMetricData(cvConfigIds, projectIdentifier, orgIdentifier, environmentIdentifier, serviceIdentifier,
        monitoringCategory, startTime, endTime, anomalousOnly, page, size);
  }

  private PageResponse<TimeSeriesMetricDataDTO> getMetricData(List<String> cvConfigIds, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Instant startTime, Instant endTime, boolean anomalousOnly, int page,
      int size) {
    List<Callable<List<TimeSeriesRecord>>> recordsPerId = new ArrayList<>();

    cvConfigIds.forEach(cvConfigId -> {
      recordsPerId.add(() -> {
        List<TimeSeriesRecord> timeSeriesRecordsfromDB =
            timeSeriesService.getTimeSeriesRecordsForConfigs(Arrays.asList(cvConfigId), startTime, endTime, false);
        List<TimeSeriesRecord> timeSeriesRecords = Collections.synchronizedList(new ArrayList<>());
        if (isEmpty(timeSeriesRecordsfromDB)) {
          return timeSeriesRecords;
        }
        if (!anomalousOnly) {
          timeSeriesRecords.addAll(timeSeriesRecordsfromDB);
        } else {
          // it is possible that there are some transactions with good risk and some with bad.
          // We want to surface only those with bad. So filter out the good ones.
          SortedSet<TimeSeriesRecord> recordsForConfig = new TreeSet<>();
          recordsForConfig.addAll(timeSeriesRecordsfromDB);
          Instant analysisTime = recordsForConfig.last().getBucketStartTime();

          List<TimeSeriesRecord> lastAnalysisRecords =
              timeSeriesRecordsfromDB.stream()
                  .filter(record -> record.getBucketStartTime().equals(analysisTime))
                  .collect(Collectors.toList());

          Map<String, List<String>> riskMetricGroupNamesMap = new HashMap<>();

          lastAnalysisRecords.forEach(timeSeriesRecord -> {
            Set<TimeSeriesRecord.TimeSeriesGroupValue> groupValuesWithRisk = timeSeriesRecord.getTimeSeriesGroupValues()
                                                                                 .stream()
                                                                                 .filter(gv -> gv.getRiskScore() > 0)
                                                                                 .collect(Collectors.toSet());
            riskMetricGroupNamesMap.put(timeSeriesRecord.getMetricName(), new ArrayList<>());

            groupValuesWithRisk.forEach(
                gv -> riskMetricGroupNamesMap.get(timeSeriesRecord.getMetricName()).add(gv.getGroupName()));
          });

          for (TimeSeriesRecord tsRecord : timeSeriesRecordsfromDB) {
            String metricName = tsRecord.getMetricName();
            if (riskMetricGroupNamesMap.containsKey(metricName)) {
              Iterator<TimeSeriesRecord.TimeSeriesGroupValue> groupValueIterator =
                  tsRecord.getTimeSeriesGroupValues().iterator();
              Set<TimeSeriesRecord.TimeSeriesGroupValue> badGroupValues = new HashSet<>();
              for (; groupValueIterator.hasNext();) {
                TimeSeriesRecord.TimeSeriesGroupValue gv = groupValueIterator.next();
                String groupName = gv.getGroupName();
                if (!riskMetricGroupNamesMap.get(metricName).contains(groupName)) {
                  groupValueIterator.remove();
                } else {
                  badGroupValues.add(gv);
                }
              }

              tsRecord.setTimeSeriesGroupValues(badGroupValues);
              // remove all those GroupValues that doent belong in the risk list.
              //              Set<TimeSeriesRecord.TimeSeriesGroupValue> groupValuesWithLowRisk =
              //                  tsRecord.getTimeSeriesGroupValues()
              //                      .stream()
              //                      .filter(gv ->
              //                      !riskMetricGroupNamesMap.get(metricName).contains(gv.getGroupName()))
              //                      .collect(Collectors.toSet());
              //              Set<TimeSeriesRecord.TimeSeriesGroupValue> groupValues =
              //              tsRecord.getTimeSeriesGroupValues(); groupValues.stream().filter(gv -> gv.getRiskScore() >
              //              0).map(tsRecord.getTimeSeriesGroupValues()::remove); for
              //              (TimeSeriesRecord.TimeSeriesGroupValue groupValue : groupValuesWithLowRisk) {
              //                groupValues.remove(groupValue);
              //              }
              // groupValuesWithLowRisk.stream().map(tsRecord.getTimeSeriesGroupValues()::remove);
              timeSeriesRecords.add(tsRecord);
            }
          }
        }
        return timeSeriesRecords;
      });
    });

    List<List<TimeSeriesRecord>> timeSeriesThatMatter = cvParallelExecutor.executeParallel(recordsPerId);

    List<TimeSeriesRecord> timeSeriesRecords = new ArrayList<>();
    timeSeriesThatMatter.forEach(timeSeriesRecords::addAll);

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

  private PageResponse<TimeSeriesMetricDataDTO> formPageResponse(
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

    int totalNumPages = sortedMetricdata.size() / size + 1;

    return PageResponse.<TimeSeriesMetricDataDTO>builder()
        .pageSize(size)
        .totalPages(totalNumPages)
        .totalItems(sortedMetricdata.size())
        .pageIndex(returnList.size() == 0 ? -1 : page)
        .empty(returnList.size() == 0)
        .content(returnList)
        .build();
  }
}
