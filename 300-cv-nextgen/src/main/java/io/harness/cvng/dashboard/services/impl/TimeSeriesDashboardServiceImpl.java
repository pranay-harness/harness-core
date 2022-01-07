/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.services.impl;

import static io.harness.cvng.analysis.CVAnalysisConstants.TIMESERIES_SERVICE_GUARD_WINDOW_SIZE;
import static io.harness.cvng.core.utils.DateTimeUtils.roundDownToMinBoundary;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.beans.TimeSeriesMetricType;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.TimeRangeParams;
import io.harness.cvng.core.beans.params.filterParams.TimeSeriesAnalysisFilter;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.TimeSeriesRecord;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.TimeSeriesRecordService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO;
import io.harness.cvng.dashboard.beans.TimeSeriesMetricDataDTO.MetricData;
import io.harness.cvng.dashboard.services.api.TimeSeriesDashboardService;
import io.harness.cvng.utils.CVNGParallelExecutor;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class TimeSeriesDashboardServiceImpl implements TimeSeriesDashboardService {
  @Inject private CVConfigService cvConfigService;
  @Inject private TimeSeriesRecordService timeSeriesRecordService;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private CVNGParallelExecutor cvngParallelExecutor;
  @Inject private ActivityService activityService;

  @Override
  public PageResponse<TimeSeriesMetricDataDTO> getTimeSeriesMetricData(
      ServiceEnvironmentParams serviceEnvironmentParams, TimeRangeParams timeRangeParams,
      TimeSeriesAnalysisFilter timeSeriesAnalysisFilter, PageParams pageParams) {
    List<CVConfig> cvConfigs;
    if (timeSeriesAnalysisFilter.filterByHealthSourceIdentifiers()) {
      cvConfigs = cvConfigService.list(serviceEnvironmentParams, timeSeriesAnalysisFilter.getHealthSourceIdentifiers());
    } else {
      cvConfigs = cvConfigService.list(serviceEnvironmentParams);
    }
    List<String> cvConfigIds = cvConfigs.stream().map(CVConfig::getUuid).collect(Collectors.toList());
    PageResponse<TimeSeriesMetricDataDTO> timeSeriesMetricDataDTOPageResponse = getMetricData(cvConfigIds,
        serviceEnvironmentParams.getAccountIdentifier(), serviceEnvironmentParams.getProjectIdentifier(),
        serviceEnvironmentParams.getOrgIdentifier(), serviceEnvironmentParams.getEnvironmentIdentifier(),
        serviceEnvironmentParams.getServiceIdentifier(), null, timeRangeParams.getStartTime(),
        timeRangeParams.getEndTime(), timeRangeParams.getStartTime(), timeSeriesAnalysisFilter.isAnomalous(),
        pageParams.getPage(), pageParams.getSize(), timeSeriesAnalysisFilter.getFilter());
    setUpMetricDataForFullTimeRange(timeRangeParams, timeSeriesMetricDataDTOPageResponse);
    return timeSeriesMetricDataDTOPageResponse;
  }

  private void setUpMetricDataForFullTimeRange(
      TimeRangeParams timeRangeParams, PageResponse<TimeSeriesMetricDataDTO> timeSeriesMetricDataDTOPageResponse) {
    Instant startTime = roundDownToMinBoundary(timeRangeParams.getStartTime(), 1);
    Instant endTime = roundDownToMinBoundary(timeRangeParams.getEndTime(), 1);
    timeSeriesMetricDataDTOPageResponse.getContent().forEach(timeSeriesMetricDataDTO -> {
      List<MetricData> metricDataList = new ArrayList<>();
      Instant time = startTime;
      while (time.isBefore(endTime) || time.compareTo(endTime) == 0) {
        metricDataList.add(MetricData.builder().timestamp(time.toEpochMilli()).value(null).risk(Risk.NO_DATA).build());
        time = time.plus(1, ChronoUnit.MINUTES);
      }
      timeSeriesMetricDataDTO.getMetricDataList().forEach(metricData -> {
        int index = (int) ChronoUnit.MINUTES.between(startTime, Instant.ofEpochMilli(metricData.getTimestamp()));
        if (index >= 0 && index < metricDataList.size()) {
          metricDataList.set(index, metricData);
        }
      });
      SortedSet<MetricData> metricDataSortedSet = new TreeSet<>(metricDataList);
      timeSeriesMetricDataDTO.setMetricDataList(metricDataSortedSet);
    });
  }

  @Override
  public PageResponse<TimeSeriesMetricDataDTO> getSortedMetricData(String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Long startTimeMillis, Long endTimeMillis, Long analysisStartTimeMillis,
      boolean anomalous, int page, int size, String filter, DataSourceType dataSourceType) {
    // TODO: Change this to a request body. This is too many query params.
    Instant startTime = Instant.ofEpochMilli(startTimeMillis);
    Instant endTime = Instant.ofEpochMilli(endTimeMillis);
    Instant analysisStartTime = Instant.ofEpochMilli(analysisStartTimeMillis);

    // get all the cvConfigs that belong to
    List<CVConfig> cvConfigList = cvConfigService.getConfigsOfProductionEnvironments(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier, monitoringCategory);
    if (dataSourceType != null) {
      cvConfigList = cvConfigList.stream()
                         .filter(cvConfig -> cvConfig.getType().equals(dataSourceType))
                         .collect(Collectors.toList());
    }
    List<String> cvConfigIds = cvConfigList.stream().map(CVConfig::getUuid).collect(Collectors.toList());

    return getMetricData(cvConfigIds, accountId, projectIdentifier, orgIdentifier, environmentIdentifier,
        serviceIdentifier, monitoringCategory, startTime, endTime, analysisStartTime, anomalous, page, size, filter);
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
                -> verificationTaskService.maybeGetVerificationTaskIds(accountId, verificationJobInstanceId))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    List<String> cvConfigIds = verificationTaskIds.stream()
                                   .map(verificationTaskId -> verificationTaskService.getCVConfigId(verificationTaskId))
                                   .collect(Collectors.toList());
    return getMetricData(cvConfigIds, accountId, projectIdentifier, orgIdentifier, environmentIdentifier,
        serviceIdentifier, null, Instant.ofEpochMilli(startTimeMillis), Instant.ofEpochMilli(endTimeMillis),
        activity.getActivityStartTime(), anomalousOnly, page, size, null);
  }

  private PageResponse<TimeSeriesMetricDataDTO> getMetricData(List<String> cvConfigIds, String accountId,
      String projectIdentifier, String orgIdentifier, String environmentIdentifier, String serviceIdentifier,
      CVMonitoringCategory monitoringCategory, Instant startTime, Instant endTime, Instant analysisStartTime,
      boolean anomalousOnly, int page, int size, String filter) {
    List<Callable<List<TimeSeriesRecord>>> recordsPerId = new ArrayList<>();

    // TODO: this should be passed as parameter, needs refactoring in other methods as well
    ServiceEnvironmentParams serviceEnvironmentParams = ServiceEnvironmentParams.builder()
                                                            .accountIdentifier(accountId)
                                                            .orgIdentifier(orgIdentifier)
                                                            .projectIdentifier(projectIdentifier)
                                                            .serviceIdentifier(serviceIdentifier)
                                                            .environmentIdentifier(environmentIdentifier)
                                                            .build();
    Map<String, DataSourceType> cvConfigIdToDataSourceTypeMap =
        cvConfigService.getDataSourceTypeForCVConfigs(serviceEnvironmentParams, cvConfigIds);
    cvConfigIds.forEach(cvConfigId -> recordsPerId.add(() -> {
      List<TimeSeriesRecord> timeSeriesRecordsfromDB =
          timeSeriesRecordService.getTimeSeriesRecordsForConfigs(Arrays.asList(cvConfigId), startTime, endTime, false);
      List<TimeSeriesRecord> timeSeriesRecords = Collections.synchronizedList(new ArrayList<>());
      if (isEmpty(timeSeriesRecordsfromDB)) {
        return timeSeriesRecords;
      }
      if (!anomalousOnly) {
        timeSeriesRecords.addAll(timeSeriesRecordsfromDB);
      } else {
        // it is possible that there are some transactions with good risk and some with bad.
        // We want to surface only those with bad. So filter out the good ones.

        List<TimeSeriesRecord> lastAnalysisRecords =
            timeSeriesRecordsfromDB.stream()
                .filter(record -> !record.getBucketStartTime().isBefore(analysisStartTime))
                .collect(Collectors.toList());

        Map<String, Set<String>> riskMetricGroupNamesMap = new HashMap<>();

        lastAnalysisRecords.forEach(timeSeriesRecord -> {
          Set<TimeSeriesRecord.TimeSeriesGroupValue> groupValuesWithRisk = timeSeriesRecord.getTimeSeriesGroupValues()
                                                                               .stream()
                                                                               .filter(gv -> gv.getRiskScore() > 0)
                                                                               .collect(Collectors.toSet());

          if (!riskMetricGroupNamesMap.containsKey(timeSeriesRecord.getMetricName())) {
            riskMetricGroupNamesMap.put(timeSeriesRecord.getMetricName(), new HashSet<>());
          }

          groupValuesWithRisk.forEach(
              gv -> riskMetricGroupNamesMap.get(timeSeriesRecord.getMetricName()).add(gv.getGroupName()));
        });

        for (TimeSeriesRecord tsRecord : timeSeriesRecordsfromDB) {
          String metricName = tsRecord.getMetricName();
          if (riskMetricGroupNamesMap.containsKey(metricName)) {
            Set<TimeSeriesRecord.TimeSeriesGroupValue> badGroupValues =
                tsRecord.getTimeSeriesGroupValues()
                    .stream()
                    .filter(timeSeriesGroupValue
                        -> riskMetricGroupNamesMap.get(metricName).contains(timeSeriesGroupValue.getGroupName()))
                    .collect(Collectors.toSet());

            tsRecord.setTimeSeriesGroupValues(badGroupValues);
            timeSeriesRecords.add(tsRecord);
          }
        }
      }
      return timeSeriesRecords;
    }));

    List<List<TimeSeriesRecord>> timeSeriesThatMatter = cvngParallelExecutor.executeParallel(recordsPerId);

    List<TimeSeriesRecord> timeSeriesRecords = new ArrayList<>();
    timeSeriesThatMatter.forEach(timeSeriesRecords::addAll);

    Map<String, TimeSeriesMetricDataDTO> transactionMetricDataMap = new HashMap<>();

    // make the timeseries records a flat map for transaction+metric combination
    timeSeriesRecords.forEach(record -> {
      String metricName = record.getMetricName();
      record.getTimeSeriesGroupValues().forEach(timeSeriesGroupValue -> {
        String txnName = timeSeriesGroupValue.getGroupName();
        String key = txnName + "." + metricName;
        if (isNotEmpty(filter) && !txnName.toLowerCase().contains(filter)
            && !metricName.toLowerCase().contains(filter.toLowerCase())) {
          return;
        }
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
                  .metricType(record.getMetricType())
                  .dataSourceType(cvConfigIdToDataSourceTypeMap.get(record.getVerificationTaskId()))
                  .build());
        }
        TimeSeriesMetricDataDTO timeSeriesMetricDataDTO = transactionMetricDataMap.get(key);

        if (!TimeSeriesMetricType.ERROR.equals(record.getMetricType())) {
          timeSeriesMetricDataDTO.addMetricData(timeSeriesGroupValue.getMetricValue(),
              timeSeriesGroupValue.getTimeStamp().toEpochMilli(), timeSeriesGroupValue.getRiskScore());
        } else if (timeSeriesGroupValue.getPercentValue() != null) {
          timeSeriesMetricDataDTO.addMetricData(timeSeriesGroupValue.getPercentValue(),
              timeSeriesGroupValue.getTimeStamp().toEpochMilli(), timeSeriesGroupValue.getRiskScore());
        } else {
          // if there is no percent for error then zero fill
          timeSeriesMetricDataDTO.addMetricData(Double.valueOf(0), timeSeriesGroupValue.getTimeStamp().toEpochMilli(),
              timeSeriesGroupValue.getRiskScore());
        }
      });
    });
    SortedSet<TimeSeriesMetricDataDTO> sortedMetricData = new TreeSet<>(transactionMetricDataMap.values());
    // updateLastWindowRisk(sortedMetricData, endTime);
    return PageUtils.offsetAndLimit(new ArrayList<>(sortedMetricData), page, size);
  }

  private void updateLastWindowRisk(SortedSet<TimeSeriesMetricDataDTO> sortedMetricData, Instant endTime) {
    // TODO: This seems buggy. Need to fix this with Sowmya's help.

    /*
     * We need to update the last 15 minutes in the UI to reflect the state which was present just after instant endTime
     * as these risks might later get updated if the next analysis window risk is high (as we use 15 min as test data in
     * LE).
     * */
    Instant lastWindowStartTime = endTime.minus(TIMESERIES_SERVICE_GUARD_WINDOW_SIZE, ChronoUnit.MINUTES);
    for (TimeSeriesMetricDataDTO timeSeriesMetricDataDTO : sortedMetricData) {
      Optional<Risk> risk = timeSeriesMetricDataDTO.getMetricDataList()
                                .stream()
                                .filter(data -> data.getTimestamp() >= lastWindowStartTime.toEpochMilli())
                                .map(MetricData::getRisk)
                                .findFirst();
      risk.ifPresent(timeSeriesRisk
          -> timeSeriesMetricDataDTO.getMetricDataList()
                 .stream()
                 .filter(data -> data.getTimestamp() >= lastWindowStartTime.toEpochMilli())
                 .forEach(data -> data.setRisk(timeSeriesRisk)));
    }
  }
}
