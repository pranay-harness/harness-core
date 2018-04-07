package software.wings.sm.states;

import static com.amazonaws.services.cloudwatch.model.Statistic.Average;
import static com.amazonaws.services.cloudwatch.model.Statistic.Maximum;
import static com.amazonaws.services.cloudwatch.model.Statistic.Minimum;
import static com.amazonaws.services.cloudwatch.model.Statistic.SampleCount;
import static com.amazonaws.services.cloudwatch.model.Statistic.Sum;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CloudWatchExecutionData;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping.AwsRegionDataProvider;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsSettingProvider;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateExecutionException;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 12/7/16.
 */
public class CloudWatchState extends State {
  @Transient @Inject private SettingsService settingsService;
  @Transient @Inject private ActivityService activityService;
  @Transient @Inject private AwsHelperService awsHelperService;
  @Transient @Inject private SecretManager secretManager;

  @EnumData(enumDataProvider = AwsSettingProvider.class)
  @Attributes(required = true, title = "AWS account")
  private String awsCredentialsConfigId;

  @Attributes(title = "Region")
  @DefaultValue("us-east-1")
  @EnumData(enumDataProvider = AwsRegionDataProvider.class)
  private String region = "us-east-1";

  @Attributes(required = true, title = "Namespace") private String namespace;

  @Attributes(required = true, title = "MetricName") private String metricName;
  @Attributes(title = "Percentile (pNN.NN)") private String percentile;
  @Attributes(title = "Dimensions") private List<Dimension> dimensions = new ArrayList<>();
  @Attributes(title = "Time duration (in minutes, default 10 minutes)") private java.lang.String timeDuration;
  @Attributes(title = "Assertion") private String assertion;

  @Transient private static final Logger logger = LoggerFactory.getLogger(CloudWatchState.class);
  private static final int DEFAULT_TIME_DURATION = 10 * 60; // seconds
  private static final int DEFAULT_AGGREGATION_PERIOD = 60; // seconds

  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public CloudWatchState(String name) {
    super(name, StateType.CLOUD_WATCH.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);

    CloudWatchExecutionData stateExecutionData = CloudWatchExecutionData.builder()
                                                     .namespace(namespace)
                                                     .metricName(metricName)
                                                     .percentile(percentile)
                                                     .dimensions(dimensions)
                                                     .assertionStatement(assertion)
                                                     .build();

    SettingAttribute settingAttribute = settingsService.get(GLOBAL_APP_ID, awsCredentialsConfigId);
    if (settingAttribute == null || !(settingAttribute.getValue() instanceof AwsConfig)) {
      throw new StateExecutionException("AWS account setting not found");
    }
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();
    List<EncryptedDataDetail> encryptionDetails =
        secretManager.getEncryptionDetails(awsConfig, context.getAppId(), context.getWorkflowExecutionId());
    ContextElement contextElement = (ContextElement) context.evaluateExpression("${instance}");
    if (contextElement != null) {
      HostElement hostElement = ((InstanceElement) contextElement).getHost();
      String hostName = hostElement.getHostName();
      if (isNotEmpty(hostName)) {
        String awsInstanceId = awsHelperService.getInstanceId(
            Regions.US_EAST_1, awsConfig.getAccessKey(), awsConfig.getSecretKey(), hostName);
        hostElement.setInstanceId(awsInstanceId);
      }
    }

    if (dimensions != null) {
      List<Dimension> evaluatedDimensions = dimensions.stream()
                                                .map(dimension -> {
                                                  Dimension d = new Dimension();
                                                  d.setName(dimension.getName());
                                                  d.setValue(context.renderExpression(dimension.getValue()));
                                                  return d;
                                                })
                                                .collect(toList());
      stateExecutionData.setDimensions(evaluatedDimensions);
    }

    GetMetricStatisticsRequest getMetricRequest = new GetMetricStatisticsRequest();

    getMetricRequest.setNamespace(namespace);
    getMetricRequest.setMetricName(metricName);
    List<String> statistics = asList(SampleCount.name(), Average.name(), Sum.name(), Minimum.name(), Maximum.name());
    if (isNotEmpty(percentile)) {
      getMetricRequest.setExtendedStatistics(asList(percentile));
    }
    getMetricRequest.setStatistics(statistics);
    getMetricRequest.setDimensions(stateExecutionData.getDimensions());
    getMetricRequest.setPeriod(DEFAULT_AGGREGATION_PERIOD);

    long startTimeOffset = isEmpty(timeDuration) ? DEFAULT_TIME_DURATION : Long.parseLong(timeDuration);
    long endEpoch = System.currentTimeMillis();
    long startEpoch = endEpoch - startTimeOffset * 1000;
    getMetricRequest.setStartTime(new Date(startEpoch));
    getMetricRequest.setEndTime(new Date(endEpoch));

    Datapoint datapoint =
        awsHelperService.getCloudWatchMetricStatistics(awsConfig, encryptionDetails, region, getMetricRequest);

    stateExecutionData.setDatapoint(datapoint);

    boolean status;
    String errorMsg = null;
    try {
      status = (boolean) context.evaluateExpression(assertion, prepareStateExecutionData(datapoint));
      logger.info("assertion status: {}", status);
    } catch (Exception e) {
      errorMsg = getMessage(e);
      logger.error("Error in Cloudwatch assertion evaluation", e);
      status = false;
    }

    ExecutionStatus executionStatus = status ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
    stateExecutionData.setStatus(executionStatus);
    stateExecutionData.setErrorMsg(errorMsg);
    stateExecutionData.setAssertionStatus(executionStatus.name());

    updateActivityStatus(activityId, ((ExecutionContextImpl) context).getApp().getUuid(), executionStatus);
    return ExecutionResponse.Builder.anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(stateExecutionData)
        .withErrorMessage(errorMsg)
        .build();
  }

