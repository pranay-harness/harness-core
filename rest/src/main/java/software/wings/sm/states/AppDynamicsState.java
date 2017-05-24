package software.wings.sm.states;

import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;
import static software.wings.sm.StateExecutionData.StateExecutionDataBuilder.aStateExecutionData;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.AppDynamicsSettingProvider;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateExecutionData;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by anubhaw on 8/4/16.
 */
public class AppDynamicsState extends State {
  @Transient private static final Logger logger = LoggerFactory.getLogger(AppDynamicsState.class);

  @Transient public static final int EXTRA_DATA_COLLECTION_TIME_MINUTES = 5;

  @EnumData(enumDataProvider = AppDynamicsSettingProvider.class)
  @Attributes(required = true, title = "AppDynamics Server")
  private String appDynamicsConfigId;

  @Attributes(required = true, title = "Application Name") private String applicationId;

  @Attributes(required = true, title = "Tier Name") private String tierId;

  @DefaultValue("15")
  @Attributes(title = "Analyze Time duration (in minutes)", description = "Default 15 minutes")
  private String timeDuration;

  private final AtomicBoolean aborted = new AtomicBoolean(false);

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
  public String getApplicationId() {
    return applicationId;
  }

  /**
   * Sets application identifier.
   *
   * @param applicationId the application identifier
   */
  public void setApplicationId(String applicationId) {
    this.applicationId = applicationId;
  }

  public String getTierId() {
    return tierId;
  }

  public void setTierId(String tierId) {
    this.tierId = tierId;
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
    final long verificationStartTime = System.currentTimeMillis();
    synchronized (this) {
      while (
          System.currentTimeMillis() - verificationStartTime < TimeUnit.MINUTES.toMillis(Integer.parseInt(timeDuration))
          && !aborted.get()) {
        try {
          this.wait(TimeUnit.MINUTES.toMillis(1L));
        } catch (InterruptedException e) {
          // do nothing
        }
      }
    }

    if (aborted.get()) {
      final StateExecutionData stateExecutionData = aStateExecutionData()
                                                        .withStateName(getName())
                                                        .withStartTs(verificationStartTime)
                                                        .withEndTs(System.currentTimeMillis())
                                                        .withStatus(ExecutionStatus.ABORTED)
                                                        .build();
      return anExecutionResponse()
          .withExecutionStatus(ExecutionStatus.ABORTED)
          .withErrorMessage("Verification was aborted")
          .withStateExecutionData(stateExecutionData)
          .build();
    }

    final StateExecutionData stateExecutionData = aStateExecutionData()
                                                      .withStateName(getName())
                                                      .withStartTs(verificationStartTime)
                                                      .withEndTs(System.currentTimeMillis())
                                                      .withStatus(ExecutionStatus.SUCCESS)
                                                      .build();
    return anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .withErrorMessage("Verification succeeded")
        .withStateExecutionData(stateExecutionData)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {
    synchronized (this) {
      aborted.set(true);
      this.notifyAll();
    }
  }
}
