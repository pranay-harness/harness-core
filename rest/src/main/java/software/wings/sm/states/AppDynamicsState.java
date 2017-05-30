package software.wings.sm.states;

import static software.wings.api.AppdynamicsAnalysisResponse.Builder.anAppdynamicsAnalysisResponse;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.AppDynamicsExecutionData;
import software.wings.api.AppdynamicsAnalysisResponse;
import software.wings.api.CanaryWorkflowStandardParams;
import software.wings.api.InfraNodeRequest;
import software.wings.service.impl.AppDynamicsSettingProvider;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

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

  @Inject @Transient private WaitNotifyEngine waitNotifyEngine;

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
    CanaryWorkflowStandardParams canaryWorkflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    List<InfraNodeRequest> infraNodeRequests = canaryWorkflowStandardParams.getInfraNodeRequests();
    logger.info("infraNodeRequests: {}", infraNodeRequests);

    logger.info("Current Phase Instances: {}", canaryWorkflowStandardParams.getInstances());

    final AppDynamicsExecutionData executionData = AppDynamicsExecutionData.Builder.anAppDynamicsExecutionData()
                                                       .withAppDynamicsConfigID(appDynamicsConfigId)
                                                       .withAppDynamicsApplicationId(Long.parseLong(applicationId))
                                                       .withAppdynamicsTierId(Long.parseLong(tierId))
                                                       .withCorrelationId(UUID.randomUUID().toString())
                                                       .build();
    final AppdynamicsAnalysisResponse response = anAppdynamicsAnalysisResponse()
                                                     .withAppDynamicsExecutionData(executionData)
                                                     .withExecutionStatus(ExecutionStatus.SUCCESS)
                                                     .build();
    final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    scheduledExecutorService.schedule(() -> {
      waitNotifyEngine.notify(executionData.getCorrelationId(), response);
    }, Long.parseLong(timeDuration), TimeUnit.MINUTES);

    return anExecutionResponse()
        .withAsync(true)
        .withCorrelationIds(Collections.singletonList(executionData.getCorrelationId()))
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .withErrorMessage("Verification succeeded")
        .withStateExecutionData(executionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    AppdynamicsAnalysisResponse executionResponse = (AppdynamicsAnalysisResponse) response.values().iterator().next();
    return anExecutionResponse()
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .withStateExecutionData(executionResponse.getAppDynamicsExecutionData())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
