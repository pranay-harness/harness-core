package software.wings.sm.states;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.codec.binary.Base64;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.AppDynamicsExecutionData;
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.ErrorCodes;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.exception.WingsException;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Created by anubhaw on 8/4/16.
 */

public class AppDynamicsState extends HttpState {
  @Transient @Inject private SettingsService settingsService;

  @Attributes(title = "Application identifier", description = "application-name or application-id")
  private String applicationIdentifier;
  @Attributes(title = "Metric Path", description = "Overall Application Performance|Average Response Time (ms)")
  private String metricPath;

  private static final Logger logger = LoggerFactory.getLogger(AppDynamicsState.class);

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
  public ExecutionResponse execute(ExecutionContext context) {
    AppDynamicsConfig appdConfig =
        (AppDynamicsConfig) settingsService.getGlobalSettingAttributesByType(SettingVariableTypes.APP_DYNAMICS)
            .get(0)
            .getValue();
    String controllerUrl = appdConfig.getControllerUrl();

    String evaluatedMetricPath = urlEncodeString(context.renderExpression(metricPath));
    String evaluatedAppIdentifier = urlEncodeString(context.renderExpression(applicationIdentifier));

    setUrl(appdConfig.getControllerUrl() + "/rest/applications");
    setUrl(String.format(
        "%s/rest/applications/%s/metric-data?metric-path=%s&time-range-type=BEFORE_NOW&duration-in-mins=60",
        controllerUrl, evaluatedAppIdentifier, evaluatedMetricPath));
    setMethod("GET");
    setHeader("Authorization: Basic "
        + Base64.encodeBase64String(
              String.format("%s@%s:%s", appdConfig.getUsername(), appdConfig.getAccountname(), appdConfig.getPassword())
                  .getBytes(StandardCharsets.UTF_8)));

    ExecutionResponse executionResponse = super.execute(context);

    HttpStateExecutionData httpStateExecutionData = (HttpStateExecutionData) executionResponse.getStateExecutionData();
    logger.info("Metric Data: {}", httpStateExecutionData.getHttpResponseBody());

    executionResponse.setStateExecutionData(AppDynamicsExecutionData.Builder.anAppDynamicsExecutionData()
                                                .withHttpResponseCode(httpStateExecutionData.getHttpResponseCode())
                                                .withAssertionStatement(getAssertion())
                                                .withAssertionStatus(httpStateExecutionData.getAssertionStatus())
                                                .withResponse(httpStateExecutionData.getHttpResponseBody())
                                                .build());

    return executionResponse;
  }

  private String urlEncodeString(String queryString) {
    try {
      return URLEncoder.encode(queryString, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "message", "Couldn't url-encode " + queryString);
    }
  }

  @SchemaIgnore
  @Override
  public String getBody() {
    return super.getBody();
  }

  @SchemaIgnore
  @Override
  public String getHeader() {
    return super.getHeader();
  }

  @SchemaIgnore
  @Override
  public String getMethod() {
    return super.getMethod();
  }

  @SchemaIgnore
  @Override
  public String getUrl() {
    return super.getUrl();
  }

  public String getApplicationIdentifier() {
    return applicationIdentifier;
  }

  public void setApplicationIdentifier(String applicationIdentifier) {
    this.applicationIdentifier = applicationIdentifier;
  }

  public String getMetricPath() {
    return metricPath;
  }

  public void setMetricPath(String metricPath) {
    this.metricPath = metricPath;
  }
}
