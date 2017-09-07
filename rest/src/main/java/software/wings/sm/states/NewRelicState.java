package software.wings.sm.states;

import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.api.PhaseElement;
import software.wings.beans.DelegateTask;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.metrics.MetricDefinition.Threshold;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicExecutionData;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysis;
import software.wings.service.impl.newrelic.NewRelicMetricAnalysisRecord.NewRelicMetricAnalysisValue;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.impl.newrelic.NewRelicSettingProvider;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.time.WingsTimeUtils;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by rsingh on 8/28/17.
 */
public class NewRelicState extends AbstractAnalysisState {
  @Transient @SchemaIgnore private static final Logger logger = LoggerFactory.getLogger(NewRelicState.class);

  @Transient @SchemaIgnore private ScheduledExecutorService analysisExecutorService;

  @Transient @Inject private NewRelicService newRelicService;

  @EnumData(enumDataProvider = NewRelicSettingProvider.class)
  @Attributes(required = true, title = "New Relic Server")
  private String analysisServerConfigId;

  @Attributes(required = true, title = "Application Name") private String applicationId;

  public NewRelicState(String name) {
    super(name, StateType.NEW_RELIC.name());
  }

  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (StringUtils.isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Attributes(title = "Analysis Time duration (in minutes)", description = "Default 15 minutes")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (StringUtils.isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @Override
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

  public String getApplicationId() {
    return applicationId;
  }

  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  @Override
  protected void triggerAnalysisDataCollection(ExecutionContext context, Set<String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No splunk setting with id: " + analysisServerConfigId + " found");
    }

    final NewRelicConfig newRelicConfig = (NewRelicConfig) settingAttribute.getValue();

    final long dataCollectionStartTimeStamp = WingsTimeUtils.getMinuteBoundary(System.currentTimeMillis());
    final NewRelicDataCollectionInfo dataCollectionInfo = NewRelicDataCollectionInfo.builder()
                                                              .newRelicConfig(newRelicConfig)
                                                              .applicationId(context.getAppId())
                                                              .stateExecutionId(context.getStateExecutionInstanceId())
                                                              .workflowId(getWorkflowId(context))
                                                              .workflowExecutionId(context.getWorkflowExecutionId())
                                                              .serviceId(getPhaseServiceId(context))
                                                              .startTime(dataCollectionStartTimeStamp)
                                                              .collectionTime(Integer.parseInt(timeDuration))
                                                              .newRelicAppId(Long.parseLong(applicationId))
                                                              .build();

    String waitId = UUIDGenerator.getUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.NEWRELIC_COLLECT_METRIC_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .withInfrastructureMappingId(infrastructureMappingId)
                                    .build();
    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId()), waitId);
    delegateService.queueTask(delegateTask);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    logger.debug("Executing new relic state");
    Set<String> canaryNewHostNames = getCanaryNewHostNames(context);
    if (canaryNewHostNames == null || canaryNewHostNames.isEmpty()) {
      getLogger().error("Could not find test nodes to compare the data");
      return generateAnalysisResponse(context, ExecutionStatus.FAILED, "Could not find test nodes to compare the data");
    }

    Set<String> lastExecutionNodes = getLastExecutionNodes(context);
    if (lastExecutionNodes == null || lastExecutionNodes.isEmpty()) {
      if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
        getLogger().error("No nodes with older version found to compare the logs. Skipping analysis");
        return generateAnalysisResponse(context, ExecutionStatus.SUCCESS,
            "Skipping analysis due to lack of baseline data (First time deployment).");
      }

