package io.harness.ng.cdOverview.service;

import static io.harness.NGDateUtils.DAY_IN_MS;
import static io.harness.NGDateUtils.HOUR_IN_MS;
import static io.harness.NGDateUtils.getNumberOfDays;
import static io.harness.NGDateUtils.getStartTimeOfNextDay;
import static io.harness.NGDateUtils.getStartTimeOfPreviousInterval;
import static io.harness.NGDateUtils.getStartTimeOfTheDayAsEpoch;
import static io.harness.event.timeseries.processor.utils.DateUtils.getCurrentTime;
import static io.harness.ng.core.activityhistory.dto.TimeGroupType.DAY;
import static io.harness.ng.core.activityhistory.dto.TimeGroupType.HOUR;

import io.harness.NGDateUtils;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cd.NGPipelineSummaryCDConstants;
import io.harness.cd.NGServiceConstants;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.InstancesByBuildId;
import io.harness.models.dashboard.InstanceCountDetailsByEnvTypeBase;
import io.harness.ng.cdOverview.dto.BuildIdAndInstanceCount;
import io.harness.ng.cdOverview.dto.DashboardDeploymentActiveFailedRunningInfo;
import io.harness.ng.cdOverview.dto.DashboardWorkloadDeployment;
import io.harness.ng.cdOverview.dto.Deployment;
import io.harness.ng.cdOverview.dto.DeploymentCount;
import io.harness.ng.cdOverview.dto.DeploymentDateAndCount;
import io.harness.ng.cdOverview.dto.DeploymentInfo;
import io.harness.ng.cdOverview.dto.DeploymentStatusInfo;
import io.harness.ng.cdOverview.dto.DeploymentStatusInfoList;
import io.harness.ng.cdOverview.dto.EntityStatusDetails;
import io.harness.ng.cdOverview.dto.EnvBuildIdAndInstanceCountInfo;
import io.harness.ng.cdOverview.dto.EnvBuildIdAndInstanceCountInfoList;
import io.harness.ng.cdOverview.dto.ExecutionDeployment;
import io.harness.ng.cdOverview.dto.ExecutionDeploymentInfo;
import io.harness.ng.cdOverview.dto.HealthDeploymentDashboard;
import io.harness.ng.cdOverview.dto.HealthDeploymentInfo;
import io.harness.ng.cdOverview.dto.InstancesByBuildIdList;
import io.harness.ng.cdOverview.dto.LastWorkloadInfo;
import io.harness.ng.cdOverview.dto.ServiceDeployment;
import io.harness.ng.cdOverview.dto.ServiceDeploymentInfo;
import io.harness.ng.cdOverview.dto.ServiceDeploymentInfoDTO;
import io.harness.ng.cdOverview.dto.ServiceDeploymentListInfo;
import io.harness.ng.cdOverview.dto.ServiceDetailsDTO;
import io.harness.ng.cdOverview.dto.ServiceDetailsInfoDTO;
import io.harness.ng.cdOverview.dto.ServicePipelineInfo;
import io.harness.ng.cdOverview.dto.TimeAndStatusDeployment;
import io.harness.ng.cdOverview.dto.TimeValuePairListDTO;
import io.harness.ng.cdOverview.dto.TotalDeploymentInfo;
import io.harness.ng.cdOverview.dto.WorkloadCountInfo;
import io.harness.ng.cdOverview.dto.WorkloadDateCountInfo;
import io.harness.ng.cdOverview.dto.WorkloadDeploymentInfo;
import io.harness.ng.cdOverview.util.GrowthTrendEvaluator;
import io.harness.ng.core.activityhistory.dto.TimeGroupType;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.services.ServiceEntityService;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.service.instancedashboardservice.InstanceDashboardService;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(HarnessTeam.CDC)
@Singleton
@Slf4j
public class CDOverviewDashboardServiceImpl implements CDOverviewDashboardService {
  @Inject TimeScaleDBService timeScaleDBService;
  @Inject ServiceEntityService serviceEntityService;
  @Inject InstanceDashboardService instanceDashboardService;

  private String tableNameCD = "pipeline_execution_summary_cd";
  private String tableNameServiceAndInfra = "service_infra_info";
  private List<String> failedStatusList = Arrays.asList(ExecutionStatus.FAILED.name(), ExecutionStatus.ABORTED.name(),
      ExecutionStatus.EXPIRED.name(), ExecutionStatus.IGNOREFAILED.name(), ExecutionStatus.ERRORED.name());
  private List<String> activeStatusList =
      Arrays.asList(ExecutionStatus.RUNNING.name(), ExecutionStatus.ASYNCWAITING.name(),
          ExecutionStatus.TASKWAITING.name(), ExecutionStatus.TIMEDWAITING.name(), ExecutionStatus.PAUSED.name());
  private List<String> pendingStatusList = Arrays.asList(ExecutionStatus.INTERVENTIONWAITING.name(),
      ExecutionStatus.APPROVALWAITING.name(), ExecutionStatus.WAITING.name(), ExecutionStatus.RESOURCEWAITING.name());
  private static final int MAX_RETRY_COUNT = 5;

  public String queryBuilderSelectStatusTime(
      String accountId, String orgId, String projectId, long startInterval, long endInterval) {
    String selectStatusQuery = "select status,startts from " + tableNameCD + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectStatusQuery);

