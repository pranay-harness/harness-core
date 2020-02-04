package software.wings.sm.states;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.base.Preconditions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.instana.InstanaDataCollectionInfo;
import software.wings.service.impl.instana.InstanaMetricTemplate;
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
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
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
    metricAnalysisService.saveMetricTemplates(
        context.getAppId(), StateType.INSTANA, context.getStateExecutionInstanceId(), null, createMetricTemplates());
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
        .tagFilters(tagFilters)
        .hostTagFilter(hostTagFilter)
        .build();
  }

  public Map<String, TimeSeriesMetricDefinition> createMetricTemplates() {
    Map<String, TimeSeriesMetricDefinition> rv = new HashMap<>();
    Map<String, InstanaMetricTemplate> infraMetricTemplateMap = InstanaUtils.getInfraMetricTemplateMap();
    metrics.forEach(metric -> {
      InstanaMetricTemplate instanaMetricTemplate = infraMetricTemplateMap.get(metric);
      Preconditions.checkNotNull(instanaMetricTemplate, "instanaMetricTemplate can not be null");
      rv.put(instanaMetricTemplate.getDisplayName(),
          TimeSeriesMetricDefinition.builder()
              .metricName(instanaMetricTemplate.getDisplayName())
              .metricType(instanaMetricTemplate.getMetricType())
              .build());
    });
    InstanaUtils.getApplicationMetricTemplateMap().forEach(
        (metricName, instanaMetricTemplate)
            -> rv.put(instanaMetricTemplate.getDisplayName(),
                TimeSeriesMetricDefinition.builder()
                    .metricName(instanaMetricTemplate.getDisplayName())
                    .metricType(instanaMetricTemplate.getMetricType())
                    .build()));
    return rv;
  }
  @Override
  protected boolean isCVTaskEnqueuingEnabled(String accountId) {
    return true;
  }
}
