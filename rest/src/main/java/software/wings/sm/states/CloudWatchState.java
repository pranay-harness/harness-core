package software.wings.sm.states;

import static com.amazonaws.services.cloudwatch.model.Statistic.Average;
import static com.amazonaws.services.cloudwatch.model.Statistic.Maximum;
import static com.amazonaws.services.cloudwatch.model.Statistic.Minimum;
import static com.amazonaws.services.cloudwatch.model.Statistic.SampleCount;
import static com.amazonaws.services.cloudwatch.model.Statistic.Sum;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getMessage;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.common.base.Strings;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.CloudWatchExecutionData;
import software.wings.api.CloudWatchExecutionData.Builder;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.impl.AwsSettingProvider;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateExecutionException;
import software.wings.sm.StateType;
import software.wings.stencils.EnumData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by anubhaw on 12/7/16.
 */
public class CloudWatchState extends State {
  @Transient @Inject private SettingsService settingsService;
  @Transient @Inject private ActivityService activityService;
  @Transient @Inject private AwsHelperService awsHelperService;

  @EnumData(enumDataProvider = AwsSettingProvider.class)
  @Attributes(required = true, title = "AWS account")
  private String awsCredentialsConfigId;

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

    CloudWatchExecutionData stateExecutionData = Builder.aCloudWatchExecutionData()
                                                     .withNamespace(namespace)
                                                     .withMetricName(metricName)
                                                     .withPercentile(percentile)
                                                     .withDimensions(dimensions)
                                                     .withAssertionStatement(assertion)
                                                     .build();

    SettingAttribute settingAttribute = settingsService.get(GLOBAL_APP_ID, awsCredentialsConfigId);
    if (settingAttribute == null || !(settingAttribute.getValue() instanceof AwsConfig)) {
      throw new StateExecutionException("AWS account setting not found");
    }
    AwsConfig awsConfig = (AwsConfig) settingAttribute.getValue();

    ContextElement contextElement = (ContextElement) context.evaluateExpression("${instance}");
    if (contextElement != null) {
      HostElement hostElement = ((InstanceElement) contextElement).getHostElement();
      String hostName = hostElement.getHostName();
      if (!Strings.isNullOrEmpty(hostName)) {
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
                                                .collect(Collectors.toList());
      stateExecutionData.setDimensions(evaluatedDimensions);
    }

    AmazonCloudWatchClient cloudWatchClient =
        awsHelperService.getAwsCloudWatchClient(awsConfig.getAccessKey(), awsConfig.getSecretKey());

    GetMetricStatisticsRequest getMetricRequest = new GetMetricStatisticsRequest();

    getMetricRequest.setNamespace(namespace);
    getMetricRequest.setMetricName(metricName);
    List<String> statistics =
        new ArrayList<>(asList(SampleCount.name(), Average.name(), Sum.name(), Minimum.name(), Maximum.name()));
    if (!Strings.isNullOrEmpty(percentile)) {
      getMetricRequest.setExtendedStatistics(asList(percentile));
    }
    getMetricRequest.setStatistics(statistics);
    getMetricRequest.setDimensions(stateExecutionData.getDimensions());
    getMetricRequest.setPeriod(DEFAULT_AGGREGATION_PERIOD);

    long startTimeOffset = Strings.isNullOrEmpty(timeDuration) ? DEFAULT_TIME_DURATION : Long.parseLong(timeDuration);
    long endEpoch = System.currentTimeMillis();
    long startEpoch = endEpoch - startTimeOffset * 1000;
    getMetricRequest.setStartTime(new Date(startEpoch));
    getMetricRequest.setEndTime(new Date(endEpoch));

    GetMetricStatisticsResult metricStatistics = cloudWatchClient.getMetricStatistics(getMetricRequest);
    Datapoint datapoint =
        metricStatistics.getDatapoints().stream().max(Comparator.comparing(Datapoint::getTimestamp)).orElse(null);

    stateExecutionData.setDatapoint(datapoint);

    boolean status;
    String errorMsg = null;
    try {
      status = (boolean) context.evaluateExpression(assertion, prepareStateExecutionData(datapoint));
      logger.info("assertion status: {}", status);
    } catch (Exception e) {
      errorMsg = getMessage(e);
      logger.error("Error in Cloudwatch assertion evaluation: " + e.getMessage(), e);
      Arrays.stream(e.getStackTrace()).forEach(elem -> logger.error("Trace: {}", elem));
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

    Activity.Builder activityBuilder =
        anActivity()
            .withAppId(app.getUuid())
            .withApplicationName(app.getName())
            .withEnvironmentId(env.getUuid())
            .withEnvironmentName(env.getName())
            .withEnvironmentType(env.getEnvironmentType())
            .withCommandName(getName())
            .withType(Type.Verification)
            .withWorkflowType(executionContext.getWorkflowType())
            .withWorkflowExecutionName(executionContext.getWorkflowExecutionName())
            .withStateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
            .withStateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
            .withCommandType(getStateType())
            .withWorkflowExecutionId(executionContext.getWorkflowExecutionId());

    if (instanceElement != null) {
      activityBuilder.withServiceTemplateId(instanceElement.getServiceTemplateElement().getUuid())
          .withServiceTemplateName(instanceElement.getServiceTemplateElement().getName())
          .withServiceId(instanceElement.getServiceTemplateElement().getServiceElement().getUuid())
          .withServiceName(instanceElement.getServiceTemplateElement().getServiceElement().getName())
          .withServiceInstanceId(instanceElement.getUuid())
          .withHostName(instanceElement.getHostElement().getHostName());
    }

    return activityService.save(activityBuilder.build()).getUuid();
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
}
