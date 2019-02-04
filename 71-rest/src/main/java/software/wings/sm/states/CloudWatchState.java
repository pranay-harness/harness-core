package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Collections.singletonList;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.states.DynatraceState.CONTROL_HOST_NAME;
import static software.wings.sm.states.DynatraceState.TEST_HOST_NAME;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import lombok.Builder;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.AwsLambdaContextElement;
import software.wings.api.ContainerServiceElement;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseElement;
import software.wings.beans.AwsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.common.Constants;
import software.wings.metrics.MetricType;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.cloudwatch.AwsNameSpace;
import software.wings.service.impl.cloudwatch.CloudWatchDataCollectionInfo;
import software.wings.service.impl.cloudwatch.CloudWatchMetric;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.intfc.CloudWatchService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.ArrayList;
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

  @Inject private transient AwsHelperService awsHelperService;
  @Inject private transient CloudWatchService cloudWatchService;

  @Attributes(required = true, title = "AWS account") private String analysisServerConfigId;

  @Attributes(title = "Region") @DefaultValue("us-east-1") private String region = "us-east-1";

  @SchemaIgnore @Builder.Default private Map<String, List<CloudWatchMetric>> loadBalancerMetrics = new HashMap<>();

  @SchemaIgnore @Builder.Default private List<CloudWatchMetric> ec2Metrics = new ArrayList<>();

  @SchemaIgnore private boolean shouldDoLambdaVerification;

  @SchemaIgnore private boolean shouldDoECSClusterVerification;

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
      MetricAnalysisExecutionData executionData, Map<String, String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);

    Map<String, String> lambdaFunctions = new HashMap<>();
    String clusterName = null;
    if (shouldDoLambdaVerification && getDeploymentType(context).equals(DeploymentType.AWS_LAMBDA)) {
      AwsLambdaContextElement elements = context.getContextElement(ContextElementType.PARAM);
      elements.getFunctionArns().forEach(contextElement -> {
        lambdaFunctions.put(contextElement.getFunctionName(), contextElement.getFunctionArn());
      });
    }

    if (shouldDoECSClusterVerification && getDeploymentType(context).equals(DeploymentType.ECS)) {
      ContainerServiceElement containerServiceElement = context.getContextElement(ContextElementType.PARAM);
      clusterName = containerServiceElement.getClusterName();
    }

    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);

    if (settingAttribute == null) {
      throw new WingsException("No aws config with id: " + analysisServerConfigId + " found");
    }

    final AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    final long dataCollectionStartTimeStamp = Timestamp.minuteBoundary(System.currentTimeMillis());
    Map<AwsNameSpace, List<CloudWatchMetric>> cloudWatchMetrics = cloudWatchService.getCloudWatchMetrics();

    metricAnalysisService.saveMetricTemplates(context.getAppId(), StateType.CLOUD_WATCH,
        context.getStateExecutionInstanceId(), null, fetchMetricTemplates(cloudWatchMetrics));
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
            .collectionTime(Integer.parseInt(getTimeDuration()))
            .dataCollectionMinute(0)
            .encryptedDataDetails(
                secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId()))
            .hosts(hosts)
            .region(getRegion())
            .loadBalancerMetrics(loadBalancerMetrics)
            .ec2Metrics(ec2Metrics)
            .lambdaFunctionNames(createLambdaMetrics(lambdaFunctions.keySet(), cloudWatchMetrics))
            .metricsByECSClusterName(createECSMetrics(clusterName, cloudWatchMetrics))
            .build();

    analysisContext.getTestNodes().put(TEST_HOST_NAME, DEFAULT_GROUP_NAME);
    if (getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT) {
      for (int i = 1; i <= CANARY_DAYS_TO_COLLECT; ++i) {
        analysisContext.getControlNodes().put(CONTROL_HOST_NAME + "-" + i, DEFAULT_GROUP_NAME);
      }
    }

    String waitId = generateUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.CLOUD_WATCH_COLLECT_METRIC_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withTags(isNotEmpty(dataCollectionInfo.getAwsConfig().getTag())
                                            ? singletonList(dataCollectionInfo.getAwsConfig().getTag())
                                            : null)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .withInfrastructureMappingId(infrastructureMappingId)
                                    .withTimeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration()) + 120))
                                    .build();
    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), executionData, false), waitId);
    return delegateService.queueTask(delegateTask);
  }

  private Map<String, List<CloudWatchMetric>> createECSMetrics(
      String clusterName, Map<AwsNameSpace, List<CloudWatchMetric>> cloudWatchMetrics) {
    if (isEmpty(clusterName)) {
      return null;
    }
    Map<String, List<CloudWatchMetric>> ecsMetrics = new HashMap<>();
    ecsMetrics.put(clusterName, cloudWatchMetrics.get(AwsNameSpace.ECS));
    return ecsMetrics;
  }

  public static Map<String, TimeSeriesMetricDefinition> fetchMetricTemplates(
      Map<AwsNameSpace, List<CloudWatchMetric>> timeSeriesToCollect) {
    Map<String, TimeSeriesMetricDefinition> rv = new HashMap<>();
    for (Entry<AwsNameSpace, List<CloudWatchMetric>> entry : timeSeriesToCollect.entrySet()) {
      for (CloudWatchMetric timeSeries : entry.getValue()) {
        rv.put(timeSeries.getMetricName(),
            TimeSeriesMetricDefinition.builder()
                .metricName(timeSeries.getMetricName())
                .metricType(MetricType.valueOf(timeSeries.getMetricType()))
                .build());
      }
    }
    return rv;
  }

  public static Map<String, List<CloudWatchMetric>> createLambdaMetrics(
      Set<String> functionNames, Map<AwsNameSpace, List<CloudWatchMetric>> cloudWatchMetrics) {
    if (isEmpty(functionNames)) {
      return null;
    }
    Map<String, List<CloudWatchMetric>> lambdaMetrics = new HashMap<>();
    functionNames.forEach(function -> { lambdaMetrics.put(function, cloudWatchMetrics.get(AwsNameSpace.LAMBDA)); });
    return lambdaMetrics;
  }

  @Attributes(required = false, title = "Expression for Host/Container name")
  public String getHostnameTemplate() {
    if (isEmpty(hostnameTemplate)) {
      return "${host.ec2Instance.instanceId}";
    }
    return hostnameTemplate;
  }

  public void setHostnameTemplate(String hostnameTemplate) {
    this.hostnameTemplate = hostnameTemplate;
  }

  public String getRegion() {
    if (isEmpty(region)) {
      return "us-east-1";
    }
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public Map<String, List<CloudWatchMetric>> getLoadBalancerMetrics() {
    return loadBalancerMetrics;
  }

  public void setLoadBalancerMetrics(Map<String, List<CloudWatchMetric>> loadBalancerMetrics) {
    this.loadBalancerMetrics = loadBalancerMetrics;
  }

  public List<CloudWatchMetric> getEc2Metrics() {
    return ec2Metrics;
  }

  public boolean getShouldDoLambdaVerification() {
    return shouldDoLambdaVerification;
  }

  public void setShouldDoLambdaVerification(boolean shouldDoLambdaVerification) {
    this.shouldDoLambdaVerification = shouldDoLambdaVerification;
  }

  public boolean isShouldDoECSClusterVerification() {
    return shouldDoECSClusterVerification;
  }

  public void setShouldDoECSClusterVerification(boolean shouldDoECSClusterVerification) {
    this.shouldDoECSClusterVerification = shouldDoECSClusterVerification;
  }

  public void setEc2Metrics(List<CloudWatchMetric> ec2Metrics) {
    this.ec2Metrics = ec2Metrics;
  }
}
