package software.wings.service.impl.analysis;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Collections.emptySet;
import static software.wings.sm.StateType.APP_DYNAMICS;

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
import software.wings.beans.FeatureName;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.common.VerificationConstants;
import software.wings.dl.WingsPersistence;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummary.EnvInfo;
import software.wings.security.PermissionAttribute.Action;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.verification.HeatMap;
import software.wings.verification.TimeSeriesDataPoint;
import software.wings.verification.appdynamics.AppDynamicsCVServiceConfiguration;
import software.wings.verification.dashboard.HeatMapUnit;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
public class ContinuousVerificationServiceImpl implements ContinuousVerificationService {
  @Inject protected WingsPersistence wingsPersistence;
  @Inject protected AuthService authService;
  @Inject protected FeatureFlagService featureFlagService;
  private static final Logger logger = LoggerFactory.getLogger(ContinuousVerificationServiceImpl.class);

  private static final int DURATION_IN_MINUTES = 10;

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

    // find the statuses of all the workflows we have.
    PageRequest<WorkflowExecution> workflowExecutionPageRequest =
        PageRequestBuilder.aPageRequest()
            .addFilter("_id", Operator.IN, deploymentData.keySet().toArray())
            .addFieldsIncluded("_id", "status", "startTs", "endTs", "name")
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
    }
    return new ArrayList(deploymentData.values());
  }

  /**
   * Check if the user has permissions to view the executionData.
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
   *
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

  private HeatMapUnit getMockHeatMapUnit(long startEpoch, long endEpoch, int eventsPerUnit, int durationInMinutes) {
    final int NUM_CLASSES = 4; // low / med / high / na

    int low = 0, medium = 0, high = 0, na = 0;

    Random random = new Random();

    if (eventsPerUnit == 1) {
      int randomNum = random.nextInt(NUM_CLASSES);
      if (randomNum == 0) {
        low = 1;
      } else if (randomNum == 1) {
        medium = 1;
      } else if (randomNum == 2) {
        high = 1;
      } else {
        na = 1;
      }

      return new HeatMapUnit(startEpoch, endEpoch, low, medium, high, na, null);
    }

    int randomNum = random.nextInt(eventsPerUnit);
    if (randomNum % 2 == 0) {
      low = randomNum;
    } else {
      medium = randomNum;
    }
    eventsPerUnit -= randomNum;
    if (eventsPerUnit > 0) {
      high = random.nextInt(eventsPerUnit);
      eventsPerUnit -= high;
    }
    if (eventsPerUnit > 0) {
      na = random.nextInt(eventsPerUnit);
    }
    return new HeatMapUnit(startEpoch, endEpoch, low, medium, high, na, null);
  }

  @NotNull
  private HeatMap generateMockHeatMap(long startTime, long endTime, int resolution, String appId, String serviceId) {
    HeatMap heatMap = new HeatMap();

    AppDynamicsCVServiceConfiguration appDynamicsCVServiceConfiguration;
    appDynamicsCVServiceConfiguration = new AppDynamicsCVServiceConfiguration();
    appDynamicsCVServiceConfiguration.setAppId(appId);
    appDynamicsCVServiceConfiguration.setEnvId(generateUuid());
    appDynamicsCVServiceConfiguration.setServiceId(serviceId);
    appDynamicsCVServiceConfiguration.setEnabled24x7(true);
    appDynamicsCVServiceConfiguration.setAppDynamicsApplicationId(generateUuid());
    appDynamicsCVServiceConfiguration.setTierId(generateUuid());
    appDynamicsCVServiceConfiguration.setConnectorId(generateUuid());
    appDynamicsCVServiceConfiguration.setStateType(APP_DYNAMICS);
    appDynamicsCVServiceConfiguration.setName("App Dynamics Service Config " + UUID.randomUUID().toString());
    appDynamicsCVServiceConfiguration.setConnectorName("Connector " + UUID.randomUUID().toString());
    heatMap.setCvConfiguration(appDynamicsCVServiceConfiguration);

    heatMap.setRiskLevelSummary(new ArrayList<>());

    if (resolution == 1) {
      long currentTs = startTime;
      long nextTs = currentTs + TimeUnit.MINUTES.toMillis(10);
      for (int i = 0; i < 6 * 24; i++) {
        heatMap.getRiskLevelSummary().add(getMockHeatMapUnit(currentTs, nextTs, 1, 10));
        currentTs = nextTs;
        nextTs = currentTs + TimeUnit.MINUTES.toMillis(DURATION_IN_MINUTES);
      }
      return heatMap;
    } else if (resolution == 7) {
      long currentTs = startTime;
      long nextTs = currentTs + TimeUnit.HOURS.toMillis(1);
      for (int i = 0; i < 24 * 7; i++) {
        heatMap.getRiskLevelSummary().add(getMockHeatMapUnit(currentTs, nextTs, 6, 60));
        currentTs = nextTs;
        nextTs = currentTs + TimeUnit.HOURS.toMillis(1);
      }
      return heatMap;
    } else if (resolution == 30) {
      long currentTs = startTime;
      long nextTs = currentTs + TimeUnit.HOURS.toMillis(4);
      for (int i = 0; i < 6 * 30; i++) {
        heatMap.getRiskLevelSummary().add(getMockHeatMapUnit(currentTs, nextTs, 24, 240));
        currentTs = nextTs;
        nextTs = currentTs + TimeUnit.HOURS.toMillis(4);
      }
      return heatMap;
    } else {
      throw new WingsException("Unsupported resolution provided. Resolution should be 1, 7 or 30.");
    }
  }

  public Map<String, List<HeatMap>> getHeatMap(
      String accountId, String serviceId, int resolution, long startTime, long endTime, boolean detailed) {
    // TODO: Fetch all CV configs of given serviceId and generate heatmaps for those configs

    final int NUM_SERVICE_CONFIGS = 2;

    // TODO: assert that (end - begin) == resolution

    Map<String, List<HeatMap>> resp = new LinkedHashMap<>();

    for (int i = 0; i < 3; i++) {
      List<HeatMap> summary = new ArrayList<>();
      String appId = UUID.randomUUID().toString();
      for (int num = 0; num < NUM_SERVICE_CONFIGS; num++) {
        HeatMap heatMap = generateMockHeatMap(startTime, endTime, resolution, appId, serviceId);
        if (detailed) {
          heatMap.setObservedTimeSeries(getObservedTimeSeries(accountId, serviceId, resolution, startTime, endTime));
          heatMap.setPredictedTimeSeries(getObservedTimeSeries(accountId, serviceId, resolution, startTime, endTime));
        }
        summary.add(heatMap);
      }
      resp.put("Environment Name " + UUID.randomUUID().toString(), summary);
    }
    return resp;
  }

  private Map<String, Map<String, List<TimeSeriesDataPoint>>> getObservedTimeSeries(
      String accountId, String serviceId, int resolution, long startTime, long endTime) {
    int minutes = (int) ((endTime - startTime) / 60);
    int NUM_TRANSACTIONS = 3;
    List<String> metrics = Arrays.asList("apdexScore", "requestsPerMinute", "averageResponseTime");

    Map<String, Map<String, List<TimeSeriesDataPoint>>> timeSeriesMap = new HashMap<>();
    for (int i = 0; i < NUM_TRANSACTIONS; i++) {
      String transactionName = "WebTransaction/Servlet/" + generateUuid();
      Map<String, List<TimeSeriesDataPoint>> metricTimeSeries = new HashMap<>();
      for (String metric : metrics) {
        metricTimeSeries.put(metric, generateRandomTimeSeriesWithNDataPoints(startTime, minutes));
      }
      timeSeriesMap.put(transactionName, metricTimeSeries);
    }
    return timeSeriesMap;
  }

  private List<TimeSeriesDataPoint> generateRandomTimeSeriesWithNDataPoints(long startTime, int count) {
    Random random = new Random();
    List<TimeSeriesDataPoint> timeSeries = new ArrayList<>();
    long currentTs = startTime;
    for (int i = 0; i < count; i++) {
      timeSeries.add(new TimeSeriesDataPoint(currentTs, random.nextFloat() * random.nextInt(10)));
      currentTs = currentTs + TimeUnit.MINUTES.toMillis(1);
    }
    return timeSeries;
  }

  public Map<String, Map<String, List<TimeSeriesDataPoint>>> getTimeSeriesOfHeatMapUnit(
      String accountId, String cvConfigId, long startTime, long endTime) {
    // TODO: RBAC check on cvConfigId
    int minutes = (int) ((endTime - startTime) / 60);

    int NUM_TRANSACTIONS = 3;
    List<String> metrics = Arrays.asList("apdexScore", "requestsPerMinute", "averageResponseTime");

    Map<String, Map<String, List<TimeSeriesDataPoint>>> timeSeriesMap = new HashMap<>();
    for (int i = 0; i < NUM_TRANSACTIONS; i++) {
      String transactionName = "WebTransaction/Servlet/" + generateUuid();
      Map<String, List<TimeSeriesDataPoint>> metricTimeSeries = new HashMap<>();
      for (String metric : metrics) {
        metricTimeSeries.put(metric, getTimeSeriesDataPointsFor24Hours(startTime, minutes));
      }
      timeSeriesMap.put(transactionName, metricTimeSeries);
    }
    return timeSeriesMap;
  }

  @NotNull
  private List<TimeSeriesDataPoint> getTimeSeriesDataPointsFor24Hours(long startTime, int durationOfCurrentSquare) {
    List<TimeSeriesDataPoint> resp = new ArrayList<>();
    List<TimeSeriesDataPoint> before =
        generateRandomTimeSeriesWithNDataPoints(startTime - TimeUnit.HOURS.toMillis(12), 12 * 60);
    List<TimeSeriesDataPoint> timeSeriesForGivenSquare =
        generateRandomTimeSeriesWithNDataPoints(startTime, durationOfCurrentSquare);
    List<TimeSeriesDataPoint> after = generateRandomTimeSeriesWithNDataPoints(
        startTime + TimeUnit.MINUTES.toMillis(durationOfCurrentSquare), (12 * 60) - durationOfCurrentSquare);
    resp.addAll(before);
    resp.addAll(timeSeriesForGivenSquare);
    resp.addAll(after);
    logger.info("Response size = " + resp.size());
    return resp;
  }
}
