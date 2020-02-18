package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.common.VerificationConstants.VERIFICATION_HOST_PLACEHOLDERV2;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.instana.InstanaDataCollectionInfo;
import software.wings.service.impl.instana.InstanaTagFilter;
import software.wings.service.impl.instana.InstanaUtils;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
@FieldNameConstants(innerTypeName = "InstanaStateKeys")
public class InstanaState extends AbstractMetricAnalysisState {
  public static final String LATENCY = "latency";
  public static final String ERRORS = "errors";
  public static final String TRACES = "traces";
  private String analysisServerConfigId;
  private List<String> metrics;
  private String query;
  private String hostTagFilter;
  private List<InstanaTagFilter> tagFilters;

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
    if (isEmpty(metrics)) {
      errors.put(InstanaStateKeys.metrics, "select at least one metric value.");
    }
    if (isEmpty(query)) {
      errors.put(InstanaStateKeys.query, "query is a required field.");
    }
    if (query != null && !query.contains(VERIFICATION_HOST_PLACEHOLDERV2)) {
      errors.put(InstanaStateKeys.query, "query should contain " + VERIFICATION_HOST_PLACEHOLDERV2);
    }

    if (isEmpty(hostTagFilter)) {
      errors.put(InstanaStateKeys.hostTagFilter, "hostTagFilter is a required field.");
    }
    getTagFilters().forEach(tagFilter -> {
      if (isEmpty(tagFilter.getName())) {
        errors.put("tagFilter.name", "tagFilter.name is a required field.");
      }
      if (isEmpty(tagFilter.getValue())) {
        errors.put("tagFilter.value", "tagFilter.value is a required field.");
      }
      if (tagFilter.getOperator() == null) {
        errors.put("tagFilter.operator", "tagFilter.operator is a required field.");
      }
    });

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
    return logger;
  }

  @Override
  protected DataCollectionInfoV2 createDataCollectionInfo(
      ExecutionContext context, Map<String, String> hostsToCollect) {
    metricAnalysisService.saveMetricTemplates(context.getAppId(), StateType.INSTANA,
        context.getStateExecutionInstanceId(), null, InstanaUtils.createMetricTemplates(metrics));
    return InstanaDataCollectionInfo.builder()
        .connectorId(analysisServerConfigId)
        .workflowExecutionId(context.getWorkflowExecutionId())
        .stateExecutionId(context.getStateExecutionInstanceId())
        .workflowId(context.getWorkflowId())
        .accountId(appService.get(context.getAppId()).getAccountId())
        .envId(getEnvId(context))
        .applicationId(context.getAppId())
        .hosts(hostsToCollect.keySet())
        .hostsToGroupNameMap(hostsToCollect)
        .serviceId(getPhaseServiceId(context))
        .metrics(metrics)
        .query(query)
        .tagFilters(getTagFilters())
        .hostTagFilter(hostTagFilter)
        .build();
  }

  public List<InstanaTagFilter> getTagFilters() {
    if (tagFilters == null) {
      return Collections.emptyList();
    }
    return tagFilters;
  }

  @Override
  protected boolean isCVTaskEnqueuingEnabled(String accountId) {
    return true;
  }
}