  private Map<String, Object> prepareStateExecutionData(Datapoint datapoint) {
    Map<String, Object> stateExecutionData = new HashMap<>();
    if (datapoint != null) {
      if (datapoint.getExtendedStatistics() != null) {
        stateExecutionData.putAll(datapoint.getExtendedStatistics());
      }
      stateExecutionData.put("sampleCount", datapoint.getSampleCount());
      stateExecutionData.put("average", datapoint.getAverage());
      stateExecutionData.put("sum", datapoint.getSum());
      stateExecutionData.put("minimum", datapoint.getMinimum());
      stateExecutionData.put("maximum", datapoint.getMaximum());
    }
    return stateExecutionData;
  }

  @SchemaIgnore
  public AmazonCloudWatchClient getAmazonCloudWatchClient(BasicAWSCredentials awsCredentials) {
    return new AmazonCloudWatchClient(awsCredentials);
  }

  /**
   * Gets namespace.
   *
   * @return the namespace
   */

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  public String getNamespace() {
    return namespace;
  }

  /**
   * Sets namespace.
   *
   * @param namespace the namespace
   */
  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  /**
   * Gets metric name.
   *
   * @return the metric name
   */
  public String getMetricName() {
    return metricName;
  }

  /**
   * Sets metric name.
   *
   * @param metricName the metric name
   */
  public void setMetricName(String metricName) {
    this.metricName = metricName;
  }

  /**
   * Gets percentile.
   *
   * @return the percentile
   */
  public String getPercentile() {
    return percentile;
  }

  /**
   * Sets percentile.
   *
   * @param percentile the percentile
   */
  public void setPercentile(String percentile) {
    this.percentile = percentile;
  }

  /**
   * Gets dimensions.
   *
   * @return the dimensions
   */
  public List<Dimension> getDimensions() {
    return dimensions;
  }

  /**
   * Sets dimensions.
   *
   * @param dimensions the dimensions
   */
  public void setDimensions(List<Dimension> dimensions) {
    this.dimensions = dimensions;
  }

  /**
   * Gets time duration.
   *
   * @return the time duration
   */
  public String getTimeDuration() {
    return timeDuration;
  }

  /**
   * Sets time duration.
   *
   * @param timeDuration the time duration
   */
  public void setTimeDuration(String timeDuration) {
    this.timeDuration = timeDuration;
  }

  public String getAssertion() {
    return assertion;
  }

  public void setAssertion(String assertion) {
    this.assertion = assertion;
  }

  public String getAwsCredentialsConfigId() {
    return awsCredentialsConfigId;
  }

  public void setAwsCredentialsConfigId(String awsCredentialsConfigId) {
    this.awsCredentialsConfigId = awsCredentialsConfigId;
  }

  /**
   * Create activity string.
   *
   * @param executionContext the execution context
   * @return the string
   */
  private String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).getApp();
    Environment env = ((ExecutionContextImpl) executionContext).getEnv();
    InstanceElement instanceElement = executionContext.getContextElement(ContextElementType.INSTANCE);

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .environmentId(env.getUuid())
                                          .environmentName(env.getName())
                                          .environmentType(env.getEnvironmentType())
                                          .commandName(getName())
                                          .type(Type.Verification)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(Collections.emptyList())
                                          .serviceVariables(Maps.newHashMap())
                                          .status(ExecutionStatus.RUNNING);

    if (instanceElement != null) {
      activityBuilder.serviceTemplateId(instanceElement.getServiceTemplateElement().getUuid())
          .serviceTemplateName(instanceElement.getServiceTemplateElement().getName())
          .serviceId(instanceElement.getServiceTemplateElement().getServiceElement().getUuid())
          .serviceName(instanceElement.getServiceTemplateElement().getServiceElement().getName())
          .serviceInstanceId(instanceElement.getUuid())
          .hostName(instanceElement.getHost().getHostName());
    }

    Activity activity = activityBuilder.build();
    activity.setAppId(app.getUuid());
    return activityService.save(activity).getUuid();
  }

  /**
   * Update activity status.
   *
   * @param activityId the activity id
   * @param appId      the app id
   * @param status     the status
   */
  private void updateActivityStatus(String activityId, String appId, ExecutionStatus status) {
    activityService.updateStatus(activityId, appId, status);
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }
}
