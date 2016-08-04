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
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateType;

import java.nio.charset.StandardCharsets;

/**
 * Created by anubhaw on 8/4/16.
 */

public class AppDynamicsState extends HttpState {
  @Transient @Inject private SettingsService settingsService;

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

    setUrl(appdConfig.getControllerUrl() + "/rest/applications");
    setMethod("GET");
    setHeader("Authorization: Basic "
        + Base64.encodeBase64String(
              String.format("%s@%s:%s", appdConfig.getUsername(), appdConfig.getAccountname(), appdConfig.getPassword())
                  .getBytes(StandardCharsets.UTF_8)));

    ExecutionResponse executionResponse = super.execute(context);

    HttpStateExecutionData httpStateExecutionData = (HttpStateExecutionData) executionResponse.getStateExecutionData();
    executionResponse.setStateExecutionData(AppDynamicsExecutionData.Builder.anAppDynamicsExecutionData()
                                                .withHttpResponseCode(httpStateExecutionData.getHttpResponseCode())
                                                .withAssertionStatement(getAssertion())
                                                .withAssertionStatus(httpStateExecutionData.getAssertionStatus())
                                                .withResponse(httpStateExecutionData.getHttpResponseBody())
                                                .build());

    return executionResponse;
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

  @Attributes(required = true, title = "Assertion")
  @Override
  public String getAssertion() {
    return super.getAssertion();
  }
}
