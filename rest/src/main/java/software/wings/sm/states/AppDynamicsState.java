package software.wings.sm.states;

import static software.wings.api.AppdynamicsAnalysisResponse.Builder.anAppdynamicsAnalysisResponse;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.NotImplementedException;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.api.AppDynamicsExecutionData;
import software.wings.api.AppdynamicsAnalysisResponse;
import software.wings.api.PhaseElement;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.DelegateTask;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.collect.AppdynamicsMetricDataCallback;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.metrics.MetricSummary;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.appdynamics.AppDynamicsSettingProvider;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * Created by anubhaw on 8/4/16.
 */
public class AppDynamicsState extends AbstractAnalysisState {
  @Transient @SchemaIgnore private static final Logger logger = LoggerFactory.getLogger(AppDynamicsState.class);

  @EnumData(enumDataProvider = AppDynamicsSettingProvider.class)
  @Attributes(required = true, title = "AppDynamics Server")
  private String analysisServerConfigId;

  @Attributes(required = true, title = "Application Name") private String applicationId;

  @Attributes(required = true, title = "Tier Name") private String tierId;

  @Attributes(title = "Ignore verification failure") private Boolean ignoreVerificationFailure = false;

  @Inject @Transient private AppdynamicsService appdynamicsService;

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

  public Boolean getIgnoreVerificationFailure() {
    return ignoreVerificationFailure;
  }

  public void setIgnoreVerificationFailure(Boolean ignoreVerificationFailure) {
    this.ignoreVerificationFailure = ignoreVerificationFailure;
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    logger.debug("Executing AppDynamics state");
    triggerAnalysisDataCollection(context, null);
    final Set<String> canaryNewHostNames = getCanaryNewHostNames(context);
    final List<String> btNames = getBtNames();
    final AppDynamicsExecutionData executionData =
        AppDynamicsExecutionData.Builder.anAppDynamicsExecutionData()
            .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
            .withCanaryNewHostNames(canaryNewHostNames)
            .withBtNames(btNames)
            .withAppDynamicsConfigID(analysisServerConfigId)
            .withAppDynamicsApplicationId(Long.parseLong(applicationId))
            .withAppdynamicsTierId(Long.parseLong(tierId))
            .withAnalysisDuration(Integer.parseInt(timeDuration))
            .withStatus(ExecutionStatus.RUNNING)
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
        .withExecutionStatus(ExecutionStatus.RUNNING)
        .withErrorMessage("Appdynamics Verification running")
        .withStateExecutionData(executionData)
        .build();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, NotifyResponseData> response) {
    ExecutionStatus executionStatus = ExecutionStatus.SUCCESS;
    AppdynamicsAnalysisResponse executionResponse = (AppdynamicsAnalysisResponse) response.values().iterator().next();
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    MetricSummary finalMetrics =
        appdynamicsService.generateMetrics(context.getStateExecutionInstanceId(), app.getAccountId(), app.getAppId());
    if (finalMetrics == null) {
      logger.error("No data for appdynamics verification was generated");
      if (!ignoreVerificationFailure) {
        executionStatus = ExecutionStatus.FAILED;
      }
    } else {
      finalMetrics.setStateExecutionInstanceId(context.getStateExecutionInstanceId());
      try {
        wingsPersistence.save(finalMetrics);
      } catch (Exception e) {
        logger.error("Could not save analysis report", e);
        executionStatus = ExecutionStatus.FAILED;
        executionResponse.getAppDynamicsExecutionData().setErrorMsg(
            "Could not save analysis report, Please contact support");
      }
    }

    if (!ignoreVerificationFailure && finalMetrics != null && finalMetrics.getRiskLevel() == RiskLevel.HIGH) {
      executionStatus = ExecutionStatus.FAILED;
    }
    executionResponse.getAppDynamicsExecutionData().setStatus(executionStatus);
    return anExecutionResponse()
        .withExecutionStatus(executionStatus)
        .withStateExecutionData(executionResponse.getAppDynamicsExecutionData())
        .build();
  }

  @Override
  protected void triggerAnalysisDataCollection(ExecutionContext context, Set<String> hosts) {
    final SettingAttribute settingAttribute = settingsService.get(analysisServerConfigId);
    if (settingAttribute == null) {
      throw new WingsException("No appdynamics setting with id: " + analysisServerConfigId + " found");
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    final AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    final AppdynamicsDataCollectionInfo dataCollectionInfo =
        new AppdynamicsDataCollectionInfo(appDynamicsConfig, context.getAppId(), context.getStateExecutionInstanceId(),
            Long.parseLong(applicationId), Long.parseLong(tierId), Integer.parseInt(timeDuration));
    String waitId = UUIDGenerator.getUuid();
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String infrastructureMappingId = phaseElement == null ? null : phaseElement.getInfraMappingId();
    DelegateTask delegateTask = aDelegateTask()
                                    .withTaskType(TaskType.APPDYNAMICS_COLLECT_METRIC_DATA)
                                    .withAccountId(appService.get(context.getAppId()).getAccountId())
                                    .withAppId(context.getAppId())
                                    .withWaitId(waitId)
                                    .withParameters(new Object[] {dataCollectionInfo})
                                    .withEnvId(envId)
                                    .withInfrastructureMappingId(infrastructureMappingId)
                                    .build();
    waitNotifyEngine.waitForAll(new AppdynamicsMetricDataCallback(context.getAppId()), waitId);
    delegateService.queueTask(delegateTask);
  }

  private List<String> getBtNames() {
    try {
      final List<AppdynamicsMetric> appdynamicsMetrics = appdynamicsService.getTierBTMetrics(
          analysisServerConfigId, Long.parseLong(applicationId), Long.parseLong(tierId));
      final List<String> btNames = new ArrayList<>();
      for (AppdynamicsMetric appdynamicsMetric : appdynamicsMetrics) {
        btNames.add(appdynamicsMetric.getName());
      }
      logger.debug("AppDynamics BT names: " + String.join(", ", btNames));

      return btNames;
    } catch (Exception e) {
      logger.error("error fetching Appdynamics BTs", e);
      throw new WingsException("error fetching Appdynamics BTs", e);
    }
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

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
  @SchemaIgnore
  public AnalysisComparisonStrategy getComparisonStrategy() {
    throw new NotImplementedException();
  }
}
