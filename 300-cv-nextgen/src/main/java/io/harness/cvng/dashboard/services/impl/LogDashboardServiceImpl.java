package io.harness.cvng.dashboard.services.impl;

import com.google.inject.Inject;

import io.harness.beans.NGPageResponse;
import io.harness.cvng.analysis.entities.LogAnalysisCluster;
import io.harness.cvng.analysis.entities.LogAnalysisCluster.Frequency;
import io.harness.cvng.analysis.entities.LogAnalysisResult;
import io.harness.cvng.analysis.entities.LogAnalysisResult.AnalysisResult;
import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisTag;
import io.harness.cvng.analysis.services.api.LogAnalysisService;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.utils.CVParallelExecutor;
import io.harness.cvng.dashboard.beans.AnalyzedLogDataDTO;
import io.harness.cvng.dashboard.beans.AnalyzedLogDataDTO.FrequencyDTO;
import io.harness.cvng.dashboard.beans.AnalyzedLogDataDTO.LogData;
import io.harness.cvng.dashboard.beans.LogDataByTag;
import io.harness.cvng.dashboard.beans.LogDataByTag.CountByTag;
import io.harness.cvng.dashboard.services.api.LogDashboardService;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class LogDashboardServiceImpl implements LogDashboardService {
  @Inject private LogAnalysisService logAnalysisService;
  @Inject private CVConfigService cvConfigService;
  @Inject private CVParallelExecutor cvParallelExecutor;

  @Override
  public NGPageResponse<AnalyzedLogDataDTO> getAnomalousLogs(String accountId, String projectIdentifier,
      String orgIdentifier, String serviceIdentifier, String environmentIdentifer, CVMonitoringCategory category,
      long startTimeMillis, long endTimeMillis, int page, int size) {
    return getLogs(accountId, projectIdentifier, orgIdentifier, serviceIdentifier, environmentIdentifer, category,
        startTimeMillis, endTimeMillis, Arrays.asList(LogAnalysisTag.UNEXPECTED, LogAnalysisTag.UNKNOWN), page, size);
  }

  @Override
  public NGPageResponse<AnalyzedLogDataDTO> getAllLogs(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifer, CVMonitoringCategory category, long startTimeMillis,
      long endTimeMillis, int page, int size) {
    return getLogs(accountId, projectIdentifier, orgIdentifier, serviceIdentifier, environmentIdentifer, category,
        startTimeMillis, endTimeMillis, Arrays.asList(LogAnalysisTag.values()), page, size);
  }

  @Override
  public SortedSet<LogDataByTag> getLogCountByTag(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifer, CVMonitoringCategory category, long startTimeMillis,
      long endTimeMillis) {
    Map<Long, Map<LogAnalysisTag, Integer>> logTagCountMap = new HashMap<>();
    List<LogDataByTag> logDataByTagList = new ArrayList<>();

    Instant startTime = Instant.ofEpochMilli(startTimeMillis);
    Instant endTime = Instant.ofEpochMilli(endTimeMillis);
    List<CVConfig> configs = cvConfigService.getConfigsOfProductionEnvironments(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifer, serviceIdentifier, category);
    List<String> cvConfigIds = configs.stream().map(CVConfig::getUuid).collect(Collectors.toList());
    for (String cvConfigId : cvConfigIds) {
      List<LogAnalysisResult> analysisResults =
          logAnalysisService.getAnalysisResults(cvConfigId, Arrays.asList(LogAnalysisTag.values()), startTime, endTime);
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
    logger.info("In getLogCountByTag, returning a set of size {}", sortedReturnSet.size());
    return sortedReturnSet;
  }

  private NGPageResponse<AnalyzedLogDataDTO> getLogs(String accountId, String projectIdentifier, String orgIdentifier,
      String serviceIdentifier, String environmentIdentifer, CVMonitoringCategory category, long startTimeMillis,
      long endTimeMillis, List<LogAnalysisTag> tags, int page, int size) {
    List<LogData> logDataToBeReturned = Collections.synchronizedList(new ArrayList<>());
    Instant startTime = Instant.ofEpochMilli(startTimeMillis);
    Instant endTime = Instant.ofEpochMilli(endTimeMillis);
    List<CVConfig> configs = cvConfigService.getConfigsOfProductionEnvironments(
        accountId, orgIdentifier, projectIdentifier, environmentIdentifer, serviceIdentifier, category);
    List<String> cvConfigIds = configs.stream().map(CVConfig::getUuid).collect(Collectors.toList());

    // for each cvConfigId, get the list of unknown and unexpected analysis results.
    // Total number of calls to DB = total number of cvConfigs that are part of this category for this service+env with
    // type as LOG
    Map<String, List<AnalysisResult>> cvConfigAnalysisResultMap = new ConcurrentHashMap<>();

    List<Callable<Map<String, List<AnalysisResult>>>> callables = new ArrayList<>();
    cvConfigIds.forEach(cvConfigId -> {
      callables.add(() -> {
        Map<String, List<AnalysisResult>> configResult = new HashMap<>();
        configResult.put(cvConfigId, getAnalysisResultForCvConfigId(cvConfigId, tags, startTime, endTime));
        return configResult;
      });
    });

    List<Optional<Map<String, List<AnalysisResult>>>> allResults = cvParallelExecutor.executeParrallel(callables);
    allResults.stream()
        .filter(result -> result.isPresent())
        .forEach(result -> cvConfigAnalysisResultMap.putAll(result.get()));

    // for each cvConfigId, make a call to get the labels/texts
    List<Callable<List<LogData>>> logDataCallables = new ArrayList<>();
    cvConfigAnalysisResultMap.keySet().forEach(cvConfigId -> {
      logDataCallables.add(() -> {
        List<AnalysisResult> analysisResults = cvConfigAnalysisResultMap.get(cvConfigId);
        Set<Long> labels = analysisResults.stream().map(AnalysisResult::getLabel).collect(Collectors.toSet());

        List<LogAnalysisCluster> clusters = logAnalysisService.getAnalysisClusters(cvConfigId, labels);
        return mergeClusterWithResults(analysisResults, clusters, startTime, endTime);
      });
    });

    List<Optional<List<LogData>>> logDataResults = cvParallelExecutor.executeParrallel(logDataCallables);
    logDataResults.stream()
        .filter(result -> result.isPresent())
        .forEach(result -> logDataToBeReturned.addAll(result.get()));

    SortedSet<AnalyzedLogDataDTO> sortedList = new TreeSet<>();
    // create the sorted set first. Then form the page response.
    logDataToBeReturned.forEach(logData -> {
      sortedList.add(AnalyzedLogDataDTO.builder()
                         .projectIdentifier(projectIdentifier)
                         .orgIdentifier(orgIdentifier)
                         .serviceIdentifier(serviceIdentifier)
                         .environmentIdentifier(environmentIdentifer)
                         .logData(logData)
                         .build());
    });

    return formPageResponse(page, size, sortedList);
  }

  private List<AnalysisResult> getAnalysisResultForCvConfigId(
      String cvConfigId, List<LogAnalysisTag> tags, Instant startTime, Instant endTime) {
    List<LogAnalysisResult> analysisResults =
        logAnalysisService.getAnalysisResults(cvConfigId, tags, startTime, endTime);
    return analysisResults.stream()
        .map(LogAnalysisResult::getLogAnalysisResults)
        .flatMap(Collection::stream)
        .filter(a -> tags.contains(a.getTag()))
        .collect(Collectors.toList());
  }

  private List<LogData> mergeClusterWithResults(
      List<AnalysisResult> analysisResults, List<LogAnalysisCluster> analysisClusters, Instant start, Instant end) {
    Map<Long, LogAnalysisTag> labelTagMap = new HashMap<>();
    analysisResults.forEach(result -> {
      Long label = result.getLabel();
      if (!labelTagMap.containsKey(label) || result.getTag().isMoreSevereThan(labelTagMap.get(label))) {
        labelTagMap.put(label, result.getTag());
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

      LogData data = LogData.builder()
                         .text(cluster.getText())
                         .label(cluster.getLabel())
                         .count(trendMap.values().stream().collect(Collectors.summingInt(Integer::intValue)))
                         .trend(frequencies)
                         .tag(labelTagMap.get(cluster.getLabel()))
                         .build();
      logDataList.add(data);
    });
    return logDataList;
  }

  private NGPageResponse<AnalyzedLogDataDTO> formPageResponse(
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

    return NGPageResponse.<AnalyzedLogDataDTO>builder()
        .pageSize(size)
        .pageCount(totalNumPages)
        .itemCount(analyzedLogData.size())
        .pageIndex(returnList.size() == 0 ? -1 : page)
        .empty(returnList.size() == 0)
        .content(returnList)
        .build();
  }
}
