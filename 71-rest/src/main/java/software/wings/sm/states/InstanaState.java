package software.wings.sm.states;

import static org.apache.commons.lang3.StringUtils.isBlank;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.instana.InstanaDataCollectionInfo;
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
  private String analysisServerConfigId;
  private List<String> metrics;
  private String query;

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
        .build();
  }

  public Map<String, TimeSeriesMetricDefinition> createMetricTemplates() {
    Map<String, TimeSeriesMetricDefinition> rv = new HashMap<>();
    metrics.forEach(metric -> {
      // TODO: Mongo can not save dots in the key. we need to  Find a better way to handle this.
      metric = metric.replace(".", "_");
      rv.put(metric, TimeSeriesMetricDefinition.builder().metricName(metric).metricType(MetricType.INFRA).build());
    });
    return rv;
  }
  @Override
  protected boolean isCVTaskEnqueuingEnabled(String accountId) {
    return true;
  }
}
