package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static software.wings.service.impl.analysis.TimeSeriesMlAnalysisType.PREDICTIVE;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DelegateTask;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.api.PhaseElement;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.stackdriver.StackDriverDataCollectionInfo;
import software.wings.service.impl.stackdriver.StackDriverMetric;
import software.wings.service.intfc.stackdriver.StackDriverService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.verification.VerificationStateAnalysisExecutionData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * Created by Pranjal on 11/28/2018
 */
@Slf4j
public class StackDriverState extends AbstractMetricAnalysisState {
  @Inject private transient StackDriverService stackDriverService;

  @Attributes(required = true, title = "GCP account") private String analysisServerConfigId;

  @Attributes(title = "Region") @DefaultValue("us-east-1") private String region = "us-east-1";

  private Map<String, List<StackDriverMetric>> loadBalancerMetrics;

  private List<StackDriverMetric> podMetrics;

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public Map<String, List<StackDriverMetric>> fetchLoadBalancerMetrics() {
    return loadBalancerMetrics;
  }

  public void setLoadBalancerMetrics(Map<String, List<StackDriverMetric>> loadBalancerMetrics) {
    this.loadBalancerMetrics = loadBalancerMetrics;
  }

  public List<StackDriverMetric> fetchPodMetrics() {
    return podMetrics;
  }

  public void setPodMetrics(List<StackDriverMetric> podMetrics) {
    this.podMetrics = podMetrics;
  }

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public StackDriverState(String name) {
    super(name, StateType.STACK_DRIVER);
  }

  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (isEmpty(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Attributes(title = "Analysis Time duration (in minutes)", description = "Default 15 minutes")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (isEmpty(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @Attributes(required = true, title = "Algorithm Sensitivity")
  @DefaultValue("MEDIUM")
  public AnalysisTolerance getAnalysisTolerance() {
    if (isEmpty(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
  }

  @Override
  @SchemaIgnore
  public Logger getLogger() {
    return logger;
  }

  @Override
  public String getAnalysisServerConfigId() {
    return analysisServerConfigId;
  }

  @Override
  public void setAnalysisServerConfigId(String analysisServerConfigId) {
    this.analysisServerConfigId = analysisServerConfigId;
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, AnalysisContext analysisContext,
      VerificationStateAnalysisExecutionData executionData, Map<String, String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);

    if (settingAttribute == null) {
      throw new WingsException("No gcp config with id: " + analysisServerConfigId + " found");
    }

    TimeSeriesMlAnalysisType analyzedTierAnalysisType = getComparisonStrategy() == AnalysisComparisonStrategy.PREDICTIVE
        ? PREDICTIVE
        : TimeSeriesMlAnalysisType.COMPARATIVE;

    final GcpConfig gcpConfig = (GcpConfig) settingAttribute.getValue();

    // StartTime will be current time in milliseconds
    final long dataCollectionStartTimeStamp = Timestamp.minuteBoundary(System.currentTimeMillis());
    Map<String, List<StackDriverMetric>> stackDriverMetrics = stackDriverService.getMetrics();

    metricAnalysisService.saveMetricTemplates(context.getAppId(), StateType.STACK_DRIVER,
        context.getStateExecutionInstanceId(), null, fetchMetricTemplates(stackDriverMetrics));

    final StackDriverDataCollectionInfo dataCollectionInfo =
        StackDriverDataCollectionInfo.builder()
            .gcpConfig(gcpConfig)
            .appId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowId(getWorkflowId(context))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .serviceId(getPhaseServiceId(context))
            .timeSeriesMlAnalysisType(analyzedTierAnalysisType)
            .startTime(dataCollectionStartTimeStamp)
            // Collection time is amount of time data collection needs to happen
            .collectionTime(Integer.parseInt(timeDuration))
            // its a counter for each minute data. So basically the max value of
            // dataCollectionMinute can be equal to timeDuration
            .dataCollectionMinute(0)
            .encryptedDataDetails(
                secretManager.getEncryptionDetails(gcpConfig, context.getAppId(), context.getWorkflowExecutionId()))
            .hosts(hosts)
            .loadBalancerMetrics(loadBalancerMetrics)
            .podMetrics(podMetrics)
            .build();

    String waitId = generateUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask = DelegateTask.builder()
                                    .async(true)
                                    .accountId(appService.get(context.getAppId()).getAccountId())
                                    .appId(context.getAppId())
                                    .waitId(waitId)
                                    .data(TaskData.builder()
                                              .taskType(TaskType.STACKDRIVER_COLLECT_METRIC_DATA.name())
                                              .parameters(new Object[] {dataCollectionInfo})
                                              .timeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 120))
                                              .build())
                                    .envId(envId)
                                    .infrastructureMappingId(infrastructureMappingId)
                                    .build();
    waitNotifyEngine.waitForAll(DataCollectionCallback.builder()
                                    .appId(context.getAppId())
                                    .executionData(executionData)
                                    .isLogCollection(false)
                                    .build(),
        waitId);
    return delegateService.queueTask(delegateTask);
  }

  public static Map<String, TimeSeriesMetricDefinition> fetchMetricTemplates(
      Map<String, List<StackDriverMetric>> timeSeriesToCollect) {
    Map<String, TimeSeriesMetricDefinition> rv = new HashMap<>();

    for (Entry<String, List<StackDriverMetric>> entry : timeSeriesToCollect.entrySet()) {
      for (StackDriverMetric stackDriverMetric : entry.getValue()) {
        rv.put(stackDriverMetric.getMetric(),
            TimeSeriesMetricDefinition.builder()
                .metricName(stackDriverMetric.getMetric())
                .metricType(MetricType.valueOf(stackDriverMetric.getKind()))
                .build());
      }
    }
    return rv;
  }

  @Attributes(required = false, title = "Expression for Host/Container name")
  public String getHostnameTemplate() {
    return hostnameTemplate;
  }

  public void setHostnameTemplate(String hostnameTemplate) {
    this.hostnameTemplate = hostnameTemplate;
  }
}
