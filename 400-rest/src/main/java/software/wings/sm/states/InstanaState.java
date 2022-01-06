/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.instana.InstanaApplicationParams;
import software.wings.service.impl.instana.InstanaDataCollectionInfo;
import software.wings.service.impl.instana.InstanaInfraParams;
import software.wings.service.impl.instana.InstanaTagFilter;
import software.wings.service.impl.instana.InstanaUtils;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
@FieldNameConstants(innerTypeName = "InstanaStateKeys")
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class InstanaState extends AbstractMetricAnalysisState {
  private static final String LATENCY = "latency";
  private static final String ERRORS = "errors";
  private static final String TRACES = "traces";
  private String analysisServerConfigId;
  private InstanaInfraParams infraParams;
  private InstanaApplicationParams applicationParams;

  public InstanaState(String name) {
    super(name, StateType.INSTANA);
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, AnalysisContext analysisContext,
      VerificationStateAnalysisExecutionData executionData, Map<String, String> hosts) {
    throw new UnsupportedOperationException(
        "This should not get called. Instana is now using new data collection framework");
  }
  @Override
  public Map<String, String> validateFields() {
    Map<String, String> errors = new HashMap<>();
    if (infraParams == null && applicationParams == null) {
      errors.put("infraParams", "At least one metrics configuration should be defined");
      errors.put("applicationParams", "At least one metrics configuration should be defined");
    }
    if (infraParams != null) {
      infraParams.validateFields(errors);
    }
    if (applicationParams != null) {
      applicationParams.validateFields(errors);
    }
    return errors;
  }

  @Override
  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @DefaultValue("MEDIUM")
  public AnalysisTolerance getAnalysisTolerance() {
    if (isBlank(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
  }

  @Override
  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Override
  public Logger getLogger() {
    return log;
  }

  @Override
  protected DataCollectionInfoV2 createDataCollectionInfo(
      ExecutionContext context, Map<String, String> hostsToCollect) {
    metricAnalysisService.saveMetricTemplates(context.getAppId(), StateType.INSTANA,
        context.getStateExecutionInstanceId(), null, InstanaUtils.createMetricTemplates(infraParams));
    InstanaDataCollectionInfo instanaDataCollectionInfo =
        InstanaDataCollectionInfo.builder()
            .connectorId(
                getResolvedConnectorId(context, InstanaStateKeys.analysisServerConfigId, analysisServerConfigId))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowId(context.getWorkflowId())
            .accountId(appService.get(context.getAppId()).getAccountId())
            .envId(getEnvId(context))
            .applicationId(context.getAppId())
            .hosts(hostsToCollect.keySet())
            .hostsToGroupNameMap(hostsToCollect)
            .serviceId(getPhaseServiceId(context))
            .build();
    if (infraParams != null) {
      instanaDataCollectionInfo.setQuery(context.renderExpression(infraParams.getQuery()));
      instanaDataCollectionInfo.setMetrics(infraParams.getMetrics());
    }
    if (applicationParams != null) {
      instanaDataCollectionInfo.setTagFilters(renderExpressions(context, applicationParams.getTagFilters()));
      instanaDataCollectionInfo.setHostTagFilter(applicationParams.getHostTagFilter());
    }
    return instanaDataCollectionInfo;
  }

  private List<InstanaTagFilter> renderExpressions(ExecutionContext context, List<InstanaTagFilter> instanaTagFilters) {
    return instanaTagFilters.stream()
        .map(instanaTagFilter
            -> InstanaTagFilter.builder()
                   .value(context.renderExpression(instanaTagFilter.getValue()))
                   .operator(instanaTagFilter.getOperator())
                   .name(instanaTagFilter.getName())
                   .build())
        .collect(Collectors.toList());
  }

  @Override
  protected boolean isCVTaskEnqueuingEnabled(String accountId) {
    return true;
  }
}
