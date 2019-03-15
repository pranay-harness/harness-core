package software.wings.service.impl.analysis;

import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeCount;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static java.util.Collections.emptySet;
import static software.wings.beans.DelegateTask.DEFAULT_SYNC_CALL_TIMEOUT;
import static software.wings.common.VerificationConstants.APPDYNAMICS_DEEPLINK_FORMAT;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.common.VerificationConstants.CV_24x7_STATE_EXECUTION;
import static software.wings.common.VerificationConstants.DUMMY_HOST_NAME;
import static software.wings.common.VerificationConstants.ERROR_METRIC_NAMES;
import static software.wings.common.VerificationConstants.GLOBAL_APP_ID;
import static software.wings.common.VerificationConstants.HEARTBEAT_METRIC_NAME;
import static software.wings.common.VerificationConstants.NEW_RELIC_DEEPLINK_FORMAT;
import static software.wings.common.VerificationConstants.PROMETHEUS_DEEPLINK_FORMAT;
import static software.wings.service.impl.newrelic.NewRelicMetricDataRecord.DEFAULT_GROUP_NAME;
import static software.wings.sm.states.AbstractLogAnalysisState.HOST_BATCH_SIZE;
import static software.wings.sm.states.DatadogState.metricEndpointsInfo;
import static software.wings.verification.TimeSeriesDataPoint.initializeTimeSeriesDataPointsList;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.delegate.beans.TaskData;
import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import io.harness.waiter.WaitNotifyEngine;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.APMFetchConfig;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.MetricDataAnalysisResponse;
import software.wings.beans.APMValidateCollectorConfig;
import software.wings.beans.APMVerificationConfig;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.AwsConfig;
import software.wings.beans.DatadogConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.DynaTraceConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.PrometheusConfig;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SumoConfig;
import software.wings.beans.SyncTaskContext;
import software.wings.beans.TaskType;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.common.VerificationConstants;
import software.wings.delegatetasks.DataCollectionExecutorService;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.appdynamics.AppdynamicsConstants;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.GoogleDataStoreServiceImpl;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse.VerificationLoadResponse;
import software.wings.service.impl.apm.APMDataCollectionInfo;
import software.wings.service.impl.apm.APMMetricInfo;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionInfo;
import software.wings.service.impl.cloudwatch.CloudWatchDataCollectionInfo;
import software.wings.service.impl.datadog.DataDogFetchConfig;
import software.wings.service.impl.dynatrace.DynaTraceDataCollectionInfo;
import software.wings.service.impl.dynatrace.DynaTraceTimeSeries;
import software.wings.service.impl.elk.ElkDataCollectionInfo;
import software.wings.service.impl.newrelic.MetricAnalysisExecutionData;
import software.wings.service.impl.newrelic.NewRelicDataCollectionInfo;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.impl.prometheus.PrometheusDataCollectionInfo;
import software.wings.service.impl.sumo.SumoDataCollectionInfo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.CloudWatchService;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.verification.CV24x7DashboardService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.settings.SettingValue;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateType;
import software.wings.sm.states.AppDynamicsState;
import software.wings.sm.states.DatadogState;
import software.wings.sm.states.DynatraceState;
import software.wings.sm.states.NewRelicState;
import software.wings.verification.CVConfiguration;
import software.wings.verification.HeatMap;
import software.wings.verification.HeatMapResolution;
import software.wings.verification.TimeSeriesOfMetric;
import software.wings.verification.TransactionTimeSeries;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.cloudwatch.CloudWatchCVServiceConfiguration;
import software.wings.verification.dashboard.HeatMapUnit;
import software.wings.verification.datadog.DatadogCVServiceConfiguration;
import software.wings.verification.dynatrace.DynaTraceCVServiceConfiguration;
import software.wings.verification.log.ElkCVConfiguration;
import software.wings.verification.log.LogsCVConfiguration;
import software.wings.verification.newrelic.NewRelicCVServiceConfiguration;
import software.wings.verification.prometheus.PrometheusCVServiceConfiguration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
public class ContinuousVerificationServiceImpl implements ContinuousVerificationService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AuthService authService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private AppService appService;
  @Inject private CVConfigurationService cvConfigurationService;
  @Inject private DataStoreService dataStoreService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private SettingsService settingsService;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private DelegateService delegateService;
  @Inject private CloudWatchService cloudWatchService;
  @Inject private CV24x7DashboardService cv24x7DashboardService;
  @Inject private DataCollectionExecutorService dataCollectionService;

  private static final Logger logger = LoggerFactory.getLogger(ContinuousVerificationServiceImpl.class);

  private static final int PAGE_LIMIT = 999;
  private static final int START_OFFSET = 0;
  private static final String DATE_PATTERN = "yyyy-MM-dd HH:MM";

  @Override
  public void saveCVExecutionMetaData(ContinuousVerificationExecutionMetaData continuousVerificationExecutionMetaData) {
    wingsPersistence.save(continuousVerificationExecutionMetaData);
  }

  @Override
  public void setMetaDataExecutionStatus(String stateExecutionId, ExecutionStatus status) {
    Query<ContinuousVerificationExecutionMetaData> query =
        wingsPersistence.createQuery(ContinuousVerificationExecutionMetaData.class)
            .filter("stateExecutionId", stateExecutionId);

    wingsPersistence.update(query,
        wingsPersistence.createUpdateOperations(ContinuousVerificationExecutionMetaData.class)
            .set("executionStatus", status));
  }

  @Override
  public LinkedHashMap<Long,
      LinkedHashMap<String,
          LinkedHashMap<String,
              LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>>
  getCVExecutionMetaData(String accountId, long beginEpochTs, long endEpochTs, final User user) throws ParseException {
    LinkedHashMap<Long,
        LinkedHashMap<String,
            LinkedHashMap<String,
                LinkedHashMap<String, LinkedHashMap<String, List<ContinuousVerificationExecutionMetaData>>>>>> results =
        new LinkedHashMap<>();
    if (user == null) {
      // user is null, we can't validate permissions. Returning empty.
      logger.warn("Returning empty results from getCVExecutionMetaData since user was null");
      return results;
    }
    if (isEmpty(getAllowedApplicationsForUser(user, accountId))) {
      logger.info(
          "Returning empty results from getCVExecutionMetaData since user does not have permissions for any applications");
      return results;
    }
    PageRequest<ContinuousVerificationExecutionMetaData> request = PageRequestBuilder.aPageRequest()
                                                                       .addFilter("accountId", Operator.EQ, accountId)
                                                                       .addOrder("workflowStartTs", OrderType.DESC)
                                                                       .addOrder("stateStartTs", OrderType.ASC)
                                                                       .withLimit("500")
                                                                       .withOffset("0")
                                                                       .build();
    request.addFilter("workflowStartTs", Operator.GE, beginEpochTs);
    request.addFilter("workflowStartTs", Operator.LT, endEpochTs);
    request.addFilter("applicationId", Operator.IN, getAllowedApplicationsForUser(user, accountId).toArray());
    int previousOffSet = 0;
    List<ContinuousVerificationExecutionMetaData> continuousVerificationExecutionMetaData = new ArrayList<>();
    PageResponse<ContinuousVerificationExecutionMetaData> response =
        wingsPersistence.query(ContinuousVerificationExecutionMetaData.class, request);
    while (!response.isEmpty()) {
      continuousVerificationExecutionMetaData.addAll(response.getResponse());
      previousOffSet += response.size();
      request.setOffset(String.valueOf(previousOffSet));
      response = wingsPersistence.query(ContinuousVerificationExecutionMetaData.class, request);
    }

    Map<String, Long> pipelineTimeStampMap = new HashMap<>();

    for (ContinuousVerificationExecutionMetaData executionMetaData : continuousVerificationExecutionMetaData) {
      if (executionMetaData.getPipelineExecutionId() != null) {
        if (!pipelineTimeStampMap.containsKey(executionMetaData.getPipelineExecutionId())) {
          pipelineTimeStampMap.put(executionMetaData.getPipelineExecutionId(), executionMetaData.getWorkflowStartTs());
        } else if (executionMetaData.getWorkflowStartTs()
            > pipelineTimeStampMap.get(executionMetaData.getPipelineExecutionId())) {
          pipelineTimeStampMap.put(executionMetaData.getPipelineExecutionId(), executionMetaData.getWorkflowStartTs());
        }
      }
    }

    Long startTimeTs;

    continuousVerificationExecutionMetaData =
        validatePermissionsAndGetAllowedExecutionList(user, accountId, continuousVerificationExecutionMetaData);
    for (ContinuousVerificationExecutionMetaData executionMetaData : continuousVerificationExecutionMetaData) {
      String pipeLineId = executionMetaData.getPipelineId();
      if (pipeLineId != null && pipelineTimeStampMap.containsKey(pipeLineId)) {
        startTimeTs = pipelineTimeStampMap.get(pipeLineId);
      } else {
        startTimeTs = executionMetaData.getWorkflowStartTs();
      }
      startTimeTs = Instant.ofEpochMilli(startTimeTs).truncatedTo(ChronoUnit.DAYS).toEpochMilli();
      if (!results.containsKey(startTimeTs)) {
        results.put(startTimeTs, new LinkedHashMap<>());
      }

      if (!results.get(startTimeTs).containsKey(executionMetaData.getArtifactName())) {
        results.get(startTimeTs).put(executionMetaData.getArtifactName(), new LinkedHashMap<>());
      }

      String envWorkflowName = executionMetaData.getEnvName() + "/" + executionMetaData.getWorkflowName();
      if (!results.get(startTimeTs).get(executionMetaData.getArtifactName()).containsKey(envWorkflowName)) {
        results.get(startTimeTs).get(executionMetaData.getArtifactName()).put(envWorkflowName, new LinkedHashMap<>());
      }

      if (!results.get(startTimeTs)
               .get(executionMetaData.getArtifactName())
               .get(envWorkflowName)
               .containsKey(executionMetaData.getWorkflowExecutionId())) {
        results.get(startTimeTs)
            .get(executionMetaData.getArtifactName())
            .get(envWorkflowName)
            .put(executionMetaData.getWorkflowExecutionId(), new LinkedHashMap<>());
      }

      String phaseName = executionMetaData.getPhaseName() == null ? "BASIC" : executionMetaData.getPhaseName();

      if (!results.get(startTimeTs)
               .get(executionMetaData.getArtifactName())
               .get(envWorkflowName)
               .get(executionMetaData.getWorkflowExecutionId())
               .containsKey(phaseName)) {
        results.get(startTimeTs)
            .get(executionMetaData.getArtifactName())
            .get(envWorkflowName)
            .get(executionMetaData.getWorkflowExecutionId())
            .put(phaseName, new ArrayList<>());
      }
      results.get(startTimeTs)
          .get(executionMetaData.getArtifactName())
          .get(envWorkflowName)
          .get(executionMetaData.getWorkflowExecutionId())
          .get(phaseName)
          .add(executionMetaData);
    }

    return results;
  }

  @Override
  public PageResponse<ContinuousVerificationExecutionMetaData> getAllCVExecutionsForTime(final String accountId,
      long beginEpochTs, long endEpochTs, boolean isTimeSeries,
      PageRequest<ContinuousVerificationExecutionMetaData> pageRequestFromUI) {
    // TODO: Move this accountId check to Rbac
    if (!featureFlagService.isEnabled(FeatureName.GLOBAL_CV_DASH, accountId)) {
      return new PageResponse<>();
    }
    PageRequest<ContinuousVerificationExecutionMetaData> pageRequest =
        PageRequestBuilder.aPageRequest().withOffset(pageRequestFromUI.getOffset()).build();
    if (beginEpochTs < 0 || endEpochTs < 0) {
      // if there's no start/end, we will default to 7 days
      beginEpochTs = Timestamp.currentMinuteBoundary() - TimeUnit.DAYS.toMillis(7);
      endEpochTs = Timestamp.currentMinuteBoundary();
    }
    List<StateType> stateTypeList;
    if (isTimeSeries) {
      stateTypeList = VerificationConstants.getMetricAnalysisStates();
    } else {
      stateTypeList = VerificationConstants.getLogAnalysisStates();
    }

    pageRequest.addFilter("stateType", Operator.IN, stateTypeList.toArray());
    pageRequest.addFilter("workflowStartTs", Operator.GE, beginEpochTs);
    pageRequest.addFilter("workflowStartTs", Operator.LT, endEpochTs);
    pageRequest.setFieldsIncluded(Arrays.asList("stateExecutionId", "workflowExecutionId", "envId", "serviceId",
        "accountId", "executionStatus", "applicationId", "workflowStartTs", "stateType"));
    pageRequest.addOrder("workflowStartTs", OrderType.DESC);
    pageRequest.addOrder("stateStartTs", OrderType.DESC);

    return wingsPersistence.query(ContinuousVerificationExecutionMetaData.class, pageRequest);
  }

  @Override
  public List<CVDeploymentData> getCVDeploymentData(
      String accountId, long startTime, long endTime, User user, String serviceId) {
    List<CVDeploymentData> results = new ArrayList<>();
    if (user == null) {
      // user is null, we can't validate permissions. Returning empty.
      logger.warn("Returning empty results from getCVDeploymentData since user was null");
      return results;
    }
    List<String> allowedApplications = getAllowedApplicationsForUser(user, accountId);
    if (isEmpty(allowedApplications)) {
      logger.info(
          "Returning empty results from getCVDeploymentData since user does not have permissions for any applications");
      return results;
    }

    PageRequest<ContinuousVerificationExecutionMetaData> request = PageRequestBuilder.aPageRequest()
                                                                       .addFilter("accountId", Operator.EQ, accountId)
                                                                       .addFilter("serviceId", Operator.EQ, serviceId)
                                                                       .addOrder("workflowStartTs", OrderType.DESC)
                                                                       .addOrder("stateStartTs", OrderType.ASC)
                                                                       .withLimit("500")
                                                                       .withOffset("0")
                                                                       .build();
    request.addFilter("workflowStartTs", Operator.GE, startTime);
    request.addFilter("workflowStartTs", Operator.LT, endTime);

    int previousOffSet = 0;
    List<ContinuousVerificationExecutionMetaData> continuousVerificationExecutionMetaData = new ArrayList<>();
    PageResponse<ContinuousVerificationExecutionMetaData> response =
        wingsPersistence.query(ContinuousVerificationExecutionMetaData.class, request);
    while (!response.isEmpty()) {
      continuousVerificationExecutionMetaData.addAll(response.getResponse());
      previousOffSet += response.size();
      request.setOffset(String.valueOf(previousOffSet));
      response = wingsPersistence.query(ContinuousVerificationExecutionMetaData.class, request);
    }

    Map<String, CVDeploymentData> deploymentData = new HashMap<>();
    for (ContinuousVerificationExecutionMetaData cvData : continuousVerificationExecutionMetaData) {
      if (!deploymentData.containsKey(cvData.getWorkflowExecutionId())) {
        deploymentData.put(cvData.getWorkflowExecutionId(), new CVDeploymentData(cvData));
      }
    }

    if (isEmpty(deploymentData)) {
      logger.info("There are no deployments with CV for service {}", serviceId);
      return new ArrayList<>();
    }
    // find the statuses of all the workflows we have.
    PageRequest<WorkflowExecution> workflowExecutionPageRequest =
        PageRequestBuilder.aPageRequest()
            .addFilter("_id", Operator.IN, deploymentData.keySet().toArray())
            .addFieldsIncluded("_id", "status", "startTs", "endTs", "name", "pipelineSummary")
            .withLimit("500")
            .withOffset("0")
            .build();
    previousOffSet = 0;
    List<WorkflowExecution> workflowExecutionList = new ArrayList<>();
    PageResponse<WorkflowExecution> workflowExecutionResponse =
        wingsPersistence.query(WorkflowExecution.class, workflowExecutionPageRequest);
    while (!workflowExecutionResponse.isEmpty()) {
      workflowExecutionList.addAll(workflowExecutionResponse.getResponse());
      previousOffSet += workflowExecutionResponse.size();
      workflowExecutionPageRequest.setOffset(String.valueOf(previousOffSet));
      workflowExecutionResponse = wingsPersistence.query(WorkflowExecution.class, workflowExecutionPageRequest);
    }

    for (WorkflowExecution execution : workflowExecutionList) {
      deploymentData.get(execution.getUuid()).setStatus(execution.getStatus());
      deploymentData.get(execution.getUuid()).setWorkflowName(execution.normalizedName());
      PipelineSummary ps = execution.getPipelineSummary();
      if (ps != null) {
        deploymentData.get(execution.getUuid()).setPipelineName(ps.getPipelineName());
      }
    }
    return new ArrayList(deploymentData.values());
  }

  @Override
  public List<WorkflowExecution> getDeploymentsForService(
      String accountId, long startTime, long endTime, User user, String serviceId) {
    List<WorkflowExecution> results = new ArrayList<>();
    if (user == null) {
      // user is null, we can't validate permissions. Returning empty.
      logger.warn("Returning empty results from getCVDeploymentData since user was null");
      return results;
    }
    List<String> allowedApplications = getAllowedApplicationsForUser(user, accountId);
    Service service = wingsPersistence.get(Service.class, serviceId);
    if (isEmpty(allowedApplications) || service == null || !allowedApplications.contains(service.getAppId())) {
      logger.info(
          "Returning empty results from getCVDeploymentData since user {} does not have permissions for any applications",
          user);
      return results;
    }
    PageRequest<WorkflowExecution> workflowExecutionPageRequest =
        PageRequestBuilder.aPageRequest()
            .addFilter("appId", Operator.EQ, service.getAppId())
            .addFieldsExcluded(
                "serviceExecutionSummaries", "executionArgs", "keywords", "breakdown", "statusInstanceBreakdownMap")
            .addFilter("startTs", Operator.GE, startTime)
            .addFilter("startTs", Operator.LT, endTime)
            .withLimit(String.valueOf(PAGE_LIMIT))
            .withOffset(String.valueOf(START_OFFSET))
            .build();

    int previousOffSet = 0;

    PageResponse<WorkflowExecution> workflowExecutionResponse =
        wingsPersistence.query(WorkflowExecution.class, workflowExecutionPageRequest);
    while (!workflowExecutionResponse.isEmpty()) {
      results.addAll(workflowExecutionResponse.getResponse());
      previousOffSet += workflowExecutionResponse.size();
      workflowExecutionPageRequest.setOffset(String.valueOf(previousOffSet));
      workflowExecutionResponse = wingsPersistence.query(WorkflowExecution.class, workflowExecutionPageRequest);
    }

    List<WorkflowExecution> resultList = new ArrayList<>();
    results.forEach(workflowExecution -> {
      if (workflowExecution.getServiceIds() != null && workflowExecution.getServiceIds().contains(serviceId)) {
        resultList.add(workflowExecution);
      }
    });
    return resultList;
  }

  /**
   * Check if the user has permissions to view the executionData.
   *
   * @param user
   * @param accountId
   * @param executionMetaDataList
   * @return true if user has all the required permissions, false otherwise.
   */
  private List<ContinuousVerificationExecutionMetaData> validatePermissionsAndGetAllowedExecutionList(final User user,
      final String accountId, final List<ContinuousVerificationExecutionMetaData> executionMetaDataList) {
    List<ContinuousVerificationExecutionMetaData> finalList = new ArrayList<>();
    Map<String, AppPermissionSummary> userAppPermissions =
        authService.getUserPermissionInfo(accountId, user).getAppPermissionMapInternal();
    //"Cache" it by applicationId.
    Map<String, Set<String>> servicePermissionsByApp = new HashMap<>();
    Map<String, Set<String>> envPermissionsByApp = new HashMap<>();

    for (ContinuousVerificationExecutionMetaData executionMetaData : executionMetaDataList) {
      final String applicationId = executionMetaData.getApplicationId();
      Set<String> servicePermissions, pipelinePermissions, wfPermissions, envPermissions;

      if (!servicePermissionsByApp.containsKey(applicationId)) {
        // If it's  not present for servicePermissions,it's not present for anything. So fill up the map.
        servicePermissionsByApp.put(
            applicationId, userAppPermissions.get(applicationId).getServicePermissions().get(Action.READ));
        envPermissionsByApp.put(applicationId, getEnvPermissions(userAppPermissions, applicationId));
      }
      servicePermissions = servicePermissionsByApp.get(applicationId);
      envPermissions = envPermissionsByApp.get(applicationId);
      logger.info("Service permissions for user {} are {}", user.getName(), servicePermissions);
      logger.info("environment permissions for user {} are {}", user.getName(), envPermissions);

      if (checkIfPermissionsApproved(servicePermissions, executionMetaData.getServiceId())
          && checkIfPermissionsApproved(envPermissions, executionMetaData.getEnvId())) {
        finalList.add(executionMetaData);
      } else {
        logger.info("User {} does not have permissions to view the execution data {} and {} and {} and {}",
            user.getName(), executionMetaData.getServiceName(), executionMetaData.getWorkflowName(),
            executionMetaData.getEnvName(), executionMetaData.getPipelineName());
        logger.info("User {} does not have permissions to view the execution data {} and {} and {} and {}",
            user.getName(), executionMetaData.getServiceId(), executionMetaData.getWorkflowId(),
            executionMetaData.getEnvId(), executionMetaData.getPipelineId());
      }
    }
    return finalList;
  }

  private Set<String> getEnvPermissions(Map<String, AppPermissionSummary> userAppPermissions, String applicationId) {
    if (isEmpty(userAppPermissions)) {
      return emptySet();
    }

    AppPermissionSummary appPermissionSummary = userAppPermissions.get(applicationId);

    if (appPermissionSummary == null) {
      return emptySet();
    }

    Map<Action, Set<EnvInfo>> envPermissions = appPermissionSummary.getEnvPermissions();

    if (isEmpty(envPermissions)) {
      return emptySet();
    }

    Set<EnvInfo> envInfoSet = envPermissions.get(Action.READ);
    if (isEmpty(envInfoSet)) {
      return emptySet();
    }

    return envInfoSet.stream().map(envInfo -> envInfo.getEnvId()).collect(Collectors.toSet());
  }

  /**
   * @param setToCheck
   * @param value
   * @return False if set is either empty or it does not contain value. True otherwise.
   */
  private boolean checkIfPermissionsApproved(final Set<String> setToCheck, final String value) {
    if (isEmpty(value)) {
      return true;
    }

    if (isEmpty(setToCheck) || !setToCheck.contains(value)) {
      logger.info("Permissions rejected for value {} in set {}", value, setToCheck);
      return false;
    }
    return true;
  }

  private List<String> getAllowedApplicationsForUser(final User user, final String accountId) {
    Map<String, AppPermissionSummary> userApps =
        authService.getUserPermissionInfo(accountId, user).getAppPermissionMapInternal();
    return new ArrayList<>(userApps.keySet());
  }

  @Override
  public List<HeatMap> getHeatMap(
      String accountId, String appId, String serviceId, long startTime, long endTime, boolean detailed) {
    List<HeatMap> rv = Collections.synchronizedList(new ArrayList<>());
    List<CVConfiguration> cvConfigurations = wingsPersistence.createQuery(CVConfiguration.class)
                                                 .filter("appId", appId)
                                                 .filter("serviceId", serviceId)
                                                 .asList();

    if (isEmpty(cvConfigurations)) {
      logger.info("No cv config found for appId={}, serviceId={}", appId, serviceId);
      return new ArrayList<>();
    }

    List<Callable<Void>> callables = new ArrayList<>();
    cvConfigurations.stream()
        .filter(cvConfig -> !VerificationConstants.getLogAnalysisStates().contains(cvConfig.getStateType()))
        .forEach(cvConfig -> callables.add(() -> {
          cvConfigurationService.fillInServiceAndConnectorNames(cvConfig);
          String envName = cvConfig.getEnvName();
          logger.info("Environment name = " + envName);
          final HeatMap heatMap = HeatMap.builder().cvConfiguration(cvConfig).build();
          rv.add(heatMap);

          List<HeatMapUnit> units = createAllHeatMapUnits(appId, startTime, endTime, cvConfig);
          List<HeatMapUnit> resolvedUnits = resolveHeatMapUnits(units, startTime, endTime);
          heatMap.getRiskLevelSummary().addAll(resolvedUnits);
          return null;
        }));
    dataCollectionService.executeParrallel(callables);

    rv.addAll(cv24x7DashboardService.getHeatMapForLogs(accountId, appId, serviceId, startTime, endTime, detailed));

    return rv;
  }

  /**
   *
   * @param units - List of heat map units with the smallest possible size (currently = 1 cron job interval)
   * @param startTime - in ms
   * @param endTime - in ms
   * @return - List of heatmap units based on resolution determined by startTime, endTime
   */
  private List<HeatMapUnit> resolveHeatMapUnits(List<HeatMapUnit> units, long startTime, long endTime) {
    List<HeatMapUnit> resolvedUnits = new ArrayList<>();
    HeatMapResolution heatMapResolution = HeatMapResolution.getResolution(startTime, endTime);

    // time duration represented by each read unit
    int unitDuration = heatMapResolution.getDurationOfHeatMapUnit(heatMapResolution);

    // number of small units to be merged into one reqd unit
    int eventsPerUnit = heatMapResolution.getEventsPerHeatMapUnit(heatMapResolution);

    // total number of read units
    int numberOfUnits = (int) ceil((double) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime) / unitDuration);

    logger.info("total small units = {}, number of required units = {}", units.size(), numberOfUnits);

    for (int i = 0; i < numberOfUnits; i++) {
      // merge [i * eventsPerUnit, (i + 1) * eventsPerUnit)
      // [x, y) denotes x inclusive, y exclusive
      // Note: This works because the smallest unit is composed of exactly 1 event
      int startIndex = i * eventsPerUnit;
      int endIndex = min((i + 1) * eventsPerUnit, units.size());

      if (startIndex >= endIndex) {
        continue;
      }
      List<HeatMapUnit> subList = units.subList(startIndex, endIndex);
      if (subList.size() > 0) {
        resolvedUnits.add(merge(subList));
      }
    }
    return resolvedUnits;
  }

  private HeatMapUnit merge(List<HeatMapUnit> units) {
    HeatMapUnit mergedUnit = HeatMapUnit.builder()
                                 .startTime(units.get(0).getStartTime())
                                 .endTime(units.get(units.size() - 1).getEndTime())
                                 .overallScore(-2)
                                 .build();
    units.forEach(unit -> {
      if (unit.getScoreList() != null) {
        mergedUnit.updateOverallScore(unit.getOverallScore());
      }
    });

    return mergedUnit;
  }

  private List<HeatMapUnit> createAllHeatMapUnits(
      String appId, long startTime, long endTime, CVConfiguration cvConfiguration) {
    long cronPollIntervalMs = TimeUnit.MINUTES.toMillis(CRON_POLL_INTERVAL_IN_MINUTES);
    Preconditions.checkState((endTime - startTime) >= cronPollIntervalMs);
    List<TimeSeriesMLAnalysisRecord> records =
        getAnalysisRecordsInTimeRange(appId, startTime, endTime, cvConfiguration);

    long startMinute = TimeUnit.MILLISECONDS.toMinutes(startTime);
    long endMinute = TimeUnit.MILLISECONDS.toMinutes(endTime);

    List<HeatMapUnit> units = new ArrayList<>();
    if (isEmpty(records)) {
      while (endMinute > startMinute) {
        units.add(HeatMapUnit.builder()
                      .startTime(TimeUnit.MINUTES.toMillis(startMinute))
                      .endTime(TimeUnit.MINUTES.toMillis(startMinute + CRON_POLL_INTERVAL_IN_MINUTES))
                      .na(1)
                      .overallScore(-2)
                      .build());
        startMinute += CRON_POLL_INTERVAL_IN_MINUTES;
      }

      return units;
    }

    List<HeatMapUnit> unitsFromDB = new ArrayList<>();
    records.forEach(record -> {
      HeatMapUnit heatMapUnit =
          HeatMapUnit.builder()
              .startTime(TimeUnit.MINUTES.toMillis(record.getAnalysisMinute() - CRON_POLL_INTERVAL_IN_MINUTES) + 1)
              .endTime(TimeUnit.MINUTES.toMillis(record.getAnalysisMinute()))
              .overallScore(-2)
              .build();

      heatMapUnit.updateOverallScore(record.getOverallMetricScores());
      unitsFromDB.add(heatMapUnit);
    });

    // find the actual start time so that we fill from there
    HeatMapUnit heatMapUnit = unitsFromDB.get(0);
    long actualUnitStartTime = heatMapUnit.getStartTime();
    while (startTime < actualUnitStartTime - cronPollIntervalMs) {
      actualUnitStartTime -= cronPollIntervalMs;
    }

    int dbUnitIndex = 0;
    for (long unitTime = actualUnitStartTime; unitTime <= endTime; unitTime += cronPollIntervalMs) {
      heatMapUnit = dbUnitIndex < unitsFromDB.size() ? unitsFromDB.get(dbUnitIndex) : null;
      if (heatMapUnit != null) {
        long timeDifference = TimeUnit.MILLISECONDS.toSeconds(abs(heatMapUnit.getStartTime() - unitTime));
        if (timeDifference != 0 && timeDifference < 60) {
          logger.error(
              "Unexpected state: timeDifference = {}, should have been 0 or > 60, heatmap unit start time = {}",
              timeDifference, heatMapUnit.getStartTime());
        }
      }

      if (heatMapUnit != null && unitTime == heatMapUnit.getStartTime()) {
        units.add(heatMapUnit);
        dbUnitIndex++;
        continue;
      }

      units.add(HeatMapUnit.builder().endTime(unitTime - 1).startTime(unitTime - cronPollIntervalMs).na(1).build());
    }
    return units;
  }

  @NotNull
  private List<TimeSeriesMLAnalysisRecord> getAnalysisRecordsInTimeRange(
      String appId, long startTime, long endTime, CVConfiguration cvConfiguration) {
    final List<TimeSeriesMLAnalysisRecord> timeSeriesMLAnalysisRecords =
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class, excludeCount)
            .filter("appId", appId)
            .filter("cvConfigId", cvConfiguration.getUuid())
            .field("analysisMinute")
            .greaterThanOrEq(TimeUnit.MILLISECONDS.toMinutes(startTime))
            .field("analysisMinute")
            .lessThanOrEq(TimeUnit.MILLISECONDS.toMinutes(endTime))
            .order("analysisMinute")
            .project("transactions", false)
            .project("transactionsCompressedJson", false)
            .asList();
    timeSeriesMLAnalysisRecords.forEach(
        timeSeriesMLAnalysisRecord -> timeSeriesMLAnalysisRecord.decompressTransactions());
    return timeSeriesMLAnalysisRecords;
  }

  @NotNull
  public Map<String, Map<String, TimeSeriesOfMetric>> fetchObservedTimeSeries(
      long startTime, long endTime, CVConfiguration cvConfiguration, long historyStartTime) {
    // 1. Get time series for the entire duration from historyStartTime to endTime
    // 2. Pass startTime as the riskCutOffTime as that's the starting point where we consider risk
    if (TimeUnit.MILLISECONDS.toDays(endTime - historyStartTime) > 30L) {
      historyStartTime = startTime - TimeUnit.HOURS.toMillis(2) + 1;
    }
    return getTimeSeriesForTimeRangeFromDataRecords(cvConfiguration,
        TimeSeriesFilter.builder().startTime(startTime).endTime(endTime).historyStartTime(historyStartTime).build());
  }

  private SettingValue getConnectorConfig(CVConfiguration cvConfiguration) {
    if (isNotEmpty(cvConfiguration.getConnectorId())) {
      return wingsPersistence.get(SettingAttribute.class, cvConfiguration.getConnectorId()).getValue();
    }
    return null;
  }

  private Double getNormalizedMetricValue(String metricName, NewRelicMetricDataRecord dataRecord) {
    switch (dataRecord.getStateType()) {
      case APP_DYNAMICS:
        return AppDynamicsState.getNormalizedValue(metricName, dataRecord);
      case NEW_RELIC:
        return NewRelicState.getNormalizedErrorMetric(metricName, dataRecord);
      default:
        return dataRecord.getValues().get(metricName);
    }
  }

  private String getDisplayNameOfMetric(String metricName) {
    if (ERROR_METRIC_NAMES.containsKey(metricName)) {
      return ERROR_METRIC_NAMES.get(metricName);
    }
    return metricName;
  }

  private String getMetricType(CVConfiguration cvConfig, String metricName) {
    switch (cvConfig.getStateType()) {
      case APP_DYNAMICS:
        return AppDynamicsState.getMetricTypeForMetric(metricName);
      case NEW_RELIC:
        return NewRelicState.getMetricTypeForMetric(metricName);
      default:
        logger.info("Unsupported stateType {} for deeplinking", cvConfig.getStateType());
        return null;
    }
  }
  private String getDeeplinkUrl(
      CVConfiguration cvConfig, SettingValue connectorConfig, long startTime, long endTime, String metricString) {
    switch (cvConfig.getStateType()) {
      case APP_DYNAMICS:
        AppDynamicsCVServiceConfiguration config = (AppDynamicsCVServiceConfiguration) cvConfig;
        String baseUrl = ((AppDynamicsConfig) connectorConfig).getControllerUrl();
        return baseUrl
            + APPDYNAMICS_DEEPLINK_FORMAT.replace("{startTimeMs}", String.valueOf(startTime))
                  .replace("{endTimeMs}", String.valueOf(endTime))
                  .replace("{applicationId}", config.getAppDynamicsApplicationId())
                  .replace("{metricString}", metricString);
      case NEW_RELIC:
        String newRelicAppId = ((NewRelicCVServiceConfiguration) cvConfig).getApplicationId();
        String newRelicAccountId = ((NewRelicConfig) connectorConfig).getNewRelicAccountId();
        if (isEmpty(newRelicAccountId)) {
          return "";
        }
        int durationInHours = (int) TimeUnit.MILLISECONDS.toHours(endTime - startTime);
        long endTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(endTime);
        return NEW_RELIC_DEEPLINK_FORMAT.replace("{accountId}", newRelicAccountId)
            .replace("{applicationId}", newRelicAppId)
            .replace("{duration}", String.valueOf(durationInHours))
            .replace("{endTime}", String.valueOf(endTimeSeconds));
      case PROMETHEUS:
        int durationInMinutes = (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String endDate = format.format(new Date(endTime));
        try {
          endDate = URLEncoder.encode(endDate, "UTF-8");
        } catch (UnsupportedEncodingException e) {
          logger.error("Unable to encode the time range : ", durationInMinutes);
          throw new WingsException(e);
        }
        String url = ((PrometheusConfig) connectorConfig).getUrl();
        return PROMETHEUS_DEEPLINK_FORMAT.replace("{baseUrl", url)
            .replace("{rangeInput}", String.valueOf(durationInMinutes))
            .replace("{endTime}", endDate)
            .replace("{metricString}", String.valueOf(metricString));
      default:
        logger.info("Unsupported stateType {} for deeplinking", cvConfig.getStateType());
        return "";
    }
  }

  private void updateRisksFromSummary(long startTime, long endTime, long riskCutOff, CVConfiguration cvConfiguration,
      Map<String, Map<String, TimeSeriesOfMetric>> observedTimeSeries) {
    List<TimeSeriesRiskSummary> riskSummaries = wingsPersistence.createQuery(TimeSeriesRiskSummary.class)
                                                    .filter("appId", cvConfiguration.getAppId())
                                                    .filter("cvConfigId", cvConfiguration.getUuid())
                                                    .field("analysisMinute")
                                                    .greaterThan(TimeUnit.MILLISECONDS.toMinutes(startTime))
                                                    .field("analysisMinute")
                                                    .lessThanOrEq(TimeUnit.MILLISECONDS.toMinutes(endTime))
                                                    .order("analysisMinute")
                                                    .asList();

    riskSummaries.forEach(summary -> summary.decompressMaps());

    for (TimeSeriesRiskSummary summary : riskSummaries) {
      observedTimeSeries.forEach((transaction, metricMap) -> {
        // TODO: Remove these two loops.
        metricMap.entrySet()
            .stream()
            .filter(e
                -> isNotEmpty(summary.getTxnMetricRisk()) && summary.getTxnMetricRisk().containsKey(transaction)
                    && summary.getTxnMetricRisk().get(transaction).containsKey(e.getKey()))
            .forEach(entry -> {
              Integer risk = summary.getTxnMetricRisk().get(transaction).get(entry.getKey());
              if (TimeUnit.MINUTES.toMillis(summary.getAnalysisMinute()) > riskCutOff) {
                observedTimeSeries.get(transaction).get(entry.getKey()).updateRisk(risk);
              }
              observedTimeSeries.get(transaction)
                  .get(entry.getKey())
                  .addToRiskMap(TimeUnit.MINUTES.toMillis(summary.getAnalysisMinute()), risk);
            });

        metricMap.entrySet()
            .stream()
            .filter(e
                -> isNotEmpty(summary.getTxnMetricLongTermPattern())
                    && summary.getTxnMetricLongTermPattern().containsKey(transaction)
                    && summary.getTxnMetricLongTermPattern().get(transaction).containsKey(e.getKey()))
            .forEach(entry -> {
              Integer pattern = summary.getTxnMetricLongTermPattern().get(transaction).get(entry.getKey());
              observedTimeSeries.get(transaction).get(entry.getKey()).setLongTermPattern(pattern);
            });

        // TODO: Keep just this loop going forward and move risk and longtermPattern to this.
        metricMap.entrySet()
            .stream()
            .filter(e
                -> isNotEmpty(summary.getTxnMetricRiskData()) && summary.getTxnMetricRiskData().containsKey(transaction)
                    && summary.getTxnMetricRiskData().get(transaction).containsKey(e.getKey()))
            .forEach(entry -> {
              Integer pattern =
                  summary.getTxnMetricRiskData().get(transaction).get(entry.getKey()).getLongTermPattern();
              long lastSeenTime = summary.getTxnMetricRiskData().get(transaction).get(entry.getKey()).getLastSeenTime();
              Integer risk = summary.getTxnMetricRisk().get(transaction).get(entry.getKey());
              observedTimeSeries.get(transaction).get(entry.getKey()).setLastSeenTime(lastSeenTime);
            });
      });
    }
  }

  @Override
  public SortedSet<TransactionTimeSeries> getTimeSeriesOfHeatMapUnit(TimeSeriesFilter filter) {
    CVConfiguration cvConfiguration = wingsPersistence.get(CVConfiguration.class, filter.getCvConfigId());
    if (cvConfiguration == null) {
      logger.info("No cvConfig found for cvConfigId={}", filter.getCvConfigId());
      return new TreeSet<>();
    }

    populateMetricNames(cvConfiguration, filter);
    filter.setStartTime(Timestamp.nextMinuteBoundary(filter.getStartTime()));
    filter.setEndTime(Timestamp.minuteBoundary(filter.getEndTime()));
    filter.setHistoryStartTime(Timestamp.nextMinuteBoundary(filter.getHistoryStartTime()));

    // 1. Get time series for the entire duration from historyStartTime to endTime
    // 2. Pass startTime as the riskCutOffTime as that's the starting point where we consider risk
    if (TimeUnit.MILLISECONDS.toDays(filter.getEndTime() - filter.getHistoryStartTime()) > 30L) {
      filter.setHistoryStartTime(filter.getStartTime() - TimeUnit.HOURS.toMillis(2) + 1);
    }
    return convertTimeSeriesResponse(getTimeSeriesForTimeRangeFromDataRecords(cvConfiguration, filter));
  }

  private void populateMetricNames(CVConfiguration cvConfiguration, TimeSeriesFilter filter) {
    if (isEmpty(filter.getMetricNames())) {
      return;
    }
    switch (cvConfiguration.getStateType()) {
      case APP_DYNAMICS:
        if (filter.getMetricNames().contains(AppdynamicsConstants.ERROR_DISPLAY_METRIC_NAME)) {
          filter.getMetricNames().remove(AppdynamicsConstants.ERROR_DISPLAY_METRIC_NAME);
          filter.getMetricNames().add(AppdynamicsConstants.ERRORS_PER_MINUTE);
        }

        if (filter.getMetricNames().contains(AppdynamicsConstants.STALL_COUNT_DISPLAY_METRIC_NAME)) {
          filter.getMetricNames().remove(AppdynamicsConstants.STALL_COUNT_DISPLAY_METRIC_NAME);
          filter.getMetricNames().add(AppdynamicsConstants.STALL_COUNT);
        }
        break;

      case NEW_RELIC:
        if (filter.getMetricNames().contains(NewRelicMetricValueDefinition.ERROR_DISPLAY_METRIC_NAME)) {
          filter.getMetricNames().remove(NewRelicMetricValueDefinition.ERROR_DISPLAY_METRIC_NAME);
          filter.getMetricNames().add(NewRelicMetricValueDefinition.ERROR);
        }
        break;

      default:
        throw new WingsException("Invalid State: " + cvConfiguration.getStateType());
    }
  }

  private SortedSet<TransactionTimeSeries> convertTimeSeriesResponse(
      Map<String, Map<String, TimeSeriesOfMetric>> observedTimeSeries) {
    SortedSet<TransactionTimeSeries> resp = new TreeSet<>();
    for (Map.Entry<String, Map<String, TimeSeriesOfMetric>> txnEntry : observedTimeSeries.entrySet()) {
      TransactionTimeSeries txnTimeSeries = new TransactionTimeSeries();
      txnTimeSeries.setTransactionName(txnEntry.getKey());
      txnTimeSeries.setMetricTimeSeries(new TreeSet<>());
      for (Map.Entry<String, TimeSeriesOfMetric> metricEntry : txnEntry.getValue().entrySet()) {
        txnTimeSeries.getMetricTimeSeries().add(metricEntry.getValue());
      }
      resp.add(txnTimeSeries);
    }
    // logger.info("Timeseries response = {}", resp);
    logger.info("TimeSeries response size is : {}", resp.size());
    return resp;
  }

  private Map<String, Map<String, TimeSeriesOfMetric>> getTimeSeriesForTimeRangeFromDataRecords(
      CVConfiguration cvConfiguration, TimeSeriesFilter filter) {
    // The object to be returned which contains the map txn => metrics => timeseries per metric
    Map<String, Map<String, TimeSeriesOfMetric>> observedTimeSeries = new ConcurrentHashMap<>();

    long startTime = filter.getHistoryStartTime();
    long endTime = filter.getEndTime();
    long riskCutOffTime = filter.getStartTime();

    SettingValue connectorConfig = getConnectorConfig(cvConfiguration);
    int endMinute = (int) TimeUnit.MILLISECONDS.toMinutes(endTime);
    int startMinute = (int) TimeUnit.MILLISECONDS.toMinutes(startTime);

    final List<NewRelicMetricDataRecord> metricRecords = new ArrayList<>();
    Map<Integer, Integer> startEndMap = new HashMap<>();
    int movingStart = startMinute, movingEnd = startMinute + 60;
    while (movingEnd <= endMinute) {
      startEndMap.put(movingStart, movingEnd);
      movingStart = movingEnd;
      movingEnd = movingStart + 60;
    }
    if (movingEnd > endMinute) {
      startEndMap.put(movingStart, endMinute);
    }

    List<Callable<Void>> callables = new ArrayList<>();
    startEndMap.forEach((start, end) -> callables.add(() -> {
      PageRequest<NewRelicMetricDataRecord> dataRecordPageRequest =
          PageRequestBuilder.aPageRequest()
              .addFilter("cvConfigId", Operator.EQ, cvConfiguration.getUuid())
              .addFilter("dataCollectionMinute", Operator.GE, start)
              .build();

      if (end == endMinute) {
        dataRecordPageRequest.addFilter("dataCollectionMinute", Operator.LT_EQ, end);
      } else {
        dataRecordPageRequest.addFilter("dataCollectionMinute", Operator.LT, end);
      }

      if (dataStoreService instanceof GoogleDataStoreServiceImpl) {
        dataRecordPageRequest.setLimit(UNLIMITED);
        final List<NewRelicMetricDataRecord> records = new ArrayList<>();
        dataStoreService.list(NewRelicMetricDataRecord.class, dataRecordPageRequest)
            .getResponse()
            .stream()
            .filter(dataRecord -> !HEARTBEAT_METRIC_NAME.equals(dataRecord.getName()))
            .forEach(dataRecord -> {
              // filter for txnName
              if (isEmpty(filter.getTxnNames())) {
                records.add(dataRecord);
              } else if (filter.getTxnNames().contains(dataRecord.getName())) {
                records.add(dataRecord);
              }
            });
        filterMetrics(filter, records);

        metricRecords.addAll(records);
      } else {
        dataRecordPageRequest.addFilter("appId", Operator.EQ, cvConfiguration.getAppId());
        dataRecordPageRequest.addFilter("name", Operator.NOT_EQ, HEARTBEAT_METRIC_NAME);
        dataRecordPageRequest.setFieldsIncluded(
            Lists.newArrayList("name", "values", "dataCollectionMinute", "stateType", "deeplinkMetadata"));
        dataRecordPageRequest.setOffset("0");
        dataRecordPageRequest.setLimit("5000");
        int previousOffSet = 0;
        PageResponse<NewRelicMetricDataRecord> response =
            wingsPersistence.query(NewRelicMetricDataRecord.class, dataRecordPageRequest, excludeCount);
        while (!response.isEmpty()) {
          final List<NewRelicMetricDataRecord> records = new ArrayList<>();
          response.getResponse().forEach(dataRecord -> {
            // filter for txnName
            if (isEmpty(filter.getTxnNames())) {
              records.add(dataRecord);
            } else if (filter.getTxnNames().contains(dataRecord.getName())) {
              records.add(dataRecord);
            }
          });

          // filter for metric names
          filterMetrics(filter, records);
          metricRecords.addAll(records);
          previousOffSet += response.size();
          dataRecordPageRequest.setOffset(String.valueOf(previousOffSet));
          response = wingsPersistence.query(NewRelicMetricDataRecord.class, dataRecordPageRequest, excludeCount);
        }
      }
      return null;
    }));

    dataCollectionService.executeParrallel(callables);

    logger.info("Size of metric records : {}", metricRecords.size());
    for (NewRelicMetricDataRecord metricRecord : metricRecords) {
      if (!observedTimeSeries.containsKey(metricRecord.getName())) {
        observedTimeSeries.put(metricRecord.getName(), new HashMap<>());
      }
      Map<String, TimeSeriesOfMetric> metricMap = observedTimeSeries.get(metricRecord.getName());
      for (Entry<String, Double> metricData : metricRecord.getValues().entrySet()) {
        final String metricName = metricData.getKey();
        if (!metricMap.containsKey(metricName)) {
          metricMap.put(metricName,
              TimeSeriesOfMetric.builder()
                  .metricName(getDisplayNameOfMetric(metricName))
                  .timeSeries(initializeTimeSeriesDataPointsList(startTime, endTime, TimeUnit.MINUTES.toMillis(1), -1))
                  .risk(-1)
                  .build());
        }

        // fill in the metrics for this record at the correct spots
        metricMap.get(metricName)
            .addToTimeSeriesMap(
                metricRecord.getDataCollectionMinute(), getNormalizedMetricValue(metricName, metricRecord));
        metricMap.get(metricName).setMetricType(getMetricType(cvConfiguration, metricName));
        if (isNotEmpty(metricRecord.getDeeplinkMetadata())) {
          if (metricRecord.getDeeplinkMetadata().containsKey(metricName)) {
            String deeplinkUrl = getDeeplinkUrl(cvConfiguration, connectorConfig, startTime, endTime,
                metricRecord.getDeeplinkMetadata().get(metricName));
            metricMap.get(metricName).setMetricDeeplinkUrl(deeplinkUrl);
          }
        }
      }
    }

    // Find and add the risks for those metrics above
    updateRisksFromSummary(startTime, endTime, riskCutOffTime, cvConfiguration, observedTimeSeries);

    return observedTimeSeries;
  }

  private void filterMetrics(TimeSeriesFilter filter, List<NewRelicMetricDataRecord> records) {
    // filter for metric names
    if (isNotEmpty(filter.getMetricNames())) {
      records.forEach(dataRecord -> {
        for (Iterator<Entry<String, Double>> iterator = dataRecord.getValues().entrySet().iterator();
             iterator.hasNext();) {
          final Entry<String, Double> valueEntry = iterator.next();
          if (!filter.getMetricNames().contains(valueEntry.getKey())) {
            iterator.remove();
          }
        }
      });

      for (Iterator<NewRelicMetricDataRecord> recordIterator = records.iterator(); recordIterator.hasNext();) {
        NewRelicMetricDataRecord dataRecord = recordIterator.next();
        if (isEmpty(dataRecord.getValues())) {
          recordIterator.remove();
        }
      }
    }
  }

  @Override
  public VerificationNodeDataSetupResponse getMetricsWithDataForNode(
      String accountId, String serverConfigId, Object fetchConfig, StateType type) {
    try {
      if (isEmpty(serverConfigId) || fetchConfig == null) {
        throw new WingsException("Invalid Parameters passed while trying to get test data for APM");
      }
      SettingAttribute settingAttribute = settingsService.get(serverConfigId);
      APMValidateCollectorConfig apmValidateCollectorConfig;
      switch (type) {
        case DATA_DOG:
          DataDogFetchConfig config = (DataDogFetchConfig) fetchConfig;
          DatadogConfig datadogConfig = (DatadogConfig) settingAttribute.getValue();
          List<EncryptedDataDetail> encryptedDataDetails =
              secretManager.getEncryptionDetails((EncryptableSetting) settingAttribute.getValue(), null, null);

          apmValidateCollectorConfig = datadogConfig.createAPMValidateCollectorConfig();
          apmValidateCollectorConfig.setEncryptedDataDetails(encryptedDataDetails);
          apmValidateCollectorConfig.getOptions().put("from", String.valueOf(config.getFromtime()));
          apmValidateCollectorConfig.getOptions().put("to", String.valueOf(config.getToTime()));

          Map<String, List<APMMetricInfo>> metricInfoByQuery =
              metricEndpointsInfo(config.getDatadogServiceName(), Arrays.asList(config.getMetrics().split(",")), null);
          List<Object> loadResponse = new ArrayList<>();

          // loop for each metric
          for (Entry<String, List<APMMetricInfo>> entry : metricInfoByQuery.entrySet()) {
            String url = entry.getKey();
            if (url.contains("${host}")) {
              url = url.replace("${host}", config.getHostName());
            }
            apmValidateCollectorConfig.setUrl(url);
            VerificationNodeDataSetupResponse verificationNodeDataSetupResponse =
                getVerificationNodeDataResponse(accountId, apmValidateCollectorConfig);
            if (!verificationNodeDataSetupResponse.isProviderReachable()) {
              // if not reachable then directly return. no need to process further
              return VerificationNodeDataSetupResponse.builder().providerReachable(false).build();
            }
            // add load response only for metrics containing nodedata.
            if (verificationNodeDataSetupResponse.getLoadResponse().isLoadPresent()) {
              loadResponse.add(verificationNodeDataSetupResponse);
            }
          }

          VerificationLoadResponse response = VerificationLoadResponse.builder()
                                                  .loadResponse(loadResponse)
                                                  .isLoadPresent(!isEmpty(loadResponse))
                                                  .build();

          return VerificationNodeDataSetupResponse.builder()
              .providerReachable(true)
              .loadResponse(response)
              .dataForNode(loadResponse)
              .build();

        case APM_VERIFICATION:
          APMFetchConfig apmFetchConfig = (APMFetchConfig) fetchConfig;
          APMVerificationConfig apmVerificationConfig = (APMVerificationConfig) settingAttribute.getValue();
          apmValidateCollectorConfig =
              APMValidateCollectorConfig.builder()
                  .baseUrl(apmVerificationConfig.getUrl())
                  .headers(apmVerificationConfig.collectionHeaders())
                  .options(apmVerificationConfig.collectionParams())
                  .url(apmFetchConfig.getUrl())
                  .body(apmFetchConfig.getBody())
                  .encryptedDataDetails(apmVerificationConfig.encryptedDataDetails(secretManager))
                  .build();

          return getVerificationNodeDataResponse(accountId, apmValidateCollectorConfig);
        default:
          throw new WingsException("Invalid StateType provided" + type);
      }
    } catch (Exception e) {
      String errorMsg = e.getCause() != null ? ExceptionUtils.getMessage(e.getCause()) : ExceptionUtils.getMessage(e);
      throw new WingsException(ErrorCode.APM_CONFIGURATION_ERROR, USER).addParam("reason", errorMsg);
    }
  }

  @Override
  public boolean sendNotifyForMetricAnalysis(String correlationId, MetricDataAnalysisResponse response) {
    try {
      waitNotifyEngine.notify(correlationId, response);
      return true;
    } catch (Exception ex) {
      logger.error("Exception while notifying correlationId {}", correlationId, ex);
      return false;
    }
  }

  @Override
  public boolean collect247Data(String cvConfigId, StateType stateType, long startTime, long endTime) {
    String waitId = generateUuid();
    DelegateTask task;
    CVConfiguration cvConfiguration =
        wingsPersistence.createQuery(CVConfiguration.class).filter("_id", cvConfigId).get();
    boolean isLogCollection = false;
    switch (stateType) {
      case APP_DYNAMICS:
        AppDynamicsCVServiceConfiguration config = (AppDynamicsCVServiceConfiguration) cvConfiguration;
        task = createAppDynamicsDelegateTask(config, waitId, startTime, endTime);
        break;
      case NEW_RELIC:
        NewRelicCVServiceConfiguration nrConfig = (NewRelicCVServiceConfiguration) cvConfiguration;
        task = createNewRelicDelegateTask(nrConfig, waitId, startTime, endTime);
        break;
      case DYNA_TRACE:
        DynaTraceCVServiceConfiguration dynaTraceCVServiceConfiguration =
            (DynaTraceCVServiceConfiguration) cvConfiguration;
        task = createDynaTraceDelegateTask(dynaTraceCVServiceConfiguration, waitId, startTime, endTime);
        break;
      case PROMETHEUS:
        PrometheusCVServiceConfiguration prometheusCVServiceConfiguration =
            (PrometheusCVServiceConfiguration) cvConfiguration;
        task = createPrometheusDelegateTask(prometheusCVServiceConfiguration, waitId, startTime, endTime);
        break;
      case DATA_DOG:
        DatadogCVServiceConfiguration ddConfig = (DatadogCVServiceConfiguration) cvConfiguration;
        task = createDatadogDelegateTask(ddConfig, waitId, startTime, endTime);
        break;
      case CLOUD_WATCH:
        CloudWatchCVServiceConfiguration cloudWatchCVServiceConfiguration =
            (CloudWatchCVServiceConfiguration) cvConfiguration;
        task = createCloudWatchDelegateTask(cloudWatchCVServiceConfiguration, waitId, startTime, endTime);
        break;
      case SUMO:
        LogsCVConfiguration logsCVConfiguration = (LogsCVConfiguration) cvConfiguration;
        task = createDataCollectionDelegateTask(logsCVConfiguration, waitId, startTime, endTime);
        isLogCollection = true;
        break;
      case ELK:
        ElkCVConfiguration elkCVConfiguration = (ElkCVConfiguration) cvConfiguration;
        task = createElkDelegateTask(elkCVConfiguration, waitId, startTime, endTime);
        isLogCollection = true;
        break;
      default:
        logger.error("Calling collect 24x7 data for an unsupported state");
        return false;
    }
    waitNotifyEngine.waitForAll(
        new DataCollectionCallback(cvConfiguration.getAppId(),
            getExecutionData(cvConfiguration, waitId, (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime)),
            isLogCollection),
        waitId);
    logger.info("Queuing 24x7 data collection task for {}, cvConfigurationId: {}", stateType, cvConfigId);
    delegateService.queueTask(task);
    return true;
  }

  @Override
  public boolean collectCVDataForWorkflow(String contextId, long collectionMinute) {
    AnalysisContext context = wingsPersistence.createQuery(AnalysisContext.class).filter("_id", contextId).get();
    logger.info("Trigger Data Collection for workflow with stateType {}, stateExecutionId {}, CollectionMinute {}",
        context.getStateType(), context.getStateExecutionId(), collectionMinute);
    switch (context.getStateType()) {
      case SUMO:
        return createDataCollectionDelegateTask(context, collectionMinute);
      default:
        logger.error("Calling collect data for an unsupported state");
        return false;
    }
  }

  private MetricAnalysisExecutionData getExecutionData(
      CVConfiguration cvConfiguration, String waitId, int timeDuration) {
    return MetricAnalysisExecutionData.builder()
        .appId(cvConfiguration.getAppId())
        .workflowExecutionId(null)
        .stateExecutionInstanceId(CV_24x7_STATE_EXECUTION + "-" + cvConfiguration.getUuid())
        .serverConfigId(cvConfiguration.getConnectorId())
        .timeDuration(timeDuration)
        .canaryNewHostNames(new HashSet<>())
        .lastExecutionNodes(new HashSet<>())
        .correlationId(waitId)
        .build();
  }

  private DelegateTask createDynaTraceDelegateTask(
      DynaTraceCVServiceConfiguration config, String waitId, long startTime, long endTime) {
    DynaTraceConfig dynaTraceConfig = (DynaTraceConfig) settingsService.get(config.getConnectorId()).getValue();
    int timeDuration = (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
    final DynaTraceDataCollectionInfo dataCollectionInfo =
        DynaTraceDataCollectionInfo.builder()
            .dynaTraceConfig(dynaTraceConfig)
            .applicationId(config.getAppId())
            .serviceId(config.getServiceId())
            .cvConfigId(config.getUuid())
            .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getUuid())
            .timeSeriesDefinitions(Lists.newArrayList(DynaTraceTimeSeries.values()))
            .serviceMethods(DynatraceState.splitServiceMethods(config.getServiceMethods()))
            .startTime(startTime)
            .collectionTime(timeDuration)
            .dataCollectionMinute(0)
            .encryptedDataDetails(secretManager.getEncryptionDetails(dynaTraceConfig, config.getAppId(), null))
            .analysisComparisonStrategy(AnalysisComparisonStrategy.PREDICTIVE)
            .build();

    return createDelegateTask(TaskType.DYNATRACE_COLLECT_24_7_METRIC_DATA, config.getAccountId(), config.getAppId(),
        waitId, new Object[] {dataCollectionInfo}, config.getEnvId());
  }

  private DelegateTask createAppDynamicsDelegateTask(
      AppDynamicsCVServiceConfiguration config, String waitId, long startTime, long endTime) {
    AppDynamicsConfig appDynamicsConfig = (AppDynamicsConfig) settingsService.get(config.getConnectorId()).getValue();
    int timeDuration = (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
    final AppdynamicsDataCollectionInfo dataCollectionInfo =
        AppdynamicsDataCollectionInfo.builder()
            .appDynamicsConfig(appDynamicsConfig)
            .applicationId(config.getAppId())
            .serviceId(config.getServiceId())
            .cvConfigId(config.getUuid())
            .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getUuid())
            .startTime(startTime)
            .collectionTime(timeDuration)
            .appId(Long.parseLong(config.getAppDynamicsApplicationId()))
            .tierId(Long.parseLong(config.getTierId()))
            .dataCollectionMinute(0)
            .hosts(new HashMap<>())
            .encryptedDataDetails(secretManager.getEncryptionDetails(appDynamicsConfig, config.getAppId(), null))
            .timeSeriesMlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .build();
    return createDelegateTask(TaskType.APPDYNAMICS_COLLECT_24_7_METRIC_DATA, config.getAccountId(), config.getAppId(),
        waitId, new Object[] {dataCollectionInfo}, config.getEnvId());
  }

  private DelegateTask createNewRelicDelegateTask(
      NewRelicCVServiceConfiguration config, String waitId, long startTime, long endTime) {
    final NewRelicConfig newRelicConfig = (NewRelicConfig) settingsService.get(config.getConnectorId()).getValue();
    int timeDuration = (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
    Map<String, String> hostsMap = new HashMap<>();
    hostsMap.put("DUMMY_24_7_HOST", DEFAULT_GROUP_NAME);
    final NewRelicDataCollectionInfo dataCollectionInfo =
        NewRelicDataCollectionInfo.builder()
            .newRelicConfig(newRelicConfig)
            .applicationId(config.getAppId())
            .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getUuid())
            .serviceId(config.getServiceId())
            .startTime(startTime)
            .cvConfigId(config.getUuid())
            .collectionTime(timeDuration)
            .newRelicAppId(Long.parseLong(config.getApplicationId()))
            .timeSeriesMlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .dataCollectionMinute(0)
            .hosts(hostsMap)
            .encryptedDataDetails(secretManager.getEncryptionDetails(newRelicConfig, config.getAppId(), null))
            .settingAttributeId(config.getConnectorId())
            .build();
    return createDelegateTask(TaskType.NEWRELIC_COLLECT_24_7_METRIC_DATA, config.getAccountId(), config.getAppId(),
        waitId, new Object[] {dataCollectionInfo}, config.getEnvId());
  }

  private DelegateTask createPrometheusDelegateTask(
      PrometheusCVServiceConfiguration config, String waitId, long startTime, long endTime) {
    PrometheusConfig prometheusConfig = (PrometheusConfig) settingsService.get(config.getConnectorId()).getValue();
    int timeDuration = (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
    final PrometheusDataCollectionInfo dataCollectionInfo =
        PrometheusDataCollectionInfo.builder()
            .prometheusConfig(prometheusConfig)
            .applicationId(config.getAppId())
            .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getUuid())
            .serviceId(config.getServiceId())
            .cvConfigId(config.getUuid())
            .startTime(startTime)
            .collectionTime(timeDuration)
            .timeSeriesToCollect(config.getTimeSeriesToAnalyze())
            .hosts(new HashMap<>())
            .timeSeriesMlAnalysisType(TimeSeriesMlAnalysisType.PREDICTIVE)
            .dataCollectionMinute(0)
            .build();
    return createDelegateTask(TaskType.PROMETHEUS_COLLECT_24_7_METRIC_DATA, config.getAccountId(), config.getAppId(),
        waitId, new Object[] {dataCollectionInfo}, config.getEnvId());
  }

  private DelegateTask createDatadogDelegateTask(
      DatadogCVServiceConfiguration config, String waitId, long startTime, long endTime) {
    DatadogConfig datadogConfig = (DatadogConfig) settingsService.get(config.getConnectorId()).getValue();
    int timeDuration = (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);
    Map<String, String> hostsMap = new HashMap<>();
    hostsMap.put("DUMMY_24_7_HOST", DEFAULT_GROUP_NAME);
    final APMDataCollectionInfo dataCollectionInfo =
        APMDataCollectionInfo.builder()
            .baseUrl(datadogConfig.getUrl())
            .validationUrl(DatadogConfig.validationUrl)
            .encryptedDataDetails(secretManager.getEncryptionDetails(datadogConfig, config.getAppId(), null))
            .hosts(hostsMap)
            .stateType(StateType.DATA_DOG)
            .applicationId(config.getAppId())
            .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getUuid())
            .serviceId(config.getServiceId())
            .startTime(startTime)
            .cvConfigId(config.getUuid())
            .dataCollectionMinute(0)
            .metricEndpoints(DatadogState.metricEndpointsInfo(config.getDatadogServiceName(),
                Arrays.asList(config.getMetrics().split(",")), config.getApplicationFilter()))
            .accountId(config.getAccountId())
            .strategy(AnalysisComparisonStrategy.PREDICTIVE)
            .dataCollectionFrequency(1)
            .dataCollectionTotalTime(timeDuration)
            .build();
    return createDelegateTask(TaskType.APM_24_7_METRIC_DATA_COLLECTION_TASK, config.getAccountId(), config.getAppId(),
        waitId, new Object[] {dataCollectionInfo}, config.getEnvId());
  }

  private DelegateTask createCloudWatchDelegateTask(
      CloudWatchCVServiceConfiguration config, String waitId, long startTime, long endTime) {
    AwsConfig awsConfig = (AwsConfig) settingsService.get(config.getConnectorId()).getValue();
    int timeDuration = (int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime);

    final CloudWatchDataCollectionInfo dataCollectionInfo =
        CloudWatchDataCollectionInfo.builder()
            .awsConfig(awsConfig)
            .applicationId(config.getAppId())
            .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getUuid())
            .serviceId(config.getServiceId())
            .cvConfigId(config.getUuid())
            .startTime(startTime)
            .collectionTime(timeDuration)
            .hosts(cloudWatchService.getGroupNameByHost(config.getEc2InstanceNames()))
            .ec2Metrics(config.getEc2Metrics())
            .encryptedDataDetails(secretManager.getEncryptionDetails(awsConfig, config.getAppId(), null))
            .analysisComparisonStrategy(AnalysisComparisonStrategy.PREDICTIVE)
            .loadBalancerMetrics(config.getLoadBalancerMetrics())
            .lambdaFunctionNames(config.getLambdaFunctionsMetrics())
            .metricsByECSClusterName(config.getEcsMetrics())
            .region(config.getRegion())
            .dataCollectionMinute(0)
            .build();
    return createDelegateTask(TaskType.CLOUD_WATCH_COLLECT_24_7_METRIC_DATA, config.getAccountId(), config.getAppId(),
        waitId, new Object[] {dataCollectionInfo}, config.getEnvId());
  }

  private DelegateTask createElkDelegateTask(ElkCVConfiguration config, String waitId, long startTime, long endTime) {
    ElkConfig elkConfig = (ElkConfig) settingsService.get(config.getConnectorId()).getValue();
    final ElkDataCollectionInfo dataCollectionInfo =
        ElkDataCollectionInfo.builder()
            .elkConfig(elkConfig)
            .accountId(elkConfig.getAccountId())
            .applicationId(config.getAppId())
            .stateExecutionId(CV_24x7_STATE_EXECUTION + "-" + config.getUuid())
            .cvConfigId(config.getUuid())
            .serviceId(config.getServiceId())
            .query(config.getQuery())
            .formattedQuery(config.isFormattedQuery())
            .indices(config.getIndex())
            .hostnameField(config.getHostnameField())
            .messageField(config.getMessageField())
            .timestampField(config.getTimestampField())
            .timestampFieldFormat(config.getTimestampFormat())
            .queryType(config.getQueryType())
            .startTime(startTime)
            .endTime(endTime)
            .hosts(Sets.newHashSet(DUMMY_HOST_NAME))
            .encryptedDataDetails(secretManager.getEncryptionDetails(elkConfig, config.getAppId(), null))
            .build();
    return createDelegateTask(TaskType.ELK_COLLECT_24_7_LOG_DATA, config.getAccountId(), config.getAppId(), waitId,
        new Object[] {dataCollectionInfo}, config.getEnvId());
  }

  private DelegateTask createDataCollectionDelegateTask(
      LogsCVConfiguration config, String waitId, long startTime, long endTime) {
    String stateExecutionId = CV_24x7_STATE_EXECUTION + "-" + config.getUuid();
    if (config.isWorkflowConfig()) {
      stateExecutionId = wingsPersistence.get(AnalysisContext.class, config.getContextId()).getStateExecutionId();
    }
    switch (config.getStateType()) {
      case SUMO:
        SumoConfig sumoConfig = (SumoConfig) settingsService.get(config.getConnectorId()).getValue();
        final SumoDataCollectionInfo dataCollectionInfo =
            SumoDataCollectionInfo.builder()
                .sumoConfig(sumoConfig)
                .accountId(sumoConfig.getAccountId())
                .applicationId(config.getAppId())
                .stateExecutionId(stateExecutionId)
                .cvConfigId(config.getUuid())
                .serviceId(config.getServiceId())
                .query(config.getQuery())
                .startTime(startTime)
                .endTime(endTime)
                .encryptedDataDetails(secretManager.getEncryptionDetails(sumoConfig, config.getAppId(), null))
                .hosts(Sets.newHashSet(DUMMY_HOST_NAME))
                .build();
        return createDelegateTask(TaskType.SUMO_COLLECT_24_7_LOG_DATA, config.getAccountId(), config.getAppId(), waitId,
            new Object[] {dataCollectionInfo}, config.getEnvId());
      default:
        throw new IllegalStateException("Invalid state: " + config.getStateType());
    }
  }

  private boolean createDataCollectionDelegateTask(AnalysisContext context, long collectionStartMinute) {
    Set<String> canaryNewHostNames = context.getTestNodes().keySet();
    Map<String, String> lastExecutionNodes = context.getControlNodes();
    List<String> hostsToBeCollected = new ArrayList<>();
    if (context.getComparisonStrategy() == AnalysisComparisonStrategy.COMPARE_WITH_CURRENT
        && lastExecutionNodes != null) {
      hostsToBeCollected.addAll(lastExecutionNodes.keySet());
    }
    hostsToBeCollected.addAll(canaryNewHostNames);
    switch (context.getStateType()) {
      case SUMO:
        final LogAnalysisExecutionData executionData =
            createLogAnalysisExecutionData(context, canaryNewHostNames, lastExecutionNodes);
        try {
          SumoConfig sumoConfig = (SumoConfig) settingsService.get(context.getAnalysisServerConfigId()).getValue();
          List<DelegateTask> delegateTasks = new ArrayList<>();
          for (List<String> hostBatch : Lists.partition(hostsToBeCollected, HOST_BATCH_SIZE)) {
            final SumoDataCollectionInfo dataCollectionInfo =
                createSUMODataCollectionInfo(sumoConfig, context, collectionStartMinute, new HashSet<>(hostBatch));
            String waitId = generateUuid();
            delegateTasks.add(createDelegateTask(TaskType.SUMO_COLLECT_LOG_DATA, context.getAccountId(),
                context.getAppId(), waitId, new Object[] {dataCollectionInfo}, context.getEnvId()));
            waitNotifyEngine.waitForAll(new DataCollectionCallback(context.getAppId(), executionData, true, true,
                                            context.getStateExecutionId(), context.getStateType()),
                waitId);
          }
          for (DelegateTask task : delegateTasks) {
            delegateService.queueTask(task);
          }
        } catch (Exception ex) {
          throw new WingsException("log analysis state failed ", ex);
        }
        break;
      default:
        throw new IllegalStateException("Invalid state: " + context.getStateType());
    }
    return true;
  }

  private SumoDataCollectionInfo createSUMODataCollectionInfo(
      SumoConfig sumoConfig, AnalysisContext context, long collectionStartMinute, Set<String> hostBatch) {
    return SumoDataCollectionInfo.builder()
        .sumoConfig(sumoConfig)
        .accountId(context.getAccountId())
        .applicationId(context.getAppId())
        .stateExecutionId(context.getStateExecutionId())
        .workflowId(context.getWorkflowId())
        .workflowExecutionId(context.getWorkflowExecutionId())
        .serviceId(context.getServiceId())
        .query(context.getQuery())
        .startMinute((int) collectionStartMinute)
        .collectionTime(1)
        .hosts(hostBatch)
        .hostnameField(context.getHostNameField())
        .encryptedDataDetails(
            secretManager.getEncryptionDetails(sumoConfig, context.getAppId(), context.getWorkflowExecutionId()))
        .build();
  }

  private LogAnalysisExecutionData createLogAnalysisExecutionData(
      AnalysisContext context, Set<String> canaryNewHostNames, Map<String, String> lastExecutionNodes) {
    return LogAnalysisExecutionData.builder()
        .stateExecutionInstanceId(context.getStateExecutionId())
        .serverConfigId(context.getAnalysisServerConfigId())
        .query(context.getQuery())
        .timeDuration((int) TimeUnit.MINUTES.toMillis(1))
        .canaryNewHostNames(canaryNewHostNames)
        .lastExecutionNodes(lastExecutionNodes == null ? new HashSet<>() : new HashSet<>(lastExecutionNodes.keySet()))
        .correlationId(context.getCorrelationId())
        .build();
  }

  private DelegateTask createDelegateTask(
      TaskType taskType, String accountId, String appId, String waitId, Object[] dataCollectionInfo, String envId) {
    return DelegateTask.builder()
        .async(true)
        .taskType(taskType.name())
        .accountId(accountId)
        .appId(appId)
        .waitId(waitId)
        .data(TaskData.builder().parameters(dataCollectionInfo).timeout(TimeUnit.MINUTES.toMillis(30)).build())
        .envId(envId)
        .build();
  }

  private VerificationNodeDataSetupResponse getVerificationNodeDataResponse(
      String accountId, APMValidateCollectorConfig apmValidateCollectorConfig) {
    SyncTaskContext syncTaskContext =
        SyncTaskContext.builder().accountId(accountId).appId(GLOBAL_APP_ID).timeout(DEFAULT_SYNC_CALL_TIMEOUT).build();
    String apmResponse =
        delegateProxyFactory.get(APMDelegateService.class, syncTaskContext).fetch(apmValidateCollectorConfig);
    JSONObject jsonObject = new JSONObject(apmResponse);
    boolean hasLoad = false;
    if (jsonObject.length() != 0) {
      hasLoad = true;
    }
    VerificationLoadResponse loadResponse =
        VerificationLoadResponse.builder().loadResponse(apmResponse).isLoadPresent(hasLoad).build();
    return VerificationNodeDataSetupResponse.builder()
        .providerReachable(hasLoad)
        .loadResponse(loadResponse)
        .dataForNode(apmResponse)
        .build();
  }
}
