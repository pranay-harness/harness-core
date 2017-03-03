package software.wings.sm.states;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.codec.binary.Base64;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.AppDynamicsExecutionData;
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.TaskType;
import software.wings.exception.WingsException;
import software.wings.service.impl.AppDynamicsSettingProvider;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Created by anubhaw on 8/4/16.
 */
public class AppDynamicsState extends HttpState {
  @Transient private static final Logger logger = LoggerFactory.getLogger(AppDynamicsState.class);

  @EnumData(enumDataProvider = AppDynamicsSettingProvider.class)
  @Attributes(required = true, title = "AppDynamics Server")
  private String appDynamicsConfigId;

  @Attributes(required = true, title = "Application Name") private String applicationName;
  @Attributes(required = true, title = "Metric Path",
      description = "Overall Application Performance|Average Response Time (ms)")
  private String metricPath;
  @Attributes(title = "Time duration (in minutes)", description = "Default 10 minutes") private String timeDuration;

  /**
   * Create a new Http State with given name.
   *
   * @param name name of the state.
   */
  public AppDynamicsState(String name) {
    super(name);
    this.setStateType(StateType.APP_DYNAMICS.name());
  }

  @Override
  protected ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    AppDynamicsConfig appdConfig =
        (AppDynamicsConfig) context.getSettingValue(appDynamicsConfigId, StateType.APP_DYNAMICS.name());

    String controllerUrl = appdConfig.getControllerUrl();

    String evaluatedMetricPath = context.renderExpression(metricPath);
    String evaluatedAppName = context.renderExpression(applicationName);
    String evaluatedTimeDuration = isNullOrEmpty(timeDuration) ? "10" : context.renderExpression(timeDuration);

    setUrl(String.format(
        "%s/rest/applications/%s/metric-data?metric-path=%s&time-range-type=BEFORE_NOW&duration-in-mins=%s",
        controllerUrl, urlEncodeString(evaluatedAppName), urlEncodeString(evaluatedMetricPath), evaluatedTimeDuration));
    setMethod("GET");
    setHeader("Authorization: Basic "
        + Base64.encodeBase64String(
              String.format("%s@%s:%s", appdConfig.getUsername(), appdConfig.getAccountname(), appdConfig.getPassword())
                  .getBytes(StandardCharsets.UTF_8)));

    ExecutionResponse executionResponse = super.executeInternal(context, activityId);

    HttpStateExecutionData httpStateExecutionData = (HttpStateExecutionData) executionResponse.getStateExecutionData();
    logger.info("Metric Data: {}", httpStateExecutionData.getHttpResponseBody());

    executionResponse.setStateExecutionData(AppDynamicsExecutionData.Builder.anAppDynamicsExecutionData()
                                                .withAssertionStatement(getAssertion())
                                                .withAppIdentifier(evaluatedAppName)
                                                .withMetricPath(evaluatedMetricPath)
                                                .build());

    return executionResponse;
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionResponse executionResponse = super.handleAsyncResponse(context, response);

    HttpStateExecutionData httpStateExecutionData = (HttpStateExecutionData) executionResponse.getStateExecutionData();

    AppDynamicsExecutionData appDynamicsExecutionData = (AppDynamicsExecutionData) context.getStateExecutionData();

    executionResponse.setStateExecutionData(AppDynamicsExecutionData.Builder.anAppDynamicsExecutionData()
                                                .withHttpResponseCode(httpStateExecutionData.getHttpResponseCode())
                                                .withAssertionStatement(getAssertion())
                                                .withAssertionStatus(httpStateExecutionData.getAssertionStatus())
                                                .withResponse(httpStateExecutionData.getHttpResponseBody())
                                                .withAppIdentifier(appDynamicsExecutionData.getAppIdentifier())
                                                .withMetricPath(appDynamicsExecutionData.getMetricPath())
                                                .build());

    return executionResponse;
  }

  protected TaskType getTaskType() {
    return TaskType.APP_DYNAMICS;
  }

  private String urlEncodeString(String queryString) {
    try {
      return URLEncoder.encode(queryString, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, "message", "Couldn't url-encode " + queryString);
    }
  }

  /**
   * Gets application identifier.
   *
   * @return the application identifier
   */
  public String getApplicationName() {
    return applicationName;
  }

  /**
   * Sets application identifier.
   *
   * @param applicationName the application identifier
   */
  public void setApplicationName(String applicationName) {
    this.applicationName = applicationName;
  }

  /**
   * Gets metric path.
   *
   * @return the metric path
   */
  public String getMetricPath() {
    return metricPath;
  }

  /**
   * Sets metric path.
   *
   * @param metricPath the metric path
   */
  public void setMetricPath(String metricPath) {
    this.metricPath = metricPath;
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

  /**
   * Getter for property 'appDynamicsConfigId'.
   *
   * @return Value for property 'appDynamicsConfigId'.
   */
  public String getAppDynamicsConfigId() {
    return appDynamicsConfigId;
  }

  /**
   * Setter for property 'appDynamicsConfigId'.
   *
   * @param appDynamicsConfigId Value to set for property 'appDynamicsConfigId'.
   */
  public void setAppDynamicsConfigId(String appDynamicsConfigId) {
    this.appDynamicsConfigId = appDynamicsConfigId;
  }

  @SchemaIgnore
  @Override
  public String getBody() {
    return super.getBody();
  }

  @SchemaIgnore
  @Override
  public String getMethod() {
    return super.getMethod();
  }

  @SchemaIgnore
  @Override
  public String getHeader() {
    return super.getHeader();
  }

  @SchemaIgnore
  @Override
  public String getUrl() {
    return super.getUrl();
  }

  @Attributes(title = "Assertion")
  public String getAssertion() {
    return super.getAssertion();
  }
}
