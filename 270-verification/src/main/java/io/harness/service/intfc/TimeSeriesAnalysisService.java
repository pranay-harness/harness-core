/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.service.intfc;

import io.harness.beans.SortOrder.OrderType;
import io.harness.entities.TimeSeriesAnomaliesRecord;
import io.harness.entities.TimeSeriesCumulativeSums;
import io.harness.validation.Create;

import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.MetricAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLAnalysisRecord;
import software.wings.service.impl.analysis.TimeSeriesMLScores;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.analysis.Version;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.sm.StateType;
import software.wings.verification.CVConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

/**
 * Created by rsingh on 9/26/17.
 */
public interface TimeSeriesAnalysisService {
  @ValidationGroups(Create.class)
  boolean saveMetricData(@NotNull String accountId, String appId, String stateExecutionId, String delegateTaskId,
      @Valid List<NewRelicMetricDataRecord> metricData);

  @ValidationGroups(Create.class)
  void saveAnalysisRecordsIgnoringDuplicate(NewRelicMetricAnalysisRecord metricAnalysisRecord);

  @ValidationGroups(Create.class)
  boolean saveAnalysisRecordsML(String accountId, @NotNull StateType stateType, @NotNull String appId,
      @NotNull String stateExecutionId, @NotNull String workflowExecutionId, String groupName,
      @NotNull Integer analysisMinute, @NotNull String taskId, String baseLineExecutionId, String cvConfigId,
      @Valid MetricAnalysisRecord mlAnalysisResponse, String tag);

  @ValidationGroups(Create.class) void saveTimeSeriesMLScores(TimeSeriesMLScores scores);

  List<TimeSeriesMLScores> getTimeSeriesMLScores(String appId, String workflowId, int analysisMinute, int limit);

  Set<NewRelicMetricDataRecord> getRecords(String appId, String stateExecutionId, String groupName, Set<String> nodes,
      int analysisMinute, int analysisStartMinute, String accountId);

  Set<NewRelicMetricDataRecord> getPreviousSuccessfulRecords(String appId, String workflowExecutionID, String groupName,
      int analysisMinute, int analysisStartMinute, String accountId);

  List<String> getLastSuccessfulWorkflowExecutionIds(String appId, String workflowId, String serviceId);

  NewRelicMetricDataRecord getHeartBeat(StateType stateType, String stateExecutionId, String workflowExecutionId,
      String serviceId, String metricGroup, OrderType orderType, String accountId);

  void bumpCollectionMinuteToProcess(String appId, String stateExecutionId, String workflowExecutionId,
      String groupName, int analysisMinute, String accountId);

  int getMaxControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName, String accountId);

  int getMinControlMinuteWithData(StateType stateType, String appId, String serviceId, String workflowId,
      String workflowExecutionId, String groupName, String accountId);

  String getLastSuccessfulWorkflowExecutionIdWithData(
      StateType stateType, String appId, String workflowId, String serviceId);

  Map<String, Map<String, TimeSeriesMetricDefinition>> getMetricTemplate(String appId, StateType stateType,
      String stateExecutionId, String serviceId, String cvConfigId, String groupName);

  Map<String, Map<String, TimeSeriesMetricDefinition>> getMetricTemplateWithCategorizedThresholds(String appId,
      StateType stateType, String stateExecutionId, String serviceId, String cvConfigId, String groupName,
      Version version);

  NewRelicMetricDataRecord getAnalysisMinute(StateType stateType, String appId, String stateExecutionId,
      String workflowExecutionId, String serviceId, String groupName, String accountId);

  Map<String, TimeSeriesMetricDefinition> getMetricTemplates(
      String accountId, StateType stateType, String stateExecutionId, String cvConfigId);

  Map<String, TimeSeriesMlAnalysisGroupInfo> getMetricGroups(String appId, String stateExecutionId);

  void saveMetricTemplates(String accountId, String appId, StateType stateType, String stateExecutionId,
      Map<String, TimeSeriesMetricDefinition> metricTemplates);

  long getMaxCVCollectionMinute(String appId, String cvConfigId, String accountId);

  long getLastCVAnalysisMinute(String appId, String cvConfigId);

  TimeSeriesMLAnalysisRecord getFailFastAnalysisRecord(String appId, String stateExecutionId);

  Set<NewRelicMetricDataRecord> getMetricRecords(
      String cvConfigId, int analysisStartMinute, int analysisEndMinute, String tag, String accountId);

  TimeSeriesMLAnalysisRecord getPreviousAnalysis(String appId, String cvConfigId, long dataCollectionMin, String tag);

  List<TimeSeriesMLAnalysisRecord> getHistoricalAnalysis(
      String accountId, String appId, String serviceId, String cvConfigId, long analysisMin, String tag);

  TimeSeriesAnomaliesRecord getPreviousAnomalies(
      String appId, String cvConfigId, Map<String, List<String>> metrics, String tag);

  Set<TimeSeriesCumulativeSums> getCumulativeSumsForRange(
      String appId, String cvConfigId, int startMinute, int endMinute, String tag);

  Set<String> getKeyTransactions(String cvConfigId);

  long getLastDataCollectedMinute(String appId, String stateExecutionId, StateType stateType);

  Optional<Long> getCreatedTimeOfLastCollection(CVConfiguration cvConfiguration);

  int getNumberOfAnalysisAboveThresholdSince(int analysisMinute, String cvConfigId, double riskScore);
}
