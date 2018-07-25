package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import com.google.common.base.Preconditions;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.time.Timestamp;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.beans.Account;
import software.wings.beans.DelegateTask;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.resources.PrometheusResource;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.TimeSeries;
import software.wings.service.impl.prometheus.PrometheusDataCollectionInfo;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 2/6/18.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PrometheusState extends AbstractMetricAnalysisState {
  @Transient @SchemaIgnore private static final Logger logger = LoggerFactory.getLogger(PrometheusState.class);
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

  @Override
  protected String triggerAnalysisDataCollection(
      ExecutionContext context, String correlationId, Map<String, String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    Preconditions.checkNotNull(settingAttribute, "No new relic setting with id: " + analysisServerConfigId + " found");

    final PrometheusConfig prometheusConfig = (PrometheusConfig) settingAttribute.getValue();

    metricAnalysisService.saveMetricTemplates(
        context.getAppId(), StateType.PROMETHEUS, context.getStateExecutionInstanceId(), getMetricTemplates());
    final long dataCollectionStartTimeStamp = Timestamp.minuteBoundary(System.currentTimeMillis());
    final PrometheusDataCollectionInfo dataCollectionInfo = PrometheusDataCollectionInfo.builder()
                                                                .prometheusConfig(prometheusConfig)
                                                                .applicationId(context.getAppId())
                                                                .stateExecutionId(context.getStateExecutionInstanceId())
                                                                .workflowId(context.getWorkflowId())
                                                                .workflowExecutionId(context.getWorkflowExecutionId())
                                                                .serviceId(getPhaseServiceId(context))
                                                                .startTime(dataCollectionStartTimeStamp)
                                                                .collectionTime(Integer.parseInt(timeDuration))
                                                                .timeSeriesToCollect(timeSeriesToAnalyze)
                                                                .dataCollectionMinute(0)
                                                                .hosts(hosts)
                                                                .analysisComparisonStrategy(getComparisonStrategy())
                                                                .build();

    String waitId = generateUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.PROMETHEUS_METRIC_DATA_COLLECTION_TASK)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .withInfrastructureMappingId(infrastructureMappingId)
                                    .withTimeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 5))
                                    .build();
    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), correlationId, false), waitId);
    return delegateService.queueTask(delegateTask);
  }

  @Override
  public Map<String, String> validateFields() {
    return PrometheusResource.validateTransactions(Account.GLOBAL_ACCOUNT_ID, timeSeriesToAnalyze);
  }

  private Map<String, TimeSeriesMetricDefinition> getMetricTemplates() {
    Map<String, TimeSeriesMetricDefinition> rv = new HashMap<>();
    timeSeriesToAnalyze.forEach(timeSeries
        -> rv.put(timeSeries.getMetricName(),
            TimeSeriesMetricDefinition.builder()
                .metricName(timeSeries.getMetricName())
                .metricType(timeSeries.getMetricType())
                .build()));
    return rv;
  }

  @Override
  public Logger getLogger() {
    return logger;
  }
}
