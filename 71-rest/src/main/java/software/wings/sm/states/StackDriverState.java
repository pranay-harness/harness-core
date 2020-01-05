package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.waiter.OrchestrationNotifyEventListener.ORCHESTRATION;
import static software.wings.common.VerificationConstants.DELAY_MINUTES;
import static software.wings.service.impl.analysis.TimeSeriesMlAnalysisType.PREDICTIVE;

import com.google.common.collect.TreeBasedTable;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.beans.DelegateTask;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
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
import software.wings.verification.stackdriver.StackDriverMetricCVConfiguration;
import software.wings.verification.stackdriver.StackDriverMetricDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
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

  private boolean isLogState;

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

  @Override
  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (isEmpty(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Override
  @Attributes(title = "Analysis Time duration (in minutes)", description = "Default 15 minutes")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (isEmpty(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @Override
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

  public void saveMetricTemplates(ExecutionContext context) {
    Map<String, List<StackDriverMetric>> stackDriverMetrics = stackDriverService.getMetrics();

    metricAnalysisService.saveMetricTemplates(context.getAppId(), StateType.STACK_DRIVER,
        context.getStateExecutionInstanceId(), null, fetchMetricTemplates(stackDriverMetrics));
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, AnalysisContext analysisContext,
      VerificationStateAnalysisExecutionData executionData, Map<String, String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null || workflowStandardParams.getEnv() == null
        ? null
        : workflowStandardParams.getEnv().getUuid();

    SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);

    if (settingAttribute == null) {
      throw new WingsException("No gcp config with id: " + analysisServerConfigId + " found");
    }

    TimeSeriesMlAnalysisType analyzedTierAnalysisType = getComparisonStrategy() == AnalysisComparisonStrategy.PREDICTIVE
        ? PREDICTIVE
        : TimeSeriesMlAnalysisType.COMPARATIVE;

    final GcpConfig gcpConfig = (GcpConfig) settingAttribute.getValue();

    // StartTime will be current time in milliseconds
    final long dataCollectionStartTimeStamp = dataCollectionStartTimestampMillis();
    Map<String, List<StackDriverMetric>> stackDriverMetrics = stackDriverService.getMetrics();

    metricAnalysisService.saveMetricTemplates(context.getAppId(), StateType.STACK_DRIVER,
        context.getStateExecutionInstanceId(), null, fetchMetricTemplates(stackDriverMetrics));

    final StackDriverDataCollectionInfo dataCollectionInfo =
        StackDriverDataCollectionInfo.builder()
            .gcpConfig(gcpConfig)
            .applicationId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowId(getWorkflowId(context))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .serviceId(getPhaseServiceId(context))
            .timeSeriesMlAnalysisType(analyzedTierAnalysisType)
            .startMinute((int) (dataCollectionStartTimeStamp / TimeUnit.MINUTES.toMillis(1)))
            .startTime(dataCollectionStartTimeStamp)
            // Collection time is amount of time data collection needs to happen
            .collectionTime(Integer.parseInt(timeDuration))
            .initialDelayMinutes(DELAY_MINUTES)
            // its a counter for each minute data. So basically the max value of
            // dataCollectionMinute can be equal to timeDuration
            //            .dataCollectionMinute(0)
            .encryptedDataDetails(
                secretManager.getEncryptionDetails(gcpConfig, context.getAppId(), context.getWorkflowExecutionId()))
            .hosts(hosts)
            .loadBalancerMetrics(loadBalancerMetrics)
            .podMetrics(podMetrics)
            .build();

    String waitId = generateUuid();
    String infrastructureMappingId = context.fetchInfraMappingId();
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
    waitNotifyEngine.waitForAllOn(ORCHESTRATION,
        DataCollectionCallback.builder()
            .appId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .dataCollectionStartTime(dataCollectionStartTimeStamp)
            .dataCollectionEndTime(
                dataCollectionStartTimeStamp + TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration())))
            .executionData(executionData)
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

  @Override
  @Attributes(required = false, title = "Expression for Host/Container name")
  public String getHostnameTemplate() {
    return hostnameTemplate;
  }

  @Override
  public void setHostnameTemplate(String hostnameTemplate) {
    this.hostnameTemplate = hostnameTemplate;
  }

  public boolean isLogState() {
    return isLogState;
  }

  public void setLogState(boolean logState) {
    isLogState = logState;
  }

  public static String getMetricTypeForMetric(StackDriverMetricCVConfiguration cvConfiguration, String metricName) {
    if (cvConfiguration != null && isNotEmpty(metricName)) {
      return cvConfiguration.getMetricDefinitions()
          .stream()
          .filter(timeSeries -> timeSeries.getMetricName().equals(metricName))
          .findAny()
          .map(timeSeries -> timeSeries.getMetricType())
          .orElse(null);
    }
    return null;
  }

  public static Map<String, String> validateMetricDefinitions(
      List<StackDriverMetricDefinition> metricDefinitions, boolean serviceLevel) {
    Map<String, String> invalidFields = new HashMap<>();
    if (isEmpty(metricDefinitions)) {
      invalidFields.put("metricDefinitions", "No metrics given to analyze.");
      return invalidFields;
    }
    Map<String, String> metricNameToType = new HashMap<>();
    final TreeBasedTable<String, MetricType, Set<String>> txnToMetricType = TreeBasedTable.create();

    metricDefinitions.forEach(timeSeries -> {
      MetricType metricType = MetricType.valueOf(timeSeries.getMetricType());
      final String filter = timeSeries.getFilter();
      if (isEmpty(filter)) {
        invalidFields.put("No Filter JSON specified for ",
            "Group: " + timeSeries.getTxnName() + " Metric: " + timeSeries.getMetricName());
      }

      // TODO: When we have filterJSON for workflow, add logic here for checking host field in query json

      if (metricNameToType.get(timeSeries.getMetricName()) == null) {
        metricNameToType.put(timeSeries.getMetricName(), timeSeries.getMetricType());
      } else if (!metricNameToType.get(timeSeries.getMetricName()).equals(timeSeries.getMetricType())) {
        invalidFields.put(
            "Invalid metric type for group: " + timeSeries.getTxnName() + ", metric : " + timeSeries.getMetricName(),
            timeSeries.getMetricName() + " has been configured as " + metricNameToType.get(timeSeries.getMetricName())
                + " in previous transactions. Same metric name can not have different metric types.");
      }

      if (!txnToMetricType.contains(timeSeries.getTxnName(), metricType)) {
        txnToMetricType.put(timeSeries.getTxnName(), metricType, new HashSet<>());
      }

      txnToMetricType.get(timeSeries.getTxnName(), metricType).add(timeSeries.getMetricName());
    });

    txnToMetricType.rowKeySet().forEach(txnName -> {
      final SortedMap<MetricType, Set<String>> txnRow = txnToMetricType.row(txnName);
      if (txnRow.containsKey(MetricType.ERROR) || txnRow.containsKey(MetricType.RESP_TIME)) {
        if (!txnRow.containsKey(MetricType.THROUGHPUT)) {
          invalidFields.put("Invalid metrics for group: " + txnName,
              txnName + " has error metrics "
                  + (txnRow.get(MetricType.ERROR) == null ? Collections.emptySet() : txnRow.get(MetricType.ERROR))
                  + " and/or response time metrics "
                  + (txnRow.get(MetricType.RESP_TIME) == null ? Collections.emptySet()
                                                              : txnRow.get(MetricType.RESP_TIME))
                  + " but no throughput metrics.");
        } else if (txnRow.get(MetricType.THROUGHPUT).size() > 1) {
          invalidFields.put("Invalid metrics for group: " + txnName,
              txnName + " has more than one throughput metrics " + txnRow.get(MetricType.THROUGHPUT) + " defined.");
        }
      }

      if (txnRow.containsKey(MetricType.THROUGHPUT) && txnRow.size() == 1) {
        invalidFields.put("Invalid metrics for group: " + txnName,
            txnName + " has only throughput metrics " + txnRow.get(MetricType.THROUGHPUT)
                + ". Throughput metrics is used to analyze other metrics and is not analyzed.");
      }
    });

    return invalidFields;
  }
}
