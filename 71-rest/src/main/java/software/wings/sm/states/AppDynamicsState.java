package software.wings.sm.states;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.common.VerificationConstants.APPDYNAMICS_DEEPLINK_FORMAT;
import static software.wings.service.impl.analysis.TimeSeriesMlAnalysisType.PREDICTIVE;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.APP_DYNAMICS_24X7_VALUES_TO_ANALYZE;
import static software.wings.service.impl.newrelic.NewRelicMetricValueDefinition.APP_DYNAMICS_VALUES_TO_ANALYZE;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.context.ContextElementType;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentType;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.TemplateExpression;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.metrics.TimeSeriesMetricDefinition;
import software.wings.service.impl.analysis.AnalysisComparisonStrategy;
import software.wings.service.impl.analysis.AnalysisComparisonStrategyProvider;
import software.wings.service.impl.analysis.AnalysisContext;
import software.wings.service.impl.analysis.AnalysisTolerance;
import software.wings.service.impl.analysis.AnalysisToleranceProvider;
import software.wings.service.impl.analysis.DataCollectionCallback;
import software.wings.service.impl.analysis.TimeSeriesMetricGroup.TimeSeriesMlAnalysisGroupInfo;
import software.wings.service.impl.analysis.TimeSeriesMlAnalysisType;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.appdynamics.AppdynamicsTier;
import software.wings.service.impl.appdynamics.AppdynamicsTimeSeries;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.intfc.appdynamics.AppdynamicsService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 8/4/16.
 */
public class AppDynamicsState extends AbstractMetricAnalysisState {
  @Transient @SchemaIgnore private static final Logger logger = LoggerFactory.getLogger(AppDynamicsState.class);

  @Transient @Inject protected AppdynamicsService appdynamicsService;

  @Attributes(required = true, title = "AppDynamics Server") private String analysisServerConfigId;

  @Attributes(required = true, title = "Application Name") private String applicationId;

  @Attributes(required = true, title = "Tier Name") private String tierId;

  private List<AppdynamicsTier> dependentTiersToAnalyze;

  /**
   * Create a new Http State with given name.
   *
   * @param name name of the state.
   */
  public AppDynamicsState(String name) {
    super(name, StateType.APP_DYNAMICS);
  }

  @EnumData(enumDataProvider = AnalysisComparisonStrategyProvider.class)
  @Attributes(required = true, title = "Baseline for Risk Analysis")
  @DefaultValue("COMPARE_WITH_PREVIOUS")
  public AnalysisComparisonStrategy getComparisonStrategy() {
    if (isBlank(comparisonStrategy)) {
      return AnalysisComparisonStrategy.COMPARE_WITH_PREVIOUS;
    }
    return AnalysisComparisonStrategy.valueOf(comparisonStrategy);
  }

  @Attributes(title = "Analysis Time duration (in minutes)", description = "Default 15 minutes")
  @DefaultValue("15")
  public String getTimeDuration() {
    if (isBlank(timeDuration)) {
      return String.valueOf(15);
    }
    return timeDuration;
  }

  @EnumData(enumDataProvider = AnalysisToleranceProvider.class)
  @Attributes(required = true, title = "Algorithm Sensitivity")
  @DefaultValue("MEDIUM")
  public AnalysisTolerance getAnalysisTolerance() {
    if (isBlank(tolerance)) {
      return AnalysisTolerance.LOW;
    }
    return AnalysisTolerance.valueOf(tolerance);
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

  @Override
  protected String triggerAnalysisDataCollection(ExecutionContext context, AnalysisContext analysisContext,
      MetricAnalysisExecutionData executionData, Map<String, String> hosts) {
    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    String envId = workflowStandardParams == null ? null : workflowStandardParams.getEnv().getUuid();

    metricAnalysisService.saveMetricTemplates(context.getAppId(), StateType.APP_DYNAMICS,
        context.getStateExecutionInstanceId(), null, APP_DYNAMICS_VALUES_TO_ANALYZE);

    SettingAttribute settingAttribute = null;
    String finalApplicationId = applicationId;
    String finalServerConfigId = analysisServerConfigId;
    String finalTierId = tierId;
    if (!isEmpty(getTemplateExpressions())) {
      TemplateExpression configIdExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "analysisServerConfigId");
      if (configIdExpression != null) {
        settingAttribute = templateExpressionProcessor.resolveSettingAttribute(context, configIdExpression);
        finalServerConfigId = settingAttribute.getUuid();
      }
      TemplateExpression appIdExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "applicationId");
      if (appIdExpression != null) {
        finalApplicationId = templateExpressionProcessor.resolveTemplateExpression(context, appIdExpression);
      }
      TemplateExpression tierIdExpression =
          templateExpressionProcessor.getTemplateExpression(getTemplateExpressions(), "tierId");
      if (tierIdExpression != null) {
        finalTierId = templateExpressionProcessor.resolveTemplateExpression(context, tierIdExpression);
      }
    }
    if (settingAttribute == null) {
      settingAttribute = settingsService.get(finalServerConfigId);
      if (settingAttribute == null) {
        throw new WingsException("No appdynamics setting with id: " + finalServerConfigId + " found");
      }
    }
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingAttribute.getValue();

