package software.wings.sm.states;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.codec.binary.Base64;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.AppDynamicsExecutionData;
import software.wings.api.HttpStateExecutionData;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.TaskType;
import software.wings.service.impl.AppDynamicsSettingProvider;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Created by anubhaw on 8/4/16.
 */
public class AppDynamicsState extends State {
  @Transient private static final Logger logger = LoggerFactory.getLogger(AppDynamicsState.class);

  @EnumData(enumDataProvider = AppDynamicsSettingProvider.class)
  @Attributes(required = true, title = "AppDynamics Server")
  private String appDynamicsConfigId;

  @Attributes(required = true, title = "Application Name") private String applicationName;

  @Attributes(required = true, title = "Tier Name") private String tierName;

  @DefaultValue("15")
  @Attributes(title = "Analyze Time duration (in minutes)", description = "Default 15 minutes")
  private String timeDuration;

  /**
   * Create a new Http State with given name.
   *
   * @param name name of the state.
   */
  public AppDynamicsState(String name) {
    super(name, StateType.APP_DYNAMICS.getType());
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

  public String getTierName() {
    return tierName;
  }

  public void setTierName(String tierName) {
    this.tierName = tierName;
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

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    return null;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
