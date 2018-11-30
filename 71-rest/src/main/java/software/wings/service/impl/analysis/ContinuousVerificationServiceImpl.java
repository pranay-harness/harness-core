package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.min;
import static java.util.Collections.emptySet;
import static software.wings.common.VerificationConstants.APPDYNAMICS_DEEPLINK_FORMAT;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;
import static software.wings.common.VerificationConstants.HEARTBEAT_METRIC_NAME;
import static software.wings.verification.TimeSeriesDataPoint.initializeTimeSeriesDataPointsList;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder.OrderType;
import io.harness.exception.WingsException;
import io.harness.time.Timestamp;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.FeatureName;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.metrics.RiskLevel;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.PermissionAttribute.Action;
import software.wings.service.impl.appdynamics.AppdynamicsTimeSeries;
import software.wings.service.impl.newrelic.NewRelicMetricDataRecord;
import software.wings.service.impl.newrelic.NewRelicMetricValueDefinition;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.PipelineSummary;
import software.wings.sm.StateType;
import software.wings.sm.states.AppDynamicsState;
import software.wings.sm.states.NewRelicState;
import software.wings.verification.CVConfiguration;
import software.wings.verification.HeatMap;
import software.wings.verification.HeatMapResolution;
import software.wings.verification.TimeSeriesOfMetric;
import software.wings.verification.TransactionTimeSeries;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.dashboard.HeatMapUnit;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class ContinuousVerificationServiceImpl implements ContinuousVerificationService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AuthService authService;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private CVConfigurationService cvConfigurationService;
  private static final Logger logger = LoggerFactory.getLogger(ContinuousVerificationServiceImpl.class);

  private static final int DURATION_IN_MINUTES = 10;
  private static final int PAGE_LIMIT = 500;
  private static final int START_OFFSET = 0;

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
      deploymentData.get(execution.getUuid()).setWorkflowName(execution.getName());
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
    if (isEmpty(allowedApplications)) {
      logger.info(
          "Returning empty results from getCVDeploymentData since user {} does not have permissions for any applications",
          user);
      return results;
    }
    PageRequest<WorkflowExecution> workflowExecutionPageRequest =
        PageRequestBuilder.aPageRequest()
            .addFilter("appId", Operator.IN, allowedApplications.toArray())
            .addFilter("serviceIds", Operator.CONTAINS, serviceId)
            .addFieldsExcluded("serviceExecutionSummaries", "executionArgs", "keywords", "breakdown")
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

    return results;
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

    cvConfigurations.parallelStream().forEach(cvConfig -> {
      cvConfigurationService.fillInServiceAndConnectorNames(cvConfig);
      String envName = cvConfig.getEnvName();
      logger.info("Environment name = " + envName);
      final HeatMap heatMap = HeatMap.builder().cvConfiguration(cvConfig).build();
      rv.add(heatMap);

      List<HeatMapUnit> units = createAllHeatMapUnits(appId, startTime, endTime, cvConfig);
      List<HeatMapUnit> resolvedUnits = resolveHeatMapUnits(units, startTime, endTime);
      heatMap.getRiskLevelSummary().addAll(resolvedUnits);
    });

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
    HeatMapUnit mergedUnit = new HeatMapUnit();
    mergedUnit.setStartTime(units.get(0).getStartTime());
    mergedUnit.setEndTime(units.get(units.size() - 1).getEndTime());
    units.forEach(unit -> {
      mergedUnit.setHighRisk(mergedUnit.getHighRisk() + unit.getHighRisk());
      mergedUnit.setMediumRisk(mergedUnit.getMediumRisk() + unit.getMediumRisk());
      mergedUnit.setLowRisk(mergedUnit.getLowRisk() + unit.getLowRisk());
      mergedUnit.setNa(mergedUnit.getNa() + unit.getNa());
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
              .build();
      heatMapUnit.increment(RiskLevel.getRiskLevel(record.getAggregatedRisk()));
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
        wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
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
    return getTimeSeriesForTimeRangeFromDataRecords(historyStartTime, endTime, cvConfiguration, startTime);
  }

  private String getBaseUrlOfConnector(CVConfiguration cvConfiguration) {
    switch (cvConfiguration.getStateType()) {
      case APP_DYNAMICS:
        AppDynamicsConfig appDynamicsConfig =
            (AppDynamicsConfig) wingsPersistence.get(SettingAttribute.class, cvConfiguration.getConnectorId())
                .getValue();
        return appDynamicsConfig.getControllerUrl();
      default:
        logger.info("Unsupported stateType {} for deeplinking", cvConfiguration.getStateType());
        return "";
    }
  }

  private Map<String, Map<String, TimeSeriesOfMetric>> getTimeSeriesForTimeRangeFromDataRecords(
      long startTime, long endTime, CVConfiguration cvConfiguration, long riskCutOffTime) {
    // The object to be returned which contains the map txn => metrics => timeseries per metric
    Map<String, Map<String, TimeSeriesOfMetric>> observedTimeSeries = new HashMap<>();

    // int endMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Timestamp.minuteBoundary(endTime));
    // int startMinute = (int) TimeUnit.MILLISECONDS.toMinutes(Timestamp.minuteBoundary(startTime));

    String baseUrlForLink = getBaseUrlOfConnector(cvConfiguration);
    int endMinute = (int) TimeUnit.MILLISECONDS.toMinutes(endTime);
    int startMinute = (int) TimeUnit.MILLISECONDS.toMinutes(startTime);

    List<NewRelicMetricDataRecord> metricRecords = new ArrayList<>();

    PageRequest<NewRelicMetricDataRecord> dataRecordPageRequest =
        PageRequestBuilder.aPageRequest()
            .withLimit("5000")
            .withOffset("0")
            .addFilter("appId", Operator.EQ, cvConfiguration.getAppId())
            .addFilter("cvConfigId", Operator.EQ, cvConfiguration.getUuid())
            .addFilter("dataCollectionMinute", Operator.GE, startMinute)
            .addFilter("dataCollectionMinute", Operator.LT_EQ, endMinute)
            .addFilter("name", Operator.NOT_EQ, HEARTBEAT_METRIC_NAME)
            .addFieldsIncluded("name", "values", "dataCollectionMinute", "stateType", "deeplinkMetadata")
            .build();

    int previousOffSet = 0;
    PageResponse<NewRelicMetricDataRecord> response =
        wingsPersistence.query(NewRelicMetricDataRecord.class, dataRecordPageRequest);
    while (!response.isEmpty()) {
      metricRecords.addAll(response.getResponse());
      previousOffSet += response.size();
      dataRecordPageRequest.setOffset(String.valueOf(previousOffSet));
      response = wingsPersistence.query(NewRelicMetricDataRecord.class, dataRecordPageRequest);
    }

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
                  .metricName(metricName)
                  .timeSeries(initializeTimeSeriesDataPointsList(startTime, endTime, TimeUnit.MINUTES.toMillis(1), -1))
                  .risk(-1)
                  .build());
        }

        // fill in the metrics for this record at the correct spots
        metricMap.get(metricName)
            .addToTimeSeriesMap(
                metricRecord.getDataCollectionMinute(), getNormalizedMetricValue(metricName, metricRecord));
        if (isNotEmpty(metricRecord.getDeeplinkMetadata())) {
          if (metricRecord.getDeeplinkMetadata().containsKey(metricName)) {
            String deeplinkUrl = getDeeplinkUrl(cvConfiguration, baseUrlForLink, startTime, endTime,
                metricRecord.getDeeplinkMetadata().get(metricName));
            metricMap.get(metricName).setMetricDeeplinkUrl(deeplinkUrl);
          }
        }
      }
    }

    // Find and add the risks for those metrics above
    updateRisksForTimeSeries(startTime, endTime, riskCutOffTime, cvConfiguration, observedTimeSeries);

    return observedTimeSeries;
  }

  private Double getNormalizedMetricValue(String metricName, NewRelicMetricDataRecord dataRecord) {
    switch (dataRecord.getStateType()) {
      case APP_DYNAMICS:
        if (metricName.equals(AppdynamicsTimeSeries.ERRORS_PER_MINUTE.getMetricName())) {
          return AppDynamicsState.getErrorPercentage(dataRecord);
        }
        break;
      case NEW_RELIC:
        if (metricName.equals(NewRelicMetricValueDefinition.ERROR)) {
          return NewRelicState.getNormalizedErrorMetric(dataRecord);
        }
        break;
      default:
        return dataRecord.getValues().get(metricName);
    }
    return dataRecord.getValues().get(metricName);
  }
  private String getDeeplinkUrl(
      CVConfiguration cvConfig, String baseUrl, long startTime, long endTime, String metricString) {
    switch (cvConfig.getStateType()) {
      case APP_DYNAMICS:
        AppDynamicsCVServiceConfiguration config = (AppDynamicsCVServiceConfiguration) cvConfig;
        return baseUrl
            + APPDYNAMICS_DEEPLINK_FORMAT.replace("{startTimeMs}", String.valueOf(startTime))
                  .replace("{endTimeMs}", String.valueOf(endTime))
                  .replace("{applicationId}", config.getAppDynamicsApplicationId())
                  .replace("{metricString}", metricString);
      default:
        logger.info("Unsupported stateType {} for deeplinking", cvConfig.getStateType());
        return "";
    }
  }

  private void updateRisksForTimeSeries(long startTime, long endTime, long riskCutOff, CVConfiguration cvConfiguration,
      Map<String, Map<String, TimeSeriesOfMetric>> observedTimeSeries) {
    List<TimeSeriesMLAnalysisRecord> analysisRecords = wingsPersistence.createQuery(TimeSeriesMLAnalysisRecord.class)
                                                           .filter("appId", cvConfiguration.getAppId())
                                                           .filter("cvConfigId", cvConfiguration.getUuid())
                                                           .field("analysisMinute")
                                                           .greaterThan(TimeUnit.MILLISECONDS.toMinutes(startTime))
                                                           .field("analysisMinute")
                                                           .lessThanOrEq(TimeUnit.MILLISECONDS.toMinutes(endTime))
                                                           .order("analysisMinute")
                                                           .asList();
    analysisRecords.forEach(timeSeriesMLAnalysisRecord -> timeSeriesMLAnalysisRecord.decompressTransactions());
    try {
      for (TimeSeriesMLAnalysisRecord record : analysisRecords) {
        for (Entry<String, TimeSeriesMLTxnSummary> transaction : record.getTransactions().entrySet()) {
          Map<String, TimeSeriesOfMetric> metricMap = observedTimeSeries.get(transaction.getValue().getTxn_name());
          if (metricMap == null) {
            // there's no data in this time range for this txn, so we're  moving on.
            logger.info("Skipping {}", transaction.getValue().getTxn_name());
            continue;
          }

          for (TimeSeriesMLMetricSummary metricSummary : transaction.getValue().getMetrics().values()) {
            final String metricName = metricSummary.getMetric_name();
            if (!metricMap.containsKey(metricName)) {
              // there's no data in this time range for this metric, so we're  moving on.
              logger.info("In transaction {}, skipping {}", transaction.getValue().getTxn_name(), metricName);
              continue;
            }
            if (TimeUnit.MINUTES.toMillis(record.getAnalysisMinute()) > riskCutOff) {
              metricMap.get(metricSummary.getMetric_name()).updateRisk(metricSummary.getMax_risk());
            }

            metricMap.get(metricSummary.getMetric_name())
                .addToRiskMap(TimeUnit.MINUTES.toMillis(record.getAnalysisMinute()), metricSummary.getMax_risk());
            logger.info("Add transactionMetric {} {}", transaction.getValue().getTxn_name(), metricName);
          }
        }
      }
    } catch (Exception ex) {
      logger.error("Exception while computing timeSeries data for cvConfiguration: {}", cvConfiguration.getUuid());
      throw new WingsException("Exception while computing TimeSeries data", ex);
    }
  }

  public SortedSet<TransactionTimeSeries> getTimeSeriesOfHeatMapUnit(
      String accountId, String cvConfigId, long startTime, long endTime, long historyStartTime) {
    startTime = Timestamp.nextMinuteBoundary(startTime);
    endTime = Timestamp.minuteBoundary(endTime);
    historyStartTime = Timestamp.nextMinuteBoundary(historyStartTime);
    CVConfiguration cvConfiguration =
        wingsPersistence.createQuery(CVConfiguration.class).filter("_id", cvConfigId).get();
    if (cvConfiguration == null) {
      logger.info("No cvConfig found for cvConfigId={}", cvConfigId);
      return new TreeSet<>();
    }
    return convertTimeSeriesResponse(fetchObservedTimeSeries(startTime, endTime, cvConfiguration, historyStartTime));
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
}
