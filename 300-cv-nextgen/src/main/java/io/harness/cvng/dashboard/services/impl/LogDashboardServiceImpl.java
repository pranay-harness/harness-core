/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.services.impl;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.analysis.beans.LiveMonitoringLogAnalysisClusterDTO;
import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.analysis.entities.LogAnalysisCluster.Frequency;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.LogAnalysisResult.AnalysisResult;
import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisTag;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.params.PageParams;
import io.harness.cvng.core.beans.params.ServiceEnvironmentParams;
import io.harness.cvng.core.beans.params.TimeRangeParams;
import io.harness.cvng.core.beans.params.filterParams.LiveMonitoringLogAnalysisFilter;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.VerificationTaskService;
import io.harness.cvng.dashboard.beans.AnalyzedLogDataDTO;
import io.harness.cvng.dashboard.beans.AnalyzedLogDataDTO.FrequencyDTO;
import io.harness.cvng.dashboard.beans.AnalyzedLogDataDTO.LogData;
import io.harness.cvng.dashboard.beans.LogDataByTag;
import io.harness.cvng.dashboard.beans.LogDataByTag.CountByTag;
import io.harness.cvng.dashboard.services.api.LogDashboardService;
import io.harness.cvng.utils.CVNGParallelExecutor;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogDashboardServiceImpl implements LogDashboardService {
  @Inject private LogAnalysisService logAnalysisService;
  @Inject private CVConfigService cvConfigService;
  @Inject private CVNGParallelExecutor cvngParallelExecutor;
  @Inject private VerificationTaskService verificationTaskService;
  @Inject private ActivityService activityService;

  @Override
  public PageResponse<AnalyzedLogDataDTO> getAnomalousLogs(String accountId, String projectIdentifier,
      String orgIdentifier, String serviceIdentifier, String environmentIdentifier, CVMonitoringCategory category,
      long startTimeMillis, long endTimeMillis, int page, int size) {
    return getLogs(accountId, projectIdentifier, orgIdentifier, serviceIdentifier, environmentIdentifier, category,
        startTimeMillis, endTimeMillis, Arrays.asList(LogAnalysisTag.UNEXPECTED, LogAnalysisTag.UNKNOWN), page, size);
  }

  @Override
  public PageResponse<AnalyzedLogDataDTO> getAllLogs(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifier, CVMonitoringCategory category, long startTimeMillis,
      long endTimeMillis, int page, int size) {
    return getLogs(accountId, projectIdentifier, orgIdentifier, serviceIdentifier, environmentIdentifier, category,
        startTimeMillis, endTimeMillis, Arrays.asList(LogAnalysisTag.values()), page, size);
  }

  @Override
  public PageResponse<AnalyzedLogDataDTO> getActivityLogs(String activityId, String accountId, String projectIdentifier,
      String orgIdentifier, String environmentIdentifier, String serviceIdentifier, Long startTimeMillis,
      Long endTimeMillis, boolean anomalousOnly, int page, int size) {
    List<String> cvConfigIds = getCVConfigIdsForActivity(accountId, activityId);
    List<LogAnalysisTag> tags = anomalousOnly ? Arrays.asList(LogAnalysisTag.UNEXPECTED, LogAnalysisTag.UNKNOWN)
                                              : Arrays.asList(LogAnalysisTag.values());
    return getLogs(accountId, projectIdentifier, orgIdentifier, serviceIdentifier, environmentIdentifier, tags,
        Instant.ofEpochMilli(startTimeMillis), Instant.ofEpochMilli(endTimeMillis), cvConfigIds, page, size);
  }

  @Override
  public PageResponse<AnalyzedLogDataDTO> getAllLogsData(ServiceEnvironmentParams serviceEnvironmentParams,
      TimeRangeParams timeRangeParams, LiveMonitoringLogAnalysisFilter liveMonitoringLogAnalysisFilter,
      PageParams pageParams) {
    List<CVConfig> configs = getCVConfigs(serviceEnvironmentParams, liveMonitoringLogAnalysisFilter);
    List<String> cvConfigIds = configs.stream().map(CVConfig::getUuid).collect(Collectors.toList());
    List<LogAnalysisTag> tags = liveMonitoringLogAnalysisFilter.filterByClusterTypes()
        ? liveMonitoringLogAnalysisFilter.getClusterTypes()
        : Arrays.asList(LogAnalysisTag.values());
    return getLogs(serviceEnvironmentParams.getAccountIdentifier(), serviceEnvironmentParams.getProjectIdentifier(),
        serviceEnvironmentParams.getOrgIdentifier(), serviceEnvironmentParams.getServiceIdentifier(),
        serviceEnvironmentParams.getEnvironmentIdentifier(), tags, timeRangeParams.getStartTime(),
        timeRangeParams.getEndTime(), cvConfigIds, pageParams.getPage(), pageParams.getSize());
  }

  @Override
  public List<LiveMonitoringLogAnalysisClusterDTO> getLogAnalysisClusters(
      ServiceEnvironmentParams serviceEnvironmentParams, TimeRangeParams timeRangeParams,
      LiveMonitoringLogAnalysisFilter liveMonitoringLogAnalysisFilter) {
    List<LiveMonitoringLogAnalysisClusterDTO> liveMonitoringLogAnalysisClusterDTOS = new ArrayList<>();
    List<String> cvConfigIds = getCVConfigs(serviceEnvironmentParams, liveMonitoringLogAnalysisFilter)
                                   .stream()
                                   .map(CVConfig::getUuid)
                                   .collect(Collectors.toList());
    List<LogAnalysisTag> tags = liveMonitoringLogAnalysisFilter.filterByClusterTypes()
        ? liveMonitoringLogAnalysisFilter.getClusterTypes()
        : Arrays.asList(LogAnalysisTag.values());

    cvConfigIds.forEach(cvConfigId -> {
      List<AnalysisResult> logAnalysisResults =
          getAnalysisResultForCvConfigId(cvConfigId, timeRangeParams.getStartTime(), timeRangeParams.getEndTime());

      Map<Long, LogAnalysisTag> labelTagMap = new HashMap<>();
      logAnalysisResults.forEach(result -> {
        Long label = result.getLabel();
        if (!labelTagMap.containsKey(label) || result.getTag().isMoreSevereThan(labelTagMap.get(label))) {
          labelTagMap.put(label, result.getTag());
        }
      });

      String verificationTaskId = verificationTaskService.getServiceGuardVerificationTaskId(
          serviceEnvironmentParams.getAccountIdentifier(), cvConfigId);
      List<LogAnalysisCluster> clusters =
          logAnalysisService.getAnalysisClusters(verificationTaskId, labelTagMap.keySet());
      clusters.forEach(logAnalysisCluster -> {
        liveMonitoringLogAnalysisClusterDTOS.add(LiveMonitoringLogAnalysisClusterDTO.builder()
                                                     .x(logAnalysisCluster.getX())
                                                     .y(logAnalysisCluster.getY())
                                                     .tag(labelTagMap.get(logAnalysisCluster.getLabel()))
                                                     .text(logAnalysisCluster.getText())
                                                     .build());
      });
    });
    return liveMonitoringLogAnalysisClusterDTOS.stream()
        .filter(cluster -> tags.contains(cluster.getTag()))
        .collect(Collectors.toList());
  }

  private List<CVConfig> getCVConfigs(ServiceEnvironmentParams serviceEnvironmentParams,
      LiveMonitoringLogAnalysisFilter liveMonitoringLogAnalysisFilter) {
    List<CVConfig> configs;
    if (liveMonitoringLogAnalysisFilter.filterByHealthSourceIdentifiers()) {
      configs =
          cvConfigService.list(serviceEnvironmentParams, liveMonitoringLogAnalysisFilter.getHealthSourceIdentifiers());
    } else {
      configs = cvConfigService.list(serviceEnvironmentParams);
    }
    return configs;
  }

  private List<String> getCVConfigIdsForActivity(String accountId, String activityId) {
    Activity activity = activityService.get(activityId);
    List<String> verificationJobInstanceIds = activity.getVerificationJobInstanceIds();
    Set<String> verificationTaskIds =
        verificationJobInstanceIds.stream()
            .map(verificationJobInstanceId
                -> verificationTaskService.getVerificationTaskIds(accountId, verificationJobInstanceId))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    return verificationTaskIds.stream()
        .map(verificationTaskId -> verificationTaskService.getCVConfigId(verificationTaskId))
        .collect(Collectors.toList());
  }

  @Override
  public SortedSet<LogDataByTag> getLogCountByTag(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifier, CVMonitoringCategory category, long startTimeMillis,
      long endTimeMillis) {
    List<CVConfig> configs = cvConfigService.getConfigsOfProductionEnvironments(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier, category);
    List<String> cvConfigIds = configs.stream().map(CVConfig::getUuid).collect(Collectors.toList());
    return getLogCountByTagForConfigs(accountId, cvConfigIds, startTimeMillis, endTimeMillis);
  }

  private SortedSet<LogDataByTag> getLogCountByTagForConfigs(
      String accountId, List<String> cvConfigIds, long startTimeMillis, long endTimeMillis) {
    Map<Long, Map<LogAnalysisTag, Integer>> logTagCountMap = new HashMap<>();
    List<LogDataByTag> logDataByTagList = new ArrayList<>();

    Instant startTime = Instant.ofEpochMilli(startTimeMillis);
    Instant endTime = Instant.ofEpochMilli(endTimeMillis);

    for (String verificationTaskId : cvConfigIds) {
      // String verificationTaskId = verificationTaskService.create(accountId, cvConfigId);
      List<LogAnalysisResult> analysisResults = logAnalysisService.getAnalysisResults(
          verificationTaskId, Arrays.asList(LogAnalysisTag.values()), startTime, endTime);
      analysisResults.forEach(result -> {
        Long analysisTime = result.getAnalysisStartTime().toEpochMilli();
        if (!logTagCountMap.containsKey(analysisTime)) {
          logTagCountMap.put(analysisTime, new HashMap<>());
        }
        result.getLogAnalysisResults().forEach(analysis -> {
          LogAnalysisTag tag = analysis.getTag();
          if (!logTagCountMap.get(analysisTime).containsKey(tag)) {
            logTagCountMap.get(analysisTime).put(tag, 0);
          }
          int newCount = logTagCountMap.get(analysisTime).get(tag) + analysis.getCount();
          logTagCountMap.get(analysisTime).put(tag, newCount);
        });
      });
    }

    logTagCountMap.forEach((timestamp, countMap) -> {
      LogDataByTag logDataByTag = LogDataByTag.builder().timestamp(timestamp).build();
      countMap.forEach(
          (tag, count) -> { logDataByTag.addCountByTag(CountByTag.builder().tag(tag).count(count).build()); });
      logDataByTagList.add(logDataByTag);
    });
    SortedSet<LogDataByTag> sortedReturnSet = new TreeSet<>(logDataByTagList);
    log.info("In getLogCountByTag, returning a set of size {}", sortedReturnSet.size());
    return sortedReturnSet;
  }

  @Override
  public SortedSet<LogDataByTag> getLogCountByTagForActivity(String accountId, String projectIdentifier,
      String orgIdentifier, String activityId, Instant startTime, Instant endTime) {
    List<String> cvConfigIds = getCVConfigIdsForActivity(accountId, activityId);
    return getLogCountByTagForConfigs(accountId, cvConfigIds, startTime.toEpochMilli(), endTime.toEpochMilli());
  }

  private PageResponse<AnalyzedLogDataDTO> getLogs(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifier, CVMonitoringCategory category, long startTimeMillis,
      long endTimeMillis, List<LogAnalysisTag> tags, int page, int size) {
    Instant startTime = Instant.ofEpochMilli(startTimeMillis);
    Instant endTime = Instant.ofEpochMilli(endTimeMillis);
    List<CVConfig> configs = cvConfigService.getConfigsOfProductionEnvironments(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifier, serviceIdentifier, category);
    List<String> cvConfigIds = configs.stream().map(CVConfig::getUuid).collect(Collectors.toList());
    return getLogs(accountId, projectIdentifier, orgIdentifier, serviceIdentifier, environmentIdentifier, tags,
        startTime, endTime, cvConfigIds, page, size);
  }

  private PageResponse<AnalyzedLogDataDTO> getLogs(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifer, List<LogAnalysisTag> tags, Instant startTime,
      Instant endTime, List<String> cvConfigIds, int page, int size) {
    // for each cvConfigId, get the list of unknown and unexpected analysis results.
    // Total number of calls to DB = total number of cvConfigs that are part of this category for this service+env with
    // type as LOG
    Map<String, List<AnalysisResult>> cvConfigAnalysisResultMap = new ConcurrentHashMap<>();

    List<LogData> logDataToBeReturned = Collections.synchronizedList(new ArrayList<>());
    List<Callable<Map<String, List<AnalysisResult>>>> callables = new ArrayList<>();
    cvConfigIds.forEach(cvConfigId -> {
      callables.add(() -> {
        Map<String, List<AnalysisResult>> configResult = new HashMap<>();
        configResult.put(cvConfigId, getAnalysisResultForCvConfigId(cvConfigId, startTime, endTime));
        return configResult;
      });
    });

    List<Map<String, List<AnalysisResult>>> allResults = cvngParallelExecutor.executeParallel(callables);
    allResults.forEach(result -> cvConfigAnalysisResultMap.putAll(result));

    // for each cvConfigId, make a call to get the labels/texts
    List<Callable<List<LogData>>> logDataCallables = new ArrayList<>();
    cvConfigAnalysisResultMap.keySet().forEach(cvConfigId -> {
      logDataCallables.add(() -> {
        List<AnalysisResult> analysisResults = cvConfigAnalysisResultMap.get(cvConfigId);
        Set<Long> labels = analysisResults.stream().map(AnalysisResult::getLabel).collect(Collectors.toSet());
        String verificationTaskId = verificationTaskService.getServiceGuardVerificationTaskId(accountId, cvConfigId);
        List<LogAnalysisCluster> clusters = logAnalysisService.getAnalysisClusters(verificationTaskId, labels);
        return mergeClusterWithResults(analysisResults, clusters, startTime, endTime);
      });
    });

    List<List<LogData>> logDataResults = cvngParallelExecutor.executeParallel(logDataCallables);
    logDataResults.forEach(result -> logDataToBeReturned.addAll(result));

    List<AnalyzedLogDataDTO> sortedList = new ArrayList<>();
    // create the sorted set first. Then form the page response.
    logDataToBeReturned.stream()
        .filter(logData -> tags.contains(logData.getTag()))
        .forEach(logData
            -> sortedList.add(AnalyzedLogDataDTO.builder()
                                  .projectIdentifier(projectIdentifier)
                                  .orgIdentifier(orgIdentifier)
                                  .serviceIdentifier(serviceIdentifier)
                                  .environmentIdentifier(environmentIdentifer)
                                  .logData(logData)
                                  .build()));
    Collections.sort(sortedList);
    return PageUtils.offsetAndLimit(sortedList, page, size);
  }

  private List<AnalysisResult> getAnalysisResultForCvConfigId(String cvConfigId, Instant startTime, Instant endTime) {
    List<LogAnalysisResult> analysisResults = logAnalysisService.getAnalysisResults(cvConfigId, startTime, endTime);
    return analysisResults.stream()
        .map(LogAnalysisResult::getLogAnalysisResults)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private List<LogData> mergeClusterWithResults(
      List<AnalysisResult> analysisResults, List<LogAnalysisCluster> analysisClusters, Instant start, Instant end) {
    Map<Long, AnalysisResult> labelTagMap = new HashMap<>();

    analysisResults.forEach(result -> {
      Long label = result.getLabel();
      if (!labelTagMap.containsKey(label) || result.getTag().isMoreSevereThan(labelTagMap.get(label).getTag())) {
        labelTagMap.put(label, result);
      }
    });

    List<LogData> logDataList = new ArrayList<>();

    analysisClusters.forEach(cluster -> {
      Map<Long, Integer> trendMap =
          cluster.getFrequencyTrend().stream().collect(Collectors.toMap(Frequency::getTimestamp, Frequency::getCount));

      // filter and keep only those within the timerange we want.
      trendMap = trendMap.entrySet()
                     .stream()
                     .filter(e
                         -> e.getKey() >= TimeUnit.MILLISECONDS.toMinutes(start.toEpochMilli())
                             && e.getKey() <= TimeUnit.MILLISECONDS.toMinutes(end.toEpochMilli()))
                     .collect(Collectors.toMap(x -> TimeUnit.MINUTES.toMillis(x.getKey()), x -> x.getValue()));

      List<FrequencyDTO> frequencies = new ArrayList<>();

      trendMap.forEach(
          (timestamp, count) -> frequencies.add(FrequencyDTO.builder().timestamp(timestamp).count(count).build()));

      AnalysisResult analysisResult = labelTagMap.get(cluster.getLabel());

      // TODO:going forward this if condition needs to be removed as we are saving high riskScore for unknown and
      // unexpected tags in the db itself.
      if (LogAnalysisTag.getAnomalousTags().contains(analysisResult.getTag())) {
        analysisResult.setRiskScore(1.0);
      }
      LogData data = LogData.builder()
                         .text(cluster.getText())
                         .label(cluster.getLabel())
                         .count(trendMap.values().stream().collect(Collectors.summingInt(Integer::intValue)))
                         .trend(frequencies)
                         .tag(analysisResult.getTag())
                         .riskScore(analysisResult.getRiskScore())
                         .riskStatus(analysisResult.getRisk())
                         .build();
      logDataList.add(data);
    });
    return logDataList;
  }

  private PageResponse<AnalyzedLogDataDTO> formPageResponse(
      int page, int size, SortedSet<AnalyzedLogDataDTO> analyzedLogData) {
    List<AnalyzedLogDataDTO> returnList = new ArrayList<>();

    int totalNumPages = analyzedLogData.size() / size;
    int startIndex = page * size;
    Iterator<AnalyzedLogDataDTO> iterator = analyzedLogData.iterator();
    int i = 0;
    while (iterator.hasNext()) {
      AnalyzedLogDataDTO analyzedLogDataDTO = iterator.next();
      if (i >= startIndex && returnList.size() < size) {
        returnList.add(analyzedLogDataDTO);
      }
      i++;
    }

    return PageResponse.<AnalyzedLogDataDTO>builder()
        .pageSize(size)
        .totalPages(totalNumPages)
        .totalItems(analyzedLogData.size())
        .pageIndex(returnList.size() == 0 ? -1 : page)
        .empty(returnList.size() == 0)
        .content(returnList)
        .build();
  }
}