    List<AppdynamicsTier> dependentTiers = new ArrayList<>();
    Map<String, TimeSeriesMlAnalysisGroupInfo> metricGroups = new HashMap<>();
    AppdynamicsTier analyzedTier = AppdynamicsTier.builder().build();
    TimeSeriesMlAnalysisType analyzedTierAnalysisType = getComparisonStrategy() == AnalysisComparisonStrategy.PREDICTIVE
        ? PREDICTIVE
        : TimeSeriesMlAnalysisType.COMPARATIVE;
    try {
      Set<AppdynamicsTier> tiers = appdynamicsService.getTiers(finalServerConfigId, Long.parseLong(finalApplicationId));
      final long tierId = Long.parseLong(finalTierId);
      tiers.stream().filter(tier -> tier.getId() == tierId).forEach(tier -> {
        metricGroups.put(tier.getName(),
            TimeSeriesMlAnalysisGroupInfo.builder()
                .groupName(tier.getName())
                .mlAnalysisType(analyzedTierAnalysisType)
                .build());
        analyzedTier.setName(tier.getName());
        analyzedTier.setId(tierId);
      });
      Preconditions.checkState(!isEmpty(analyzedTier.getName()), "failed for " + analyzedTier);

      if (!isEmpty(dependentTiersToAnalyze)) {
        dependentTiers = Lists.newArrayList(appdynamicsService.getDependentTiers(
            finalServerConfigId, Long.parseLong(finalApplicationId), analyzedTier));

        for (Iterator<AppdynamicsTier> iterator = dependentTiers.iterator(); iterator.hasNext();) {
          AppdynamicsTier tier = iterator.next();
          if (!dependentTiersToAnalyze.contains(tier)) {
            iterator.remove();
          }
        }
      }
    } catch (IOException e) {
      throw new WingsException(
          "Error executing appdynamics state with id : " + context.getStateExecutionInstanceId(), e);
    }

    final long dataCollectionStartTimeStamp = Timestamp.currentMinuteBoundary();
    List<DelegateTask> delegateTasks = new ArrayList<>();
    String[] waitIds = new String[dependentTiers.size() + 1];
    waitIds[0] = createDelegateTask(context, analyzedTierAnalysisType == PREDICTIVE ? Collections.emptyMap() : hosts,
        envId, finalApplicationId, Long.parseLong(finalTierId), appDynamicsConfig, dataCollectionStartTimeStamp,
        analyzedTierAnalysisType, delegateTasks);

