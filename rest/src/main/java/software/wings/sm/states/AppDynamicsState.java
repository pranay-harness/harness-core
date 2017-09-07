package software.wings.sm.states;

import static software.wings.api.AppdynamicsAnalysisResponse.Builder.anAppdynamicsAnalysisResponse;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.SettingAttribute.Category.*;
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
import software.wings.beans.TemplateExpression;
import software.wings.collect.AppdynamicsMetricDataCallback;
import software.wings.common.Constants;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.metrics.MetricSummary;
import software.wings.metrics.RiskLevel;
import software.wings.service.impl.appdynamics.AppDynamicsSettingProvider;
import software.wings.service.impl.appdynamics.AppdynamicsApplication;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.appdynamics.AppdynamicsMetric;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.settings.SettingValue;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.EnumData;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
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

  @Transient @Inject private TemplateExpressionProcessor templateExpressionProcessor;

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
    List<TemplateExpression> templateExpressions = getTemplateExpressions();
    String analysisServerConfigIdExpression = null;
    String applicationIdExpression = null;
    String tierIdExpression = null;
    if (templateExpressions != null && !templateExpressions.isEmpty()) {
      for (TemplateExpression templateExpression : templateExpressions) {
        String fieldName = templateExpression.getFieldName();
        if (fieldName != null) {
          if (fieldName.equals("analysisServerConfigId")) {
            analysisServerConfigIdExpression = templateExpression.getExpression();
          } else if (fieldName.equals("applicationId")) {
            applicationIdExpression = templateExpression.getExpression();
          } else if (fieldName.equals("tierId")) {
            tierIdExpression = templateExpression.getExpression();
          }
        }
      }
    }
    String accountId = ((ExecutionContextImpl) context).getApp().getAccountId();
    AppDynamicsConfig appDynamicsConfig = null;
    String finalApplicationId = applicationId;
    String finalServerConfigId = analysisServerConfigId;
    String finalTierId = tierId;
    if (analysisServerConfigIdExpression != null) {
      SettingAttribute settingAttribute = templateExpressionProcessor.resolveSettingAttribute(
          context, accountId, analysisServerConfigIdExpression, CONNECTOR);
      if (settingAttribute == null) {
        throw new WingsException("No appdynamics server with id: " + analysisServerConfigIdExpression + " found");
      }
      finalServerConfigId = settingAttribute.getUuid();
      if (settingAttribute.getValue() instanceof AppDynamicsConfig) {
        appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
      }
    } else {
      final SettingAttribute settingAttribute = settingsService.get(finalServerConfigId);
      if (settingAttribute == null) {
        throw new WingsException("No appdynamics setting with id: " + finalServerConfigId + " found");
      }
      appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();
    }

    if (applicationIdExpression != null) {
      String appDAppName = context.renderExpression(applicationIdExpression);
      String resolveAppDynamicsAppId = resolveAppDynamicsAppId(finalServerConfigId, appDAppName);
      if (resolveAppDynamicsAppId == null) {
        throw new WingsException("No AppDynamic App exists  with name : " + appDAppName + " found");
      }
      finalApplicationId = resolveAppDynamicsAppId;
    }

    if (tierIdExpression != null) {
      String appDTierName = context.renderExpression(applicationIdExpression);
      String resolveAppDynamicsTierId = resolveAppDynamicsTierId(finalServerConfigId, finalApplicationId, appDTierName);
      if (resolveAppDynamicsTierId == null) {
        throw new WingsException(
            "No AppDynamic Tier exist for app " + finalApplicationId + "with name : " + appDTierName + " found");
      }
      finalTierId = resolveAppDynamicsTierId;
    }

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    final AppdynamicsDataCollectionInfo dataCollectionInfo =
        new AppdynamicsDataCollectionInfo(appDynamicsConfig, context.getAppId(), context.getStateExecutionInstanceId(),
            Long.parseLong(finalApplicationId), Long.parseLong(finalTierId), Integer.parseInt(timeDuration));

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

  private String resolveAppDynamicsAppId(String analysisServerConfigId, String appDAppName) {
    try {
      List<AppdynamicsApplication> apps = appdynamicsService.getApplications(analysisServerConfigId);
      if (apps == null || apps.isEmpty()) {
        return null;
      }
      Optional<AppdynamicsApplication> app =
          apps.stream()
              .filter(appdynamicsApplication -> appdynamicsApplication.getName().equals(appDAppName))
              .findFirst();
      if (app.isPresent()) {
        return String.valueOf(app.get().getId());
      }
    } catch (IOException e) {
      logger.error("Error fetching Appdynamics Applications", e);
      throw new WingsException("Error fetching Appdynamics Applications", e);
    }
    return null;
  }

  private String resolveAppDynamicsTierId(String analysisServerConfigId, String appId, String appdTierName) {
    try {
      List<AppdynamicsTier> appdynamicsTiers =
          appdynamicsService.getTiers(analysisServerConfigId, Integer.parseInt(appId));
      if (appdynamicsTiers == null || appdynamicsTiers.isEmpty()) {
        return null;
      }
      Optional<AppdynamicsTier> app = appdynamicsTiers.stream()
                                          .filter(appdynamicsTier -> appdynamicsTier.getName().equals(appdTierName))
                                          .findFirst();
      if (app.isPresent()) {
        return String.valueOf(app.get().getId());
      }
    } catch (IOException e) {
      logger.error("Error fetching Appdynamics tiers", e);
      throw new WingsException("Error fetching Appdynamics tiers", e);
    }
    return null;
  }
}