    if (accountId != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountId));
    }

    if (orgId != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgId));
    }

    if (projectId != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s' and ", projectId));
    }

    if (startInterval > 0 && endInterval > 0) {
      totalBuildSqlBuilder.append(
          String.format("startts is not null and startts>=%s and startts<%s;", startInterval, endInterval));
    }

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderSelectIdCdTable(
      String accountId, String orgId, String projectId, long startInterval, long endInterval) {
    String selectStatusQuery = "select id from " + tableNameCD + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectStatusQuery);

    if (accountId != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountId));
    }

    if (orgId != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgId));
    }

    if (projectId != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s' and ", projectId));
    }

    if (startInterval > 0 && endInterval > 0) {
      totalBuildSqlBuilder.append(
          String.format("startts is not null and startts>=%s and startts<%s;", startInterval, endInterval));
    }

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderSelectIdLimitTimeCdTable(
      String accountId, String orgId, String projectId, long days, List<String> statusList) {
    String selectStatusQuery = "select id from " + tableNameCD + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectStatusQuery);

    if (accountId != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountId));
    }

    if (orgId != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgId));
    }

    if (projectId != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s' and ", projectId));
    }

    totalBuildSqlBuilder.append("status in (");
    for (String status : statusList) {
      totalBuildSqlBuilder.append(String.format("'%s',", status));
    }

    totalBuildSqlBuilder.deleteCharAt(totalBuildSqlBuilder.length() - 1);

    totalBuildSqlBuilder.append(String.format(") and startts is not null ORDER BY startts DESC LIMIT %s", days));

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderEnvironmentType(
      String accountId, String orgId, String projectId, long startInterval, long endInterval) {
    String selectStatusQuery = "select env_type from " + tableNameServiceAndInfra + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectStatusQuery);

    if (startInterval > 0 && endInterval > 0) {
      String idQuery = queryBuilderSelectIdCdTable(accountId, orgId, projectId, startInterval, endInterval);
      idQuery = idQuery.replace(';', ' ');
      totalBuildSqlBuilder.append(
          String.format("pipeline_execution_summary_cd_id in (%s) and env_type is not null;", idQuery));
    }

    return totalBuildSqlBuilder.toString();
  }
  public double getRate(long current, long previous) {
    double rate = 0.0;
    if (previous != 0) {
      rate = (current - previous) / (double) previous;
    }
    rate = rate * 100.0;
    return rate;
  }

  public String queryBuilderStatus(
      String accountId, String orgId, String projectId, long days, List<String> statusList) {
    String selectStatusQuery = "select id,name,startts,endTs,status from " + tableNameCD + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectStatusQuery);

    if (accountId != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountId));
    }

    if (orgId != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgId));
    }

    if (projectId != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s' and ", projectId));
    }

    totalBuildSqlBuilder.append("status in (");
    for (String status : statusList) {
      totalBuildSqlBuilder.append(String.format("'%s',", status));
    }

    totalBuildSqlBuilder.deleteCharAt(totalBuildSqlBuilder.length() - 1);

    totalBuildSqlBuilder.append(String.format(") and startts is not null ORDER BY startts DESC LIMIT %s;", days));

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderServiceTag(String queryIdCdTable) {
    String selectStatusQuery =
        "select service_name,tag,pipeline_execution_summary_cd_id from " + tableNameServiceAndInfra + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder(20480);

    totalBuildSqlBuilder.append(String.format(
        selectStatusQuery + "pipeline_execution_summary_cd_id in (%s) and service_name is not null;", queryIdCdTable));

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderSelectWorkload(String accountId, String orgId, String projectId, long previousStartInterval,
      long endInterval, EnvironmentType envType) {
    String selectStatusQuery =
        "select service_name,service_id,service_status as status,service_startts as startts,service_endts as endts,deployment_type, pipeline_execution_summary_cd_id from "
        + tableNameServiceAndInfra + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectStatusQuery);

    if (previousStartInterval > 0 && endInterval > 0) {
      String idQuery = queryBuilderSelectIdCdTable(accountId, orgId, projectId, previousStartInterval, endInterval);
      idQuery = idQuery.replace(';', ' ');

      if (envType != null) {
        totalBuildSqlBuilder.append(String.format("env_Type='%s' and ", envType.toString()));
      }

      totalBuildSqlBuilder.append(String.format(
          "pipeline_execution_summary_cd_id in (%s) and service_name is not null and service_id is not null;",
          idQuery));
    }

    return totalBuildSqlBuilder.toString();
  }

  public TimeAndStatusDeployment queryCalculatorTimeAndStatus(String query) {
    List<Long> time = new ArrayList<>();
    List<String> status = new ArrayList<>();

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          status.add(resultSet.getString("status"));
          time.add(Long.valueOf(resultSet.getString("startts")));
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }

    return TimeAndStatusDeployment.builder().status(status).time(time).build();
  }

  public List<String> queryCalculatorEnvType(String queryEnvironmentType) {
    List<String> envType = new ArrayList<>();

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(queryEnvironmentType)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          envType.add(resultSet.getString("env_type"));
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return envType;
  }

  @Override
  public HealthDeploymentDashboard getHealthDeploymentDashboard(String accountId, String orgId, String projectId,
      long startInterval, long endInterval, long previousStartInterval) {
    endInterval = endInterval + getTimeUnitToGroupBy(DAY);
    String query = queryBuilderSelectStatusTime(accountId, orgId, projectId, previousStartInterval, endInterval);

    List<Long> time = new ArrayList<>();
    List<String> status = new ArrayList<>();
    List<String> envType = new ArrayList<>();

    TimeAndStatusDeployment timeAndStatusDeployment = queryCalculatorTimeAndStatus(query);
    time = timeAndStatusDeployment.getTime();
    status = timeAndStatusDeployment.getStatus();

    long total = 0;
    long currentSuccess = 0;
    long currentFailed = 0;
    long previousSuccess = 0;
    long previousFailed = 0;

    HashMap<Long, Integer> totalCountMap = new HashMap<>();
    HashMap<Long, Integer> successCountMap = new HashMap<>();
    HashMap<Long, Integer> failedCountMap = new HashMap<>();

    long startDateCopy = startInterval;
    long endDateCopy = endInterval;

    long timeUnitPerDay = getTimeUnitToGroupBy(DAY);
    while (startDateCopy < endDateCopy) {
      totalCountMap.put(startDateCopy, 0);
      successCountMap.put(startDateCopy, 0);
      failedCountMap.put(startDateCopy, 0);
      startDateCopy = startDateCopy + timeUnitPerDay;
    }

    for (int i = 0; i < time.size(); i++) {
      long currentTimeEpoch = time.get(i);
      if (currentTimeEpoch >= startInterval && currentTimeEpoch < endInterval) {
        currentTimeEpoch = getStartingDateEpochValue(currentTimeEpoch, startInterval);
        total++;
        totalCountMap.put(currentTimeEpoch, totalCountMap.get(currentTimeEpoch) + 1);
        if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
          currentSuccess++;
          successCountMap.put(currentTimeEpoch, successCountMap.get(currentTimeEpoch) + 1);
        } else if (failedStatusList.contains(status.get(i))) {
          currentFailed++;
          failedCountMap.put(currentTimeEpoch, failedCountMap.get(currentTimeEpoch) + 1);
        }
      } else {
        if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
          previousSuccess++;
        } else if (failedStatusList.contains(status.get(i))) {
          previousFailed++;
        }
      }
    }

    String queryEnvironmentType = queryBuilderEnvironmentType(accountId, orgId, projectId, startInterval, endInterval);
    envType = queryCalculatorEnvType(queryEnvironmentType);

    long production = Collections.frequency(envType, EnvironmentType.Production.name());
    long nonProduction = Collections.frequency(envType, EnvironmentType.PreProduction.name());

    List<DeploymentDateAndCount> totalDateAndCount = new ArrayList<>();
    List<DeploymentDateAndCount> successDateAndCount = new ArrayList<>();
    List<DeploymentDateAndCount> failedDateAndCount = new ArrayList<>();
    startDateCopy = startInterval;
    endDateCopy = endInterval;

    while (startDateCopy < endDateCopy) {
      totalDateAndCount.add(DeploymentDateAndCount.builder()
                                .time(startDateCopy)
                                .deployments(Deployment.builder().count(totalCountMap.get(startDateCopy)).build())
                                .build());
      successDateAndCount.add(DeploymentDateAndCount.builder()
                                  .time(startDateCopy)
                                  .deployments(Deployment.builder().count(successCountMap.get(startDateCopy)).build())
                                  .build());
      failedDateAndCount.add(DeploymentDateAndCount.builder()
                                 .time(startDateCopy)
                                 .deployments(Deployment.builder().count(failedCountMap.get(startDateCopy)).build())
                                 .build());
      startDateCopy = startDateCopy + timeUnitPerDay;
    }

    return HealthDeploymentDashboard.builder()
        .healthDeploymentInfo(HealthDeploymentInfo.builder()
                                  .total(TotalDeploymentInfo.builder()
                                             .count(total)
                                             .production(production)
                                             .nonProduction(nonProduction)
                                             .countList(totalDateAndCount)
                                             .build())
                                  .success(DeploymentInfo.builder()
                                               .count(currentSuccess)
                                               .rate(getRate(currentSuccess, previousSuccess))
                                               .countList(successDateAndCount)
                                               .build())
                                  .failure(DeploymentInfo.builder()
                                               .count(currentFailed)
                                               .rate(getRate(currentFailed, previousFailed))
                                               .countList(failedDateAndCount)
                                               .build())
                                  .build())
        .build();
  }

  private ExecutionDeployment getExecutionDeployment(Long time, long total, long success, long failed) {
    return ExecutionDeployment.builder()
        .time(time)
        .deployments(DeploymentCount.builder().total(total).success(success).failure(failed).build())
        .build();
  }

  public HashMap<String, List<ServiceDeploymentInfo>> queryCalculatorServiceTagMag(String queryServiceTag) {
    HashMap<String, List<ServiceDeploymentInfo>> serviceTagMap = new HashMap<>();

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(queryServiceTag)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          String planExecutionId = resultSet.getString("pipeline_execution_summary_cd_id");
          String service_name = resultSet.getString("service_name");
          String tag = resultSet.getString("tag");
          if (serviceTagMap.containsKey(planExecutionId)) {
            serviceTagMap.get(planExecutionId).add(getServiceDeployment(service_name, tag));
          } else {
            List<ServiceDeploymentInfo> serviceDeploymentInfos = new ArrayList<>();
            serviceDeploymentInfos.add(getServiceDeployment(service_name, tag));
            serviceTagMap.put(planExecutionId, serviceDeploymentInfos);
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return serviceTagMap;
  }

  @Override
  public ExecutionDeploymentInfo getExecutionDeploymentDashboard(
      String accountId, String orgId, String projectId, long startInterval, long endInterval) {
    endInterval = endInterval + DAY_IN_MS;
    String query = queryBuilderSelectStatusTime(accountId, orgId, projectId, startInterval, endInterval);

    HashMap<Long, Integer> totalCountMap = new HashMap<>();
    HashMap<Long, Integer> successCountMap = new HashMap<>();
    HashMap<Long, Integer> failedCountMap = new HashMap<>();

    long startDateCopy = startInterval;
    long endDateCopy = endInterval;

    long timeUnitPerDay = getTimeUnitToGroupBy(DAY);
    while (startDateCopy < endDateCopy) {
      totalCountMap.put(startDateCopy, 0);
      successCountMap.put(startDateCopy, 0);
      failedCountMap.put(startDateCopy, 0);
      startDateCopy = startDateCopy + timeUnitPerDay;
    }

    TimeAndStatusDeployment timeAndStatusDeployment = queryCalculatorTimeAndStatus(query);
    List<Long> time = timeAndStatusDeployment.getTime();
    List<String> status = timeAndStatusDeployment.getStatus();

    List<ExecutionDeployment> executionDeployments = new ArrayList<>();

    for (int i = 0; i < time.size(); i++) {
      long currentTimeEpoch = time.get(i);
      currentTimeEpoch = getStartingDateEpochValue(currentTimeEpoch, startInterval);
      totalCountMap.put(currentTimeEpoch, totalCountMap.get(currentTimeEpoch) + 1);
      if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
        successCountMap.put(currentTimeEpoch, successCountMap.get(currentTimeEpoch) + 1);
      } else if (failedStatusList.contains(status.get(i))) {
        failedCountMap.put(currentTimeEpoch, failedCountMap.get(currentTimeEpoch) + 1);
      }
    }

    startDateCopy = startInterval;
    endDateCopy = endInterval;

    while (startDateCopy < endDateCopy) {
      executionDeployments.add(getExecutionDeployment(startDateCopy, totalCountMap.get(startDateCopy),
          successCountMap.get(startDateCopy), failedCountMap.get(startDateCopy)));
      startDateCopy = startDateCopy + timeUnitPerDay;
    }
    return ExecutionDeploymentInfo.builder().executionDeploymentList(executionDeployments).build();
  }

  @Override
  public ServiceDetailsInfoDTO getServiceDetailsList(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, long startTime, long endTime) throws Exception {
    startTime = getStartTimeOfTheDayAsEpoch(startTime);
    endTime = getStartTimeOfNextDay(endTime);

    long numberOfDays = getNumberOfDays(startTime, endTime);

    if (numberOfDays < 0) {
      throw new Exception("start date should be less than or equal to end date");
    }
    long previousStartTime = getStartTimeOfPreviousInterval(startTime, numberOfDays);
    DashboardWorkloadDeployment dashboardWorkloadDeployment = getDashboardWorkloadDeployment(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime, previousStartTime, null);

    List<WorkloadDeploymentInfo> workloadDeploymentInfoList =
        dashboardWorkloadDeployment.getWorkloadDeploymentInfoList();

    // Stores pipeline details corresponding to a pipeline execution id
    Map<String, ServicePipelineInfo> pipelineExecutionDetailsMap = new HashMap<>();
    List<String> pipelineExecutionIdList = workloadDeploymentInfoList.stream()
                                               .map(WorkloadDeploymentInfo::getLastPipelineExecutionId)
                                               .collect(Collectors.toList());

    // Gets all the details for the pipeline execution id's in the list and stores it in a map.
    getPipelineExecutionDetails(pipelineExecutionDetailsMap, pipelineExecutionIdList);

    List<ServiceDetailsDTO> serviceDeploymentInfoList =
        workloadDeploymentInfoList.stream()
            .map(item
                -> ServiceDetailsDTO.builder()
                       .serviceName(item.getServiceName())
                       .serviceIdentifier(item.getServiceId())
                       .deploymentTypeList(item.getDeploymentTypeList())
                       .totalDeployments(item.getTotalDeployments())
                       .totalDeploymentChangeRate(item.getTotalDeploymentChangeRate())
                       .successRate(item.getPercentSuccess())
                       .successRateChangeRate(item.getRateSuccess())
                       .failureRate(item.getFailureRate())
                       .failureRateChangeRate(item.getFailureRateChangeRate())
                       .frequency(item.getFrequency())
                       .frequencyChangeRate(item.getFrequencyChangeRate())
                       .lastPipelineExecuted(
                           pipelineExecutionDetailsMap.getOrDefault(item.getLastPipelineExecutionId(), null))
                       .build())
            .collect(Collectors.toList());

    return ServiceDetailsInfoDTO.builder().serviceDeploymentDetailsList(serviceDeploymentInfoList).build();
  }

  private void getPipelineExecutionDetails(
      Map<String, ServicePipelineInfo> pipelineExecutionDetailsMap, List<String> pipelineExecutionIdList) {
    int totalTries = 0;
    boolean successfulOperation = false;
    String sql = "select * from " + tableNameCD + " where id = any (?);";
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(sql)) {
        final Array array = connection.createArrayOf("VARCHAR", pipelineExecutionIdList.toArray());
        statement.setArray(1, array);
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          String pipelineExecutionId = resultSet.getString(NGPipelineSummaryCDConstants.ID);
          String pipelineName = resultSet.getString(NGPipelineSummaryCDConstants.NAME);
          String pipelineId = resultSet.getString(NGPipelineSummaryCDConstants.PIPELINE_IDENTIFIER);
          String status = resultSet.getString(NGPipelineSummaryCDConstants.STATUS);
          long executionTime = Long.parseLong(resultSet.getString(NGPipelineSummaryCDConstants.START_TS));
          if (!pipelineExecutionDetailsMap.containsKey(pipelineExecutionId)) {
            pipelineExecutionDetailsMap.put(pipelineExecutionId,
                ServicePipelineInfo.builder()
                    .identifier(pipelineId)
                    .pipelineExecutionId(pipelineExecutionId)
                    .name(pipelineName)
                    .lastExecutedAt(executionTime)
                    .status(status)
                    .build());
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        log.error("%s after total tries = %s", ex, totalTries);
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
  }

  @Override
  public ServiceDeploymentInfoDTO getServiceDeployments(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, long startTime, long endTime, String serviceIdentifier, long bucketSizeInDays) {
    startTime = getStartTimeOfTheDayAsEpoch(startTime);
    endTime = getStartTimeOfNextDay(endTime);
    String query = queryBuilderServiceDeployments(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime, bucketSizeInDays, serviceIdentifier);

    /**
     * Map that stores service deployment data for a bucket time - starting time of a
     * dateCDOverviewDashboardServiceImpl.java
     */
    Map<Long, ServiceDeployment> resultMap = new HashMap<>();
    long startTimeCopy = startTime;

    initializeResultMap(resultMap, startTimeCopy, endTime, bucketSizeInDays);

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          String status = resultSet.getString(NGServiceConstants.STATUS);
          long bucketTime = Long.parseLong(resultSet.getString(NGServiceConstants.TIME_ENTITY));
          long numberOfRecords = resultSet.getLong(NGServiceConstants.NUMBER_OF_RECORDS);
          ServiceDeployment serviceDeployment = resultMap.get(bucketTime);
          DeploymentCount deployments = serviceDeployment.getDeployments();
          deployments.setTotal(deployments.getTotal() + numberOfRecords);
          if ((ExecutionStatus.SUCCESS).toString().equals(status)) {
            deployments.setSuccess(deployments.getSuccess() + numberOfRecords);
          } else if (failedStatusList.contains(status)) {
            deployments.setFailure(deployments.getFailure() + numberOfRecords);
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        log.error("%s after total tries = %s", ex, totalTries);
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    List<ServiceDeployment> serviceDeploymentList = resultMap.values().stream().collect(Collectors.toList());
    return ServiceDeploymentInfoDTO.builder().serviceDeploymentList(serviceDeploymentList).build();
  }

  private void initializeResultMap(
      Map<Long, ServiceDeployment> resultMap, long startTime, long endTime, long bucketSizeInDays) {
    long bucketSizeInMS = bucketSizeInDays * DAY_IN_MS;
    while (startTime < endTime) {
      resultMap.put(startTime,
          ServiceDeployment.builder()
              .time(startTime)
              .deployments(DeploymentCount.builder().total(0).failure(0).success(0).build())
              .build());
      startTime = startTime + bucketSizeInMS;
    }
  }

  /**
   * Sample Query for queryBuilderServiceDeployments():
   * select status, time_entity, count(*) as records from (select service_status as status, service_startts as
   * execution_time, time_bucket_gapfill(86400000, service_startts, 1620000000000, 1620864000000)as time_entity,
   * pipeline_execution_summary_cd_id  from service_infra_info where pipeline_execution_summary_cd_id in (select id from
   * pipeline_execution_summary_cd where accountid='accountId' and orgidentifier='orgId' and
   * projectidentifier='projectId') and service_startts >= 1620000000000 and service_startts < 1620950400000) as
   * innertable group by status, time_entity;
   */
  public String queryBuilderServiceDeployments(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      long startTime, long endTime, long bucketSizeInDays, String serviceIdentifier) {
    long bucketSizeInMS = bucketSizeInDays * DAY_IN_MS;
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    String selectQuery =
        "select status, time_entity, COUNT(*) as numberOfRecords from (select service_status as status, service_startts as execution_time, ";
    totalBuildSqlBuilder.append(selectQuery)
        .append(String.format(
            "time_bucket_gapfill(%s, service_startts, %s, %s) as time_entity, pipeline_execution_summary_cd_id  from service_infra_info where pipeline_execution_summary_cd_id in ",
            bucketSizeInMS, startTime, endTime));
    String selectPipelineIdQuery = "(select id from " + tableNameCD + " where ";
    totalBuildSqlBuilder.append(selectPipelineIdQuery);
    if (accountIdentifier != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountIdentifier));
    }

    if (orgIdentifier != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgIdentifier));
    }

    if (projectIdentifier != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s') and ", projectIdentifier));
    }

    if (serviceIdentifier != null) {
      totalBuildSqlBuilder.append(String.format("service_id='%s') and ", serviceIdentifier));
    }

    totalBuildSqlBuilder.append(String.format(
        "service_startts>=%s and service_startts<%s) as innertable group by status, time_entity;", startTime, endTime));

    return totalBuildSqlBuilder.toString();
  }

  private static void validateBucketSize(long numberOfDays, long bucketSizeInDays) throws Exception {
    if (numberOfDays < bucketSizeInDays) {
      throw new Exception("Bucket size should be less than the number of days in the selected time range");
    }
  }

  @Override
  public ServiceDeploymentListInfo getServiceDeploymentsInfo(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, long startTime, long endTime, String serviceIdentifier, long bucketSizeInDays)
      throws Exception {
    long numberOfDays = getNumberOfDays(startTime, endTime);
    validateBucketSize(numberOfDays, bucketSizeInDays);
    long prevStartTime = startTime - (endTime - startTime + DAY_IN_MS);
    long prevEndTime = startTime - DAY_IN_MS;

    ServiceDeploymentInfoDTO serviceDeployments = getServiceDeployments(
        accountIdentifier, orgIdentifier, projectIdentifier, startTime, endTime, serviceIdentifier, bucketSizeInDays);
    List<ServiceDeployment> serviceDeploymentList = serviceDeployments.getServiceDeploymentList();

    ServiceDeploymentInfoDTO prevServiceDeployment = getServiceDeployments(accountIdentifier, orgIdentifier,
        projectIdentifier, prevStartTime, prevEndTime, serviceIdentifier, bucketSizeInDays);
    List<ServiceDeployment> prevServiceDeploymentList = prevServiceDeployment.getServiceDeploymentList();

    long totalDeployments = getTotalDeployments(serviceDeploymentList);
    long prevTotalDeployments = getTotalDeployments(prevServiceDeploymentList);
    double failureRate = getFailureRate(serviceDeploymentList);
    double frequency = totalDeployments / numberOfDays;
    double prevFrequency = prevTotalDeployments / numberOfDays;

    double totalDeloymentChangeRate = (totalDeployments - prevTotalDeployments) * 100;
    if (prevTotalDeployments != 0) {
      totalDeloymentChangeRate = totalDeloymentChangeRate / prevTotalDeployments;
    }
    double failureRateChangeRate = getFailureRateChangeRate(serviceDeploymentList, prevServiceDeploymentList);
    double frequencyChangeRate = calculateChangeRate(prevFrequency, frequency);

    return ServiceDeploymentListInfo.builder()
        .startTime(startTime)
        .endTime(endTime)
        .totalDeployments(totalDeployments)
        .failureRate(failureRate)
        .frequency(frequency)
        .totalDeploymentsChangeRate(totalDeloymentChangeRate)
        .failureRateChangeRate(failureRateChangeRate)
        .frequencyChangeRate(frequencyChangeRate)
        .serviceDeploymentList(serviceDeploymentList)
        .build();
  }

  /**
   * This API processes all services for given combination of identifiers and produces list of data points
   * determining the active number of services at particular timestamps, distanced by equal quantity
   * determined by the groupBy param
   * @param accountIdentifier
   * @param orgIdentifier
   * @param projectIdentifier
   * @param startTimeInMs start time of the search interval
   * @param endTimeInMs end time of the search interval
   * @param timeGroupType groupBy param to determine the discreteness of the growth trend
   * @return
   */
  @Override
  public TimeValuePairListDTO<Integer> getServicesGrowthTrend(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, long startTimeInMs, long endTimeInMs, TimeGroupType timeGroupType) {
    // Fetch all services for given accId + orgId + projectId including deleted ones in ASC order of creation time
    List<ServiceEntity> serviceEntities =
        serviceEntityService.getAllServices(accountIdentifier, orgIdentifier, projectIdentifier);

    // Create List<EntityStatusDetails> out of service entity list to create growth trend out of it
    List<EntityStatusDetails> entities = new ArrayList<>();
    serviceEntities.forEach(serviceEntity
        -> entities.add(new EntityStatusDetails(
            serviceEntity.getCreatedAt(), serviceEntity.getDeleted(), serviceEntity.getDeletedAt())));

    return new TimeValuePairListDTO<>(
        GrowthTrendEvaluator.getGrowthTrend(entities, startTimeInMs, endTimeInMs, timeGroupType));
  }

  private double getFailureRateChangeRate(
      List<ServiceDeployment> executionDeploymentList, List<ServiceDeployment> prevExecutionDeploymentList) {
    double failureRate = getFailureRate(executionDeploymentList);
    double prevFailureRate = getFailureRate(prevExecutionDeploymentList);
    return calculateChangeRate(prevFailureRate, failureRate);
  }

  private double getFailureRate(List<ServiceDeployment> executionDeploymentList) {
    long totalDeployments = executionDeploymentList.stream()
                                .map(ServiceDeployment::getDeployments)
                                .mapToLong(DeploymentCount::getTotal)
                                .sum();
    long totalFailure = executionDeploymentList.stream()
                            .map(ServiceDeployment::getDeployments)
                            .mapToLong(DeploymentCount::getFailure)
                            .sum();
    double failureRate = totalFailure * 100;
    if (totalDeployments != 0) {
      failureRate = failureRate / totalDeployments;
    }
    return failureRate;
  }
  private double calculateChangeRate(double prevValue, double curValue) {
    double rate = (curValue - prevValue) * 100;
    if (prevValue != 0) {
      rate = rate / prevValue;
    }
    return rate;
  }

  private long getTotalDeployments(List<ServiceDeployment> executionDeploymentList) {
    long total = 0;
    for (ServiceDeployment item : executionDeploymentList) {
      total += item.getDeployments().getTotal();
    }
    return total;
  }

  public DeploymentStatusInfoList queryCalculatorDeploymentInfo(String queryStatus) {
    List<String> planExecutionIdList = new ArrayList<>();
    List<String> namePipelineList = new ArrayList<>();
    List<Long> startTs = new ArrayList<>();
    List<Long> endTs = new ArrayList<>();
    List<String> deploymentStatus = new ArrayList<>();

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(queryStatus)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          planExecutionIdList.add(resultSet.getString("id"));
          namePipelineList.add(resultSet.getString("name"));
          startTs.add(Long.valueOf(resultSet.getString("startts")));
          deploymentStatus.add(resultSet.getString("status"));
          if (resultSet.getString("endTs") != null) {
            endTs.add(Long.valueOf(resultSet.getString("endTs")));
          } else {
            endTs.add(new Date().getTime());
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return DeploymentStatusInfoList.builder()
        .planExecutionIdList(planExecutionIdList)
        .deploymentStatus(deploymentStatus)
        .endTs(endTs)
        .namePipelineList(namePipelineList)
        .startTs(startTs)
        .build();
  }

  public List<DeploymentStatusInfo> getDeploymentStatusInfo(String queryStatus, String queryServiceNameTagId) {
    List<String> planExecutionIdList = new ArrayList<>();
    List<String> namePipelineList = new ArrayList<>();
    List<Long> startTs = new ArrayList<>();
    List<Long> endTs = new ArrayList<>();
    List<String> deploymentStatus = new ArrayList<>();

    HashMap<String, List<ServiceDeploymentInfo>> serviceTagMap = new HashMap<>();

    DeploymentStatusInfoList deploymentStatusInfoList = queryCalculatorDeploymentInfo(queryStatus);
    deploymentStatus = deploymentStatusInfoList.getDeploymentStatus();
    endTs = deploymentStatusInfoList.getEndTs();
    namePipelineList = deploymentStatusInfoList.getNamePipelineList();
    planExecutionIdList = deploymentStatusInfoList.getPlanExecutionIdList();
    startTs = deploymentStatusInfoList.getStartTs();

    String queryServiceTag = queryBuilderServiceTag(queryServiceNameTagId);

    serviceTagMap = queryCalculatorServiceTagMag(queryServiceTag);

    List<DeploymentStatusInfo> statusInfo = new ArrayList<>();
    for (int i = 0; i < planExecutionIdList.size(); i++) {
      String planExecutionId = planExecutionIdList.get(i);
      long startTime = startTs.get(i);
      long endTime = endTs.get(i);
      statusInfo.add(this.getDeploymentStatusInfoObject(
          namePipelineList.get(i), startTime, endTime, deploymentStatus.get(i), serviceTagMap.get(planExecutionId)));
    }
    return statusInfo;
  }
  @Override
  public DashboardDeploymentActiveFailedRunningInfo getDeploymentActiveFailedRunningInfo(
      String accountId, String orgId, String projectId, long days) {
    // failed
    String queryFailed = queryBuilderStatus(accountId, orgId, projectId, days, failedStatusList);
    String queryServiceNameTagIdFailed =
        queryBuilderSelectIdLimitTimeCdTable(accountId, orgId, projectId, days, failedStatusList);
    List<DeploymentStatusInfo> failure = getDeploymentStatusInfo(queryFailed, queryServiceNameTagIdFailed);

    // active
    String queryActive = queryBuilderStatus(accountId, orgId, projectId, days, activeStatusList);
    String queryServiceNameTagIdActive =
        queryBuilderSelectIdLimitTimeCdTable(accountId, orgId, projectId, days, activeStatusList);
    List<DeploymentStatusInfo> active = getDeploymentStatusInfo(queryActive, queryServiceNameTagIdActive);

    // pending
    String queryPending = queryBuilderStatus(accountId, orgId, projectId, days, pendingStatusList);
    String queryServiceNameTagIdPending =
        queryBuilderSelectIdLimitTimeCdTable(accountId, orgId, projectId, days, pendingStatusList);
    List<DeploymentStatusInfo> pending = getDeploymentStatusInfo(queryPending, queryServiceNameTagIdPending);

    return DashboardDeploymentActiveFailedRunningInfo.builder()
        .failure(failure)
        .active(active)
        .pending(pending)
        .build();
  }

  private DeploymentStatusInfo getDeploymentStatusInfoObject(
      String name, Long startTime, Long endTime, String status, List<ServiceDeploymentInfo> serviceDeploymentInfos) {
    return DeploymentStatusInfo.builder()
        .name(name)
        .startTs(startTime)
        .endTs(endTime)
        .status(status)
        .serviceInfoList(serviceDeploymentInfos)
        .build();
  }

  private ServiceDeploymentInfo getServiceDeployment(String service_name, String tag) {
    if (service_name != null) {
      if (tag != null) {
        return ServiceDeploymentInfo.builder().serviceName(service_name).serviceTag(tag).build();
      } else {
        return ServiceDeploymentInfo.builder().serviceName(service_name).build();
      }
    }
    return ServiceDeploymentInfo.builder().build();
  }

  private WorkloadDeploymentInfo getWorkloadDeploymentInfo(WorkloadDeploymentInfo workloadDeploymentInfo,
      long totalDeployment, long prevTotalDeployment, long success, long previousSuccess, long failure,
      long previousFailure, long numberOfDays) {
    double percentSuccess = 0.0;
    double failureRate = 0.0;
    double failureRateChangeRate = calculateChangeRate(previousFailure, failure);
    double totalDeploymentChangeRate = calculateChangeRate(prevTotalDeployment, totalDeployment);
    double frequency = totalDeployment / numberOfDays;
    double prevFrequency = prevTotalDeployment / numberOfDays;
    double frequencyChangeRate = calculateChangeRate(prevFrequency, frequency);
    if (totalDeployment != 0) {
      percentSuccess = success / (double) totalDeployment;
      percentSuccess = percentSuccess * 100.0;
      failureRate = failure / (double) totalDeployment;
      failureRate = failureRate * 100.0;
    }
    return WorkloadDeploymentInfo.builder()
        .serviceName(workloadDeploymentInfo.getServiceName())
        .serviceId(workloadDeploymentInfo.getServiceId())
        .lastExecuted(workloadDeploymentInfo.getLastExecuted())
        .deploymentTypeList(workloadDeploymentInfo.getDeploymentTypeList())
        .totalDeployments(totalDeployment)
        .totalDeploymentChangeRate(totalDeploymentChangeRate)
        .percentSuccess(percentSuccess)
        .rateSuccess(getRate(success, previousSuccess))
        .failureRate(failureRate)
        .failureRateChangeRate(failureRateChangeRate)
        .frequency(frequency)
        .frequencyChangeRate(frequencyChangeRate)
        .lastPipelineExecutionId(workloadDeploymentInfo.getLastPipelineExecutionId())
        .workload(workloadDeploymentInfo.getWorkload())
        .build();
  }

  public DashboardWorkloadDeployment getWorkloadDeploymentInfoCalculation(List<String> workloadsId, List<String> status,
      List<Pair<Long, Long>> timeInterval, List<String> deploymentTypeList,
      HashMap<String, String> uniqueWorkloadNameAndId, long startDate, long endDate,
      List<String> pipelineExecutionIdList) {
    List<WorkloadDeploymentInfo> workloadDeploymentInfoList = new ArrayList<>();
    long numberOfDays = NGDateUtils.getNumberOfDays(startDate, endDate);
    for (String workloadId : uniqueWorkloadNameAndId.keySet()) {
      long totalDeployment = 0;
      long prevTotalDeployments = 0;
      long success = 0;
      long previousSuccess = 0;
      long failure = 0;
      long previousFailure = 0;
      long lastExecutedStartTs = 0L;
      long lastExecutedEndTs = 0L;
      String lastStatus = null;
      String deploymentType = null;
      String pipelineExecutionId = null;

      HashMap<Long, Integer> deploymentCountMap = new HashMap<>();

      long startDateCopy = startDate;
      long endDateCopy = endDate;

      while (startDateCopy < endDateCopy) {
        deploymentCountMap.put(startDateCopy, 0);
        startDateCopy = startDateCopy + DAY_IN_MS;
      }

      for (int i = 0; i < workloadsId.size(); i++) {
        if (workloadsId.get(i).contentEquals(workloadId)) {
          long startTime = timeInterval.get(i).getKey();
          long endTime = timeInterval.get(i).getValue();
          long currentTimeEpoch = startTime;
          if (currentTimeEpoch >= startDate && currentTimeEpoch < endDate) {
            currentTimeEpoch = getStartingDateEpochValue(currentTimeEpoch, startDate);
            totalDeployment++;
            deploymentCountMap.put(currentTimeEpoch, deploymentCountMap.get(currentTimeEpoch) + 1);
            if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
              success++;
            }
            if (failedStatusList.contains(status.get(i))) {
              failure++;
            }
            if (lastExecutedStartTs == 0) {
              lastExecutedStartTs = startTime;
              lastExecutedEndTs = endTime;
              lastStatus = status.get(i);
              deploymentType = deploymentTypeList.get(i);
              pipelineExecutionId = pipelineExecutionIdList.get(i);
            } else {
              if (lastExecutedStartTs < startTime) {
                lastExecutedStartTs = startTime;
                lastExecutedEndTs = endTime;
                lastStatus = status.get(i);
                deploymentType = deploymentTypeList.get(i);
                pipelineExecutionId = pipelineExecutionIdList.get(i);
              }
            }
          } else {
            if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
              previousSuccess++;
            }
            if (status.get(i).contentEquals(ExecutionStatus.FAILED.name())) {
              previousFailure++;
            }
          }
        }
      }

      if (totalDeployment > 0) {
        List<WorkloadDateCountInfo> dateCount = new ArrayList<>();
        startDateCopy = startDate;
        endDateCopy = endDate;
        while (startDateCopy < endDateCopy) {
          dateCount.add(WorkloadDateCountInfo.builder()
                            .date(startDateCopy)
                            .execution(WorkloadCountInfo.builder().count(deploymentCountMap.get(startDateCopy)).build())
                            .build());
          startDateCopy = startDateCopy + DAY_IN_MS;
        }
        LastWorkloadInfo lastWorkloadInfo = LastWorkloadInfo.builder()
                                                .startTime(lastExecutedStartTs)
                                                .endTime(lastExecutedEndTs)
                                                .status(lastStatus)
                                                .deploymentType(deploymentType)
                                                .build();
        WorkloadDeploymentInfo workloadDeploymentInfo = WorkloadDeploymentInfo.builder()
                                                            .serviceName(uniqueWorkloadNameAndId.get(workloadId))
                                                            .serviceId(workloadId)
                                                            .totalDeployments(totalDeployment)
                                                            .lastExecuted(lastWorkloadInfo)
                                                            .lastPipelineExecutionId(pipelineExecutionId)
                                                            .deploymentTypeList(deploymentTypeList)
                                                            .workload(dateCount)
                                                            .build();
        workloadDeploymentInfoList.add(getWorkloadDeploymentInfo(workloadDeploymentInfo, totalDeployment,
            prevTotalDeployments, success, previousSuccess, failure, previousFailure, numberOfDays));
      }
    }
    return DashboardWorkloadDeployment.builder().workloadDeploymentInfoList(workloadDeploymentInfoList).build();
  }

  @Override
  public DashboardWorkloadDeployment getDashboardWorkloadDeployment(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, long startInterval, long endInterval, long previousStartInterval,
      EnvironmentType envType) {
    endInterval = endInterval + DAY_IN_MS;
    String query = queryBuilderSelectWorkload(
        accountIdentifier, orgIdentifier, projectIdentifier, previousStartInterval, endInterval, envType);

    List<String> workloadsId = new ArrayList<>();
    List<String> status = new ArrayList<>();
    List<Pair<Long, Long>> timeInterval = new ArrayList<>();
    List<String> deploymentTypeList = new ArrayList<>();
    List<String> pipelineExecutionIdList = new ArrayList<>();

    HashMap<String, String> uniqueWorkloadNameAndId = new HashMap<>();

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          String serviceName = resultSet.getString("service_name");
          String service_id = resultSet.getString("service_id");
          long startTime = Long.parseLong(resultSet.getString("startTs"));
          workloadsId.add(service_id);
          status.add(resultSet.getString("status"));
          String pipelineExecutionId = resultSet.getString(NGServiceConstants.PIPELINE_EXECUTION_ID);
          pipelineExecutionIdList.add(pipelineExecutionId);
          if (resultSet.getString("endTs") != null) {
            timeInterval.add(Pair.of(startTime, Long.valueOf(resultSet.getString("endTs"))));
          } else {
            timeInterval.add(Pair.of(startInterval, -1L));
          }
          deploymentTypeList.add(resultSet.getString("deployment_type"));

          if (!uniqueWorkloadNameAndId.containsKey(service_id)) {
            uniqueWorkloadNameAndId.put(service_id, serviceName);
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return getWorkloadDeploymentInfoCalculation(workloadsId, status, timeInterval, deploymentTypeList,
        uniqueWorkloadNameAndId, startInterval, endInterval, pipelineExecutionIdList);
  }

  public long getTimeUnitToGroupBy(TimeGroupType timeGroupType) {
    if (timeGroupType == DAY) {
      return DAY_IN_MS;
    } else if (timeGroupType == HOUR) {
      return HOUR_IN_MS;
    } else {
      throw new UnknownEnumTypeException("Time Group Type", String.valueOf(timeGroupType));
    }
  }

  public long getStartingDateEpochValue(long epochValue, long startInterval) {
    return epochValue - epochValue % DAY_IN_MS;
  }

  /*
    Returns break down of instance count for various environment type for given account+org+project+service
  */
  @Override
  public InstanceCountDetailsByEnvTypeBase getActiveServiceInstanceCountBreakdown(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId) {
    return instanceDashboardService.getActiveServiceInstanceCountBreakdown(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, getCurrentTime());
  }

  /*
    Returns a list of buildId and instance counts for various environments for given account+org+project+service
  */
  @Override
  public EnvBuildIdAndInstanceCountInfoList getEnvBuildInstanceCountByServiceId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId) {
    Map<String, List<BuildIdAndInstanceCount>> envIdToBuildMap = new HashMap<>();
    Map<String, String> envIdToEnvNameMap = new HashMap<>();

    List<EnvBuildInstanceCount> envBuildInstanceCounts = instanceDashboardService.getEnvBuildInstanceCountByServiceId(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, getCurrentTime());

    envBuildInstanceCounts.forEach(envBuildInstanceCount -> {
      final String envId = envBuildInstanceCount.getEnvId();
      final String envName = envBuildInstanceCount.getEnvName();
      final String buildId = envBuildInstanceCount.getTag();
      final int count = envBuildInstanceCount.getCount();
      envIdToBuildMap.putIfAbsent(envId, new ArrayList<>());

      BuildIdAndInstanceCount buildIdAndInstanceCount =
          BuildIdAndInstanceCount.builder().buildId(buildId).count(count).build();
      envIdToBuildMap.get(envId).add(buildIdAndInstanceCount);

      envIdToEnvNameMap.putIfAbsent(envId, envName);
    });

    List<EnvBuildIdAndInstanceCountInfo> envBuildIdAndInstanceCountInfoList = new ArrayList<>();
    envIdToBuildMap.forEach((envId, buildIdAndInstanceCountList) -> {
      EnvBuildIdAndInstanceCountInfo envBuildIdAndInstanceCountInfo =
          EnvBuildIdAndInstanceCountInfo.builder()
              .envId(envId)
              .envName(envIdToEnvNameMap.getOrDefault(envId, ""))
              .buildIdAndInstanceCountList(buildIdAndInstanceCountList)
              .build();
      envBuildIdAndInstanceCountInfoList.add(envBuildIdAndInstanceCountInfo);
    });

    return EnvBuildIdAndInstanceCountInfoList.builder()
        .envBuildIdAndInstanceCountInfoList(envBuildIdAndInstanceCountInfoList)
        .build();
  }

  /*
    Returns list of instances for each build id for given account+org+project+service+env
  */
  @Override
  public InstancesByBuildIdList getActiveInstancesByServiceIdEnvIdAndBuildIds(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String serviceId, String envId, List<String> buildIds) {
    List<InstancesByBuildId> instancesByBuildIdList =
        instanceDashboardService.getActiveInstancesByServiceIdEnvIdAndBuildIds(
            accountIdentifier, orgIdentifier, projectIdentifier, serviceId, envId, buildIds, getCurrentTime());
    return InstancesByBuildIdList.builder().instancesByBuildIdList(instancesByBuildIdList).build();
  }
}
