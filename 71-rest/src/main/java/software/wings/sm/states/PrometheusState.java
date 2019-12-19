package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.resources.PrometheusResource.renderFetchQueries;
import static software.wings.service.impl.analysis.TimeSeriesMlAnalysisType.PREDICTIVE;

import com.google.common.base.Preconditions;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DelegateTask;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.resources.PrometheusResource;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.prometheus.PrometheusDataCollectionInfo;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 2/6/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class PrometheusState extends AbstractMetricAnalysisState {
  @Transient @SchemaIgnore public static final String TEST_HOST_NAME = "testNode";
  @Transient @SchemaIgnore public static final String CONTROL_HOST_NAME = "controlNode";

  @Attributes(required = true, title = "Prometheus Server") private String analysisServerConfigId;

  private List<TimeSeries> timeSeriesToAnalyze;

  public PrometheusState(String name) {
    super(name, StateType.PROMETHEUS);
  }

  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Attributes(title = "Analysis Time duration (in minutes)", description = "Default 15 minutes")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @Attributes(required = true, title = "Algorithm Sensitivity")
  @DefaultValue("MEDIUM")
  public AnalysisTolerance getAnalysisTolerance() {
    if (isBlank(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
  }

  @Attributes(required = true, title = "Include nodes from previous phases")
  public boolean getIncludePreviousPhaseNodes() {
    return includePreviousPhaseNodes;
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, AnalysisContext analysisContext,
      VerificationStateAnalysisExecutionData executionData, Map<String, String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    Preconditions.checkNotNull(settingAttribute, "No prometheus setting with id: " + analysisServerConfigId + " found");
    TimeSeriesMlAnalysisType analyzedTierAnalysisType = getComparisonStrategy() == AnalysisComparisonStrategy.PREDICTIVE
        ? PREDICTIVE
        : TimeSeriesMlAnalysisType.COMPARATIVE;

    final PrometheusConfig prometheusConfig = (PrometheusConfig) settingAttribute.getValue();

    metricAnalysisService.saveMetricTemplates(context.getAppId(), StateType.PROMETHEUS,
        context.getStateExecutionInstanceId(), null, createMetricTemplates(timeSeriesToAnalyze));

    renderURLExpressions(context, timeSeriesToAnalyze);
    final long dataCollectionStartTimeStamp = dataCollectionStartTimestampMillis();
    final PrometheusDataCollectionInfo dataCollectionInfo =
        PrometheusDataCollectionInfo.builder()
            .prometheusConfig(prometheusConfig)
            .applicationId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowId(context.getWorkflowId())
            .workflowExecutionId(context.getWorkflowExecutionId())
            .serviceId(getPhaseServiceId(context))
            .startTime(dataCollectionStartTimeStamp)
            .collectionTime(Integer.parseInt(getTimeDuration()))
            .timeSeriesToCollect(renderFetchQueries(timeSeriesToAnalyze))
            .dataCollectionMinute(0)
            .hosts(hosts)
            .timeSeriesMlAnalysisType(analyzedTierAnalysisType)
            .build();

    String waitId = generateUuid();
    String infrastructureMappingId = context.fetchInfraMappingId();
    DelegateTask delegateTask =
        DelegateTask.builder()
            .async(true)
            .accountId(appService.get(context.getAppId()).getAccountId())
            .appId(context.getAppId())
            .waitId(waitId)
            .data(TaskData.builder()
                      .taskType(TaskType.PROMETHEUS_METRIC_DATA_COLLECTION_TASK.name())
                      .parameters(new Object[] {dataCollectionInfo})
                      .timeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration()) + 5))
                      .build())
            .envId(envId)
            .infrastructureMappingId(infrastructureMappingId)
            .build();
    waitNotifyEngine.waitForAllOn(ORCHESTRATION,
        DataCollectionCallback.builder()
            .appId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .executionData(executionData)
            .dataCollectionStartTime(dataCollectionStartTimeStamp)
            .dataCollectionEndTime(
                dataCollectionStartTimeStamp + TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration())))
            .build(),
        waitId);
    return delegateService.queueTask(delegateTask);
  }

  @Override
  public Map<String, String> validateFields() {
    return PrometheusResource.validateTransactions(timeSeriesToAnalyze, false);
  }

  private void renderURLExpressions(ExecutionContext executionContext, List<TimeSeries> timeSeriesToAnalyze) {
    timeSeriesToAnalyze.forEach(
        timeSeries -> timeSeries.setUrl(executionContext.renderExpression(timeSeries.getUrl())));
  }

  public static Map<String, TimeSeriesMetricDefinition> createMetricTemplates(List<TimeSeries> timeSeriesToAnalyze) {
    Map<String, TimeSeriesMetricDefinition> rv = new HashMap<>();
    timeSeriesToAnalyze.forEach(timeSeries
        -> rv.put(timeSeries.getMetricName(),
            TimeSeriesMetricDefinition.builder()
                .metricName(timeSeries.getMetricName())
                .metricType(MetricType.valueOf(timeSeries.getMetricType()))
                .build()));
    return rv;
  }

  @Override
  public Logger getLogger() {
    return logger;
  }
}