      getLogger().warn(
          "It seems that there is no successful run for this workflow yet. Log data will be collected to be analyzed for next deployment run");
    }

    if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
        && lastExecutionNodes.equals(canaryNewHostNames)) {
      getLogger().error("Control and test nodes are same. Will not be running Log analysis");
      return generateAnalysisResponse(context, ExecutionStatus.FAILED,
          "Skipping analysis due to lack of baseline data (Minimum two phases are required).");
    }

    triggerAnalysisDataCollection(context, null);
    final NewRelicExecutionData executionData = NewRelicExecutionData.Builder.anAnanlysisExecutionData()
                                                    .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                                    .withServerConfigID(getAnalysisServerConfigId())
                                                    .withAnalysisDuration(Integer.parseInt(timeDuration))
                                                    .withStatus(ExecutionStatus.RUNNING)
                                                    .withCanaryNewHostNames(canaryNewHostNames)
                                                    .withCorrelationId(UUID.randomUUID().toString())
                                                    .build();
    final MetricDataAnalysisResponse response =
        MetricDataAnalysisResponse.builder().stateExecutionData(executionData).build();
    response.setExecutionStatus(ExecutionStatus.SUCCESS);
    analysisExecutorService = createExecutorService(context, response, lastExecutionNodes, canaryNewHostNames);
    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(executionData.getCorrelationId()))
        .withExecutionStatus(ExecutionStatus.RUNNING)
        .withErrorMessage("New Relic Verification running")
        .withStateExecutionData(executionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    MetricDataAnalysisResponse executionResponse = (MetricDataAnalysisResponse) response.values().iterator().next();
    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(executionResponse.getStateExecutionData())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private void shutDownGenerator(MetricDataAnalysisResponse response) {
    waitNotifyEngine.notify(((NewRelicExecutionData) response.getStateExecutionData()).getCorrelationId(), response);
    analysisExecutorService.shutdown();
  }

  private ScheduledExecutorService createExecutorService(
      ExecutionContext context, MetricDataAnalysisResponse response, Set<String> controlNodes, Set<String> testNodes) {
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.scheduleAtFixedRate(
        new NewRelicAnalysisGenerator(context, response, controlNodes, testNodes), 1, 1, TimeUnit.MINUTES);
    return scheduledExecutorService;
  }

  protected ExecutionResponse generateAnalysisResponse(
      ExecutionContext context, ExecutionStatus status, String message) {
    final NewRelicExecutionData executionData = NewRelicExecutionData.Builder.anAnanlysisExecutionData()
                                                    .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                                    .withServerConfigID(getAnalysisServerConfigId())
                                                    .withAnalysisDuration(Integer.parseInt(timeDuration))
                                                    .withStatus(ExecutionStatus.RUNNING)
                                                    .withCorrelationId(UUID.randomUUID().toString())
                                                    .build();
    NewRelicMetricAnalysisRecord metricAnalysisRecord = NewRelicMetricAnalysisRecord.builder()
                                                            .message(message)
                                                            .stateExecutionId(context.getStateExecutionInstanceId())
                                                            .workflowExecutionId(context.getWorkflowExecutionId())
                                                            .workflowId(getWorkflowId(context))
                                                            .build();
    newRelicService.saveAnalysisRecords(metricAnalysisRecord);

    return anExecutionResponse()
        .withAsync(false)
        .withExecutionStatus(status)
        .withStateExecutionData(executionData)
        .withErrorMessage(message)
        .build();
  }

  private class NewRelicAnalysisGenerator implements Runnable {
    private final ExecutionContext context;
    private final MetricDataAnalysisResponse response;
    private final Set<String> controlNodes;
    private final Set<String> testNodes;
    private final String serviceId;
    private int analysisMinute = 0;

    private NewRelicAnalysisGenerator(ExecutionContext context, MetricDataAnalysisResponse response,
        Set<String> controlNodes, Set<String> testNodes) {
      this.context = context;
      this.response = response;
      this.controlNodes = controlNodes;
      this.testNodes = testNodes;
      this.serviceId = getPhaseServiceId(context);
    }

    @Override
    public void run() {
      if (analysisMinute > Integer.parseInt(timeDuration)) {
        shutDownGenerator(response);
        return;
      }

      final List<NewRelicMetricDataRecord> controlRecords =
          getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS
          ? newRelicService.getPreviousSuccessfulRecords(getWorkflowId(context), serviceId, analysisMinute)
          : newRelicService.getRecords(context.getWorkflowExecutionId(), context.getStateExecutionInstanceId(),
                getWorkflowId(context), serviceId, controlNodes, analysisMinute);

      final List<NewRelicMetricDataRecord> testRecords = newRelicService.getRecords(context.getWorkflowExecutionId(),
          context.getStateExecutionInstanceId(), getWorkflowId(context), serviceId, testNodes, analysisMinute);

      Map<String, List<NewRelicMetricDataRecord>> controlRecordsByMetric = splitMetricsByName(controlRecords);
      Map<String, List<NewRelicMetricDataRecord>> testRecordsByMetric = splitMetricsByName(testRecords);

      NewRelicMetricAnalysisRecord analysisRecord = NewRelicMetricAnalysisRecord.builder()
                                                        .stateExecutionId(context.getStateExecutionInstanceId())
                                                        .workflowExecutionId(context.getWorkflowExecutionId())
                                                        .workflowId(getWorkflowId(context))
                                                        .metricAnalyses(new ArrayList<>())
                                                        .build();

      for (Entry<String, List<NewRelicMetricDataRecord>> metric : testRecordsByMetric.entrySet()) {
        final String metricName = metric.getKey();
        NewRelicMetricAnalysis metricAnalysis = NewRelicMetricAnalysis.builder()
                                                    .metricName(metricName)
                                                    .riskLevel(RiskLevel.LOW)
                                                    .metricValues(new ArrayList<>())
                                                    .build();

        for (Entry<String, List<Threshold>> valuesToAnalyze :
            NewRelicMetricValueDefinition.VALUES_TO_ANALYZE.entrySet()) {
          NewRelicMetricValueDefinition metricValueDefinition = NewRelicMetricValueDefinition.builder()
                                                                    .metricName(metricName)
                                                                    .metricValueName(valuesToAnalyze.getKey())
                                                                    .thresholds(valuesToAnalyze.getValue())
                                                                    .build();

          NewRelicMetricAnalysisValue metricAnalysisValue =
              metricValueDefinition.analyze(metric.getValue(), controlRecordsByMetric.get(metricName));
          metricAnalysis.addNewRelicMetricAnalysisValue(metricAnalysisValue);

          if (metricAnalysisValue.getRiskLevel().compareTo(metricAnalysis.getRiskLevel()) < 0) {
            metricAnalysis.setRiskLevel(metricAnalysisValue.getRiskLevel());
          }
        }
        analysisRecord.addNewRelicMetricAnalysis(metricAnalysis);
      }

      newRelicService.saveAnalysisRecords(analysisRecord);
      analysisMinute++;
    }

    private Map<String, List<NewRelicMetricDataRecord>> splitMetricsByName(List<NewRelicMetricDataRecord> records) {
      final Map<String, List<NewRelicMetricDataRecord>> rv = new HashMap<>();
      for (NewRelicMetricDataRecord record : records) {
        if (!rv.containsKey(record.getName())) {
          rv.put(record.getName(), new ArrayList<>());
        }

        rv.get(record.getName()).add(record);
      }

      return rv;
    }
  }
}