    for (int i = 0; i < dependentTiers.size(); i++) {
      waitIds[i + 1] = createDelegateTask(context, Collections.emptyMap(), envId, finalApplicationId,
          dependentTiers.get(i).getId(), appDynamicsConfig, dataCollectionStartTimeStamp, PREDICTIVE, delegateTasks);
      metricGroups.put(dependentTiers.get(i).getName(),
          TimeSeriesMlAnalysisGroupInfo.builder()
              .groupName(dependentTiers.get(i).getName())
              .dependencyPath(dependentTiers.get(i).getDependencyPath())
              .mlAnalysisType(PREDICTIVE)
              .build());
    }
    waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), executionData, false), waitIds);
    InfrastructureMapping infrastructureMapping = getInfrastructureMapping(context);
    DeploymentType deploymentType =
        serviceResourceService.getDeploymentType(infrastructureMapping, null, infrastructureMapping.getServiceId());

    if (DeploymentType.HELM.equals(deploymentType)) {
      super.createAndSaveMetricGroups(context, hosts);
    } else {
      metricAnalysisService.saveMetricGroups(
          context.getAppId(), StateType.APP_DYNAMICS, context.getStateExecutionInstanceId(), metricGroups);
    }
    List<String> delegateTaskIds = new ArrayList<>();
    for (DelegateTask task : delegateTasks) {
      delegateTaskIds.add(delegateService.queueTask(task));
    }
    return StringUtils.join(delegateTaskIds, ",");
  }

  private String createDelegateTask(ExecutionContext context, Map<String, String> hosts, String envId,
      String finalApplicationId, long finalTierId, AppDynamicsConfig appDynamicsConfig,
      long dataCollectionStartTimeStamp, TimeSeriesMlAnalysisType mlAnalysisType, List<DelegateTask> delegateTasks) {
    final AppdynamicsDataCollectionInfo dataCollectionInfo =
        AppdynamicsDataCollectionInfo.builder()
            .appDynamicsConfig(appDynamicsConfig)
            .applicationId(context.getAppId())
            .stateExecutionId(context.getStateExecutionInstanceId())
            .workflowId(getWorkflowId(context))
            .workflowExecutionId(context.getWorkflowExecutionId())
            .serviceId(getPhaseServiceId(context))
            .startTime(dataCollectionStartTimeStamp)
            .collectionTime(Integer.parseInt(getTimeDuration()))
            .appId(Long.parseLong(finalApplicationId))
            .tierId(finalTierId)
            .dataCollectionMinute(0)
            .encryptedDataDetails(secretManager.getEncryptionDetails(
                appDynamicsConfig, context.getAppId(), context.getWorkflowExecutionId()))
            .hosts(hosts)
            .timeSeriesMlAnalysisType(mlAnalysisType)
            .build();

    String waitId = generateUuid();
    delegateTasks.add(DelegateTask.builder()
                          .async(true)
                          .taskType(TaskType.APPDYNAMICS_COLLECT_METRIC_DATA.name())
                          .accountId(appService.get(context.getAppId()).getAccountId())
                          .appId(context.getAppId())
                          .waitId(waitId)
                          .data(TaskData.builder()
                                    .parameters(new Object[] {dataCollectionInfo})
                                    .timeout(TimeUnit.MINUTES.toMillis(Integer.parseInt(getTimeDuration()) + 120))
                                    .build())
                          .envId(envId)
                          .build());
    return waitId;
  }

  @Override
  protected void createAndSaveMetricGroups(ExecutionContext context, Map<String, String> hostsToCollect) {
    if (!isEmpty(dependentTiersToAnalyze)) {
      InfrastructureMapping infrastructureMapping = getInfrastructureMapping(context);
      if (DeploymentType.HELM.equals(serviceResourceService.getDeploymentType(
              infrastructureMapping, null, infrastructureMapping.getServiceId()))) {
        throw new WingsException("can not analyze dependent tiers for helm type deployment");
      }
    }
  }

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

  public List<AppdynamicsTier> getDependentTiersToAnalyze() {
    return dependentTiersToAnalyze;
  }

  public void setDependentTiersToAnalyze(List<AppdynamicsTier> dependentTiersToAnalyze) {
    this.dependentTiersToAnalyze = dependentTiersToAnalyze;
  }

  @Override
  public Map<String, String> parentTemplateFields(String fieldName) {
    Map<String, String> parentTemplateFields = new LinkedHashMap<>();
    if (fieldName.equals("applicationId")) {
      if (!configIdTemplatized()) {
        parentTemplateFields.put("analysisServerConfigId", analysisServerConfigId);
      }
    } else if (fieldName.equals("tierId")) {
      if (!configIdTemplatized()) {
        parentTemplateFields.put("analysisServerConfigId", analysisServerConfigId);
        if (!appIdTemplatized()) {
          parentTemplateFields.put("applicationId", applicationId);
        }
      }
    }
    return parentTemplateFields;
  }

  @Attributes(required = false, title = "Expression for Host/Container name")
  public String getHostnameTemplate() {
    return hostnameTemplate;
  }

  public void setHostnameTemplate(String hostnameTemplate) {
    this.hostnameTemplate = hostnameTemplate;
  }

  private boolean appIdTemplatized() {
    return TemplateExpressionProcessor.checkFieldTemplatized("applicationId", getTemplateExpressions());
  }

  private boolean configIdTemplatized() {
    return TemplateExpressionProcessor.checkFieldTemplatized("analysisServerConfigId", getTemplateExpressions());
  }

  public static Double getNormalizedValue(String metricName, NewRelicMetricDataRecord metricDataRecord) {
    if (metricDataRecord != null) {
      if (metricDataRecord.getValues().containsKey(AppdynamicsTimeSeries.CALLS_PER_MINUTE.getMetricName())
          && AppdynamicsTimeSeries.getErrorMetrics().contains(metricName)) {
        double errorCount = metricDataRecord.getValues().get(metricName);
        double callsCount = metricDataRecord.getValues().get(AppdynamicsTimeSeries.CALLS_PER_MINUTE.getMetricName());

        if (callsCount != 0.0) {
          DecimalFormat twoDForm = new DecimalFormat("#.00");
          return Double.valueOf(twoDForm.format(errorCount / callsCount * 100));
        } else {
          return 0.0;
        }
      }
      return metricDataRecord.getValues().get(metricName);
    }
    return null;
  }

  public static String formDeeplinkUrl(AppDynamicsConfig appDconfig, AppDynamicsCVServiceConfiguration cvConfig,
      long startTime, long endTime, String metricString) {
    String url = appDconfig.getControllerUrl().endsWith("/") ? appDconfig.getControllerUrl()
                                                             : appDconfig.getControllerUrl() + "/";
    return url
        + APPDYNAMICS_DEEPLINK_FORMAT.replace("{applicationId}", cvConfig.getAppDynamicsApplicationId())
              .replace("{metricString}", metricString)
              .replace("{startTimeMs}", String.valueOf(startTime))
              .replace("{endTimeMs}", String.valueOf(endTime));
  }

  public static String getMetricTypeForMetric(String metricName) {
    if (isEmpty(metricName)) {
      return null;
    }
    Map<String, TimeSeriesMetricDefinition> appDMetrics = new HashMap<>(APP_DYNAMICS_VALUES_TO_ANALYZE);
    appDMetrics.putAll(APP_DYNAMICS_24X7_VALUES_TO_ANALYZE);

    if (appDMetrics.containsKey(metricName)) {
      return appDMetrics.get(metricName).getMetricType().name();
    }
    logger.error("Invalid metricName in AppDynamics {}", metricName);
    return null;
  }
}