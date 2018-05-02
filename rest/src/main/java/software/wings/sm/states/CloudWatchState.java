package software.wings.sm.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.sm.states.DynatraceState.CONTROL_HOST_NAME;
import static software.wings.sm.states.DynatraceState.TEST_HOST_NAME;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.time.Timestamp;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PhaseElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping.AwsRegionDataProvider;
import software.wings.beans.DelegateTask;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsSettingProvider;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchDataCollectionInfo;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 12/7/16.
 */
public class CloudWatchState extends AbstractMetricAnalysisState {
  @Transient @SchemaIgnore private static final Logger logger = LoggerFactory.getLogger(CloudWatchState.class);

  @Transient @Inject private SettingsService settingsService;
  @Transient @Inject private AwsHelperService awsHelperService;
  @Transient @Inject private SecretManager secretManager;
  @Transient @Inject private CloudWatchService cloudWatchService;

  @EnumData(enumDataProvider = AwsSettingProvider.class)
  @Attributes(required = true, title = "AWS account")
  private String analysisServerConfigId;

  @Attributes(title = "Region")
  @DefaultValue("us-east-1")
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  private String region = "us-east-1";

  @Attributes(title = "Load Balancer") private String loadBalancerName;

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public CloudWatchState(String name) {
    super(name, StateType.CLOUD_WATCH);
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
  @Attributes(required = true, title = "Failure Criteria")
  @DefaultValue("LOW")
  public AnalysisTolerance getAnalysisTolerance() {
    if (isBlank(tolerance)) {
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

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getLoadBalancerName() {
    return loadBalancerName;
  }

  public void setLoadBalancerName(String loadBalancerName) {
    this.loadBalancerName = loadBalancerName;
  }

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, String correlationId, Set<String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);

    if (settingAttribute == null) {
      throw new WingsException("No aws config with id: " + analysisServerConfigId + " found");
    }

    final AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    final long dataCollectionStartTimeStamp = Timestamp.minuteBoundary(System.currentTimeMillis());
    Map<AwsNameSpace, List<CloudWatchMetric>> cloudWatchMetrics = cloudWatchService.getCloudWatchMetrics();

    metricAnalysisService.saveMetricTemplates(
        StateType.CLOUD_WATCH, context.getStateExecutionInstanceId(), getMetricTemplates(cloudWatchMetrics));
    final CloudWatchDataCollectionInfo dataCollectionInfo =
        CloudWatchDataCollectionInfo.builder()
            .awsConfig(awsConfig)
            .applicationId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowId(getWorkflowId(context))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .serviceId(getPhaseServiceId(context))
            .analysisComparisonStrategy(getComparisonStrategy())
            .startTime(dataCollectionStartTimeStamp)
            .collectionTime(Integer.parseInt(timeDuration))
            .dataCollectionMinute(0)
            .encryptedDataDetails(
                secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId()))
            .hosts(hosts)
            .loadBalancers(Sets.newHashSet(loadBalancerName))
            .cloudWatchMetrics(cloudWatchMetrics)
            .build();

    String waitId = generateUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.CLOUD_WATCH_COLLECT_METRIC_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .withInfrastructureMappingId(infrastructureMappingId)
                                    .withTimeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration) + 120))
                                    .build();
    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), correlationId, false), waitId);
    String taskId = delegateService.queueTask(delegateTask);

    if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
      analysisContext.getControlNodes().add(CONTROL_HOST_NAME);
      analysisContext.getTestNodes().add(TEST_HOST_NAME);
    }

    return taskId;
  }

  private Map<String, TimeSeriesMetricDefinition> getMetricTemplates(
      Map<AwsNameSpace, List<CloudWatchMetric>> timeSeriesToCollect) {
    Map<String, TimeSeriesMetricDefinition> rv = new HashMap<>();
    for (Entry<AwsNameSpace, List<CloudWatchMetric>> entry : timeSeriesToCollect.entrySet()) {
      for (CloudWatchMetric timeSeries : entry.getValue()) {
        rv.put(timeSeries.getMetricName(),
            TimeSeriesMetricDefinition.builder()
                .metricName(timeSeries.getMetricName())
                .metricType(timeSeries.getMetricType())
                .build());
      }
    }
    return rv;
  }
}
