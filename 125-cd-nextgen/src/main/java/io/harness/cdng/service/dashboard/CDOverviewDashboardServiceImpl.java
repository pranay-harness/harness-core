package io.harness.cdng.service.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.Deployment.DashboardDeploymentActiveFailedRunningInfo;
import io.harness.cdng.Deployment.DashboardWorkloadDeployment;
import io.harness.cdng.Deployment.Deployment;
import io.harness.cdng.Deployment.DeploymentCount;
import io.harness.cdng.Deployment.DeploymentDateAndCount;
import io.harness.cdng.Deployment.DeploymentInfo;
import io.harness.cdng.Deployment.DeploymentStatusInfo;
import io.harness.cdng.Deployment.ExecutionDeployment;
import io.harness.cdng.Deployment.ExecutionDeploymentInfo;
import io.harness.cdng.Deployment.HealthDeploymentDashboard;
import io.harness.cdng.Deployment.HealthDeploymentInfo;
import io.harness.cdng.Deployment.ServiceDeploymentInfo;
import io.harness.cdng.Deployment.TimeAndStatusDeployment;
import io.harness.cdng.Deployment.TotalDeploymentInfo;
import io.harness.cdng.Deployment.WorkloadCountInfo;
import io.harness.cdng.Deployment.WorkloadDateCountInfo;
import io.harness.cdng.Deployment.WorkloadDeploymentInfo;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Singleton
@Slf4j
public class CDOverviewDashboardServiceImpl implements CDOverviewDashboardService {
  @Inject TimeScaleDBService timeScaleDBService;

  private String tableNameCD = "pipeline_execution_summary_cd";
  private String tableNameServiceAndInfra = "service_infra_info";
  private List<String> failedStatusList =
      Arrays.asList(ExecutionStatus.FAILED.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.EXPIRED.name());
  private List<String> activeStatusList = Arrays.asList(ExecutionStatus.RUNNING.name(), ExecutionStatus.PAUSED.name());
  private List<String> pendingStatusList = Arrays.asList(ExecutionStatus.INTERVENTION_WAITING.name(),
      ExecutionStatus.APPROVAL_WAITING.name(), ExecutionStatus.WAITING.name());
  private static final int MAX_RETRY_COUNT = 5;

  public String queryBuilderSelectStatusTime(
      String accountId, String orgId, String projectId, String startInterval, String endInterval) {
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

    if (startInterval != null && endInterval != null) {
      totalBuildSqlBuilder.append(String.format("startts between '%s' and '%s';", startInterval, endInterval));
    }

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderEnvironmentType(
      String accountId, String orgId, String projectId, String startInterval, String endInterval) {
    String selectStatusQuery = "select env_type from " + tableNameCD + ", " + tableNameServiceAndInfra + " where ";
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

    if (startInterval != null && endInterval != null) {
      totalBuildSqlBuilder.append(
          "pipeline_execution_summary_cd.id=pipeline_execution_summary_cd_id and env_type is not null and ");
      totalBuildSqlBuilder.append(String.format("startts between '%s' and '%s';", startInterval, endInterval));
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

    totalBuildSqlBuilder.append(String.format(") ORDER BY startts DESC LIMIT %s;", days));

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderServiceTag(
      String accountId, String orgId, String projectId, List<String> planExecutionIdList, List<String> statusList) {
    String selectStatusQuery = "select service_name,tag,pipeline_execution_summary_cd_id from "
        + tableNameServiceAndInfra + ", " + tableNameCD + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder(20480);
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

    totalBuildSqlBuilder.append(") and pipeline_execution_summary_cd_id in (");
    for (String cd_id : planExecutionIdList) {
      totalBuildSqlBuilder.append(String.format("'%s',", cd_id));
    }

    totalBuildSqlBuilder.deleteCharAt(totalBuildSqlBuilder.length() - 1);

    totalBuildSqlBuilder.append(
        ") and pipeline_execution_summary_cd.id=pipeline_execution_summary_cd_id and service_name is not null;");

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderSelectWorkload(
      String accountId, String orgId, String projectId, String previousStartInterval, String endInterval) {
    String selectStatusQuery = "select service_name,status,startts,endts,pipeline_execution_summary_cd_id from "
        + tableNameServiceAndInfra + ", " + tableNameCD + " where ";
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

    if (previousStartInterval != null && endInterval != null) {
      totalBuildSqlBuilder.append(
          "pipeline_execution_summary_cd.id=pipeline_execution_summary_cd_id and service_name is not null and ");
      totalBuildSqlBuilder.append(String.format("startts between '%s' and '%s';", previousStartInterval, endInterval));
    }

    return totalBuildSqlBuilder.toString();
  }

  public TimeAndStatusDeployment queryCalculatorTimeAndStatus(String query) {
    List<String> time = new ArrayList<>();
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
          time.add(resultSet.getString("startts"));
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

  @Override
  public HealthDeploymentDashboard getHealthDeploymentDashboard(String accountId, String orgId, String projectId,
      String startInterval, String endInterval, String previousStartInterval) {
    LocalDate startDate = LocalDate.parse(startInterval);
    LocalDate endDate = LocalDate.parse(endInterval);
    LocalDate previousStartDate = LocalDate.parse(previousStartInterval);
    String query = queryBuilderSelectStatusTime(
        accountId, orgId, projectId, previousStartInterval, endDate.plusDays(1).toString());

    List<String> time = new ArrayList<>();
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

    HashMap<String, Integer> totalCountMap = new HashMap<>();
    HashMap<String, Integer> successCountMap = new HashMap<>();
    HashMap<String, Integer> failedCountMap = new HashMap<>();

    LocalDate startDateCopy = startDate;
    LocalDate endDateCopy = endDate;

    while (!startDateCopy.isAfter(endDateCopy)) {
      totalCountMap.put(startDateCopy.toString(), 0);
      successCountMap.put(startDateCopy.toString(), 0);
      failedCountMap.put(startDateCopy.toString(), 0);
      startDateCopy = startDateCopy.plusDays(1);
    }

    for (int i = 0; i < time.size(); i++) {
      // make time.get(i) in yyyy-mm-dd format
      LocalDate variableDate = LocalDate.parse(time.get(i).substring(0, time.get(i).indexOf(' ')));
      if (startDate.compareTo(variableDate) <= 0 && endDate.compareTo(variableDate) >= 0) {
        total++;
        totalCountMap.put(variableDate.toString(), totalCountMap.get(variableDate.toString()) + 1);
        if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
          currentSuccess++;
          successCountMap.put(variableDate.toString(), successCountMap.get(variableDate.toString()) + 1);
        } else if (status.get(i).contentEquals(ExecutionStatus.FAILED.name())) {
          currentFailed++;
          failedCountMap.put(variableDate.toString(), failedCountMap.get(variableDate.toString()) + 1);
        }
      } else {
        if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
          previousSuccess++;
        } else if (status.get(i).contentEquals(ExecutionStatus.FAILED.name())) {
          previousFailed++;
        }
      }
    }

    String queryEnvironmentType =
        queryBuilderEnvironmentType(accountId, orgId, projectId, startInterval, endDate.plusDays(1).toString());
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

    long production = Collections.frequency(envType, EnvironmentType.Production.name());
    long nonProduction = Collections.frequency(envType, EnvironmentType.PreProduction.name());

    List<DeploymentDateAndCount> totalDateAndCount = new ArrayList<>();
    List<DeploymentDateAndCount> successDateAndCount = new ArrayList<>();
    List<DeploymentDateAndCount> failedDateAndCount = new ArrayList<>();
    startDateCopy = startDate;
    endDateCopy = endDate;

    while (!startDateCopy.isAfter(endDateCopy)) {
      totalDateAndCount.add(
          DeploymentDateAndCount.builder()
              .time(startDateCopy.toString())
              .deployments(Deployment.builder().count(totalCountMap.get(startDateCopy.toString())).build())
              .build());
      successDateAndCount.add(
          DeploymentDateAndCount.builder()
              .time(startDateCopy.toString())
              .deployments(Deployment.builder().count(successCountMap.get(startDateCopy.toString())).build())
              .build());
      failedDateAndCount.add(
          DeploymentDateAndCount.builder()
              .time(startDateCopy.toString())
              .deployments(Deployment.builder().count(failedCountMap.get(startDateCopy.toString())).build())
              .build());
      startDateCopy = startDateCopy.plusDays(1);
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

  private ExecutionDeployment getExecutionDeployment(String time, long total, long success, long failed) {
    return ExecutionDeployment.builder()
        .time(time)
        .deployments(DeploymentCount.builder().total(total).success(success).failure(failed).build())
        .build();
  }

  @Override
  public ExecutionDeploymentInfo getExecutionDeploymentDashboard(
      String accountId, String orgId, String projectId, String startInterval, String endInterval) {
    LocalDate startDate = LocalDate.parse(startInterval);
    LocalDate endDate = LocalDate.parse(endInterval);
    String query =
        queryBuilderSelectStatusTime(accountId, orgId, projectId, startInterval, endDate.plusDays(1).toString());

    HashMap<String, Integer> totalCountMap = new HashMap<>();
    HashMap<String, Integer> successCountMap = new HashMap<>();
    HashMap<String, Integer> failedCountMap = new HashMap<>();

    LocalDate startDateCopy = startDate;
    LocalDate endDateCopy = endDate;

    while (!startDateCopy.isAfter(endDateCopy)) {
      totalCountMap.put(startDateCopy.toString(), 0);
      successCountMap.put(startDateCopy.toString(), 0);
      failedCountMap.put(startDateCopy.toString(), 0);
      startDateCopy = startDateCopy.plusDays(1);
    }

    TimeAndStatusDeployment timeAndStatusDeployment = queryCalculatorTimeAndStatus(query);
    List<String> time = timeAndStatusDeployment.getTime();
    List<String> status = timeAndStatusDeployment.getStatus();

    List<ExecutionDeployment> executionDeployments = new ArrayList<>();

    for (int i = 0; i < time.size(); i++) {
      String variableDate = time.get(i).substring(0, time.get(i).indexOf(' '));
      totalCountMap.put(variableDate, totalCountMap.get(variableDate) + 1);
      if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
        successCountMap.put(variableDate, successCountMap.get(variableDate) + 1);
      } else if (status.get(i).contentEquals(ExecutionStatus.FAILED.name())) {
        failedCountMap.put(variableDate, failedCountMap.get(variableDate) + 1);
      }
    }

    startDateCopy = startDate;
    endDateCopy = endDate;

    while (!startDateCopy.isAfter(endDateCopy)) {
      executionDeployments.add(
          getExecutionDeployment(startDateCopy.toString(), totalCountMap.get(startDateCopy.toString()),
              successCountMap.get(startDateCopy.toString()), failedCountMap.get(startDateCopy.toString())));
      startDateCopy = startDateCopy.plusDays(1);
    }
    return ExecutionDeploymentInfo.builder().executionDeploymentList(executionDeployments).build();
  }

  public List<DeploymentStatusInfo> getDeploymentStatusInfo(
      String accountId, String orgId, String projectId, String queryStatus, List<String> statusList) {
    List<String> planExecutionIdList = new ArrayList<>();
    List<String> namePipelineList = new ArrayList<>();
    List<String> startTs = new ArrayList<>();
    List<String> endTs = new ArrayList<>();
    List<String> deploymentStatus = new ArrayList<>();

    HashMap<String, List<ServiceDeploymentInfo>> serviceTagMap = new HashMap<>();

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
          startTs.add(resultSet.getString("startts"));
          deploymentStatus.add(resultSet.getString("status"));
          if (resultSet.getString("endTs") != null) {
            endTs.add(resultSet.getString("endTs"));
          } else {
            endTs.add(LocalDateTime.now().toString().replace('T', ' '));
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }

    String queryServiceTag = queryBuilderServiceTag(accountId, orgId, projectId, planExecutionIdList, statusList);

    totalTries = 0;
    successfulOperation = false;
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

    List<DeploymentStatusInfo> statusInfo = new ArrayList<>();
    for (int i = 0; i < planExecutionIdList.size(); i++) {
      String planExecutionId = planExecutionIdList.get(i);
      String startTime = startTs.get(i);
      String endTime = endTs.get(i);
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
    List<DeploymentStatusInfo> failure =
        getDeploymentStatusInfo(accountId, orgId, projectId, queryFailed, failedStatusList);

    // active
    String queryActive = queryBuilderStatus(accountId, orgId, projectId, days, activeStatusList);
    List<DeploymentStatusInfo> active =
        getDeploymentStatusInfo(accountId, orgId, projectId, queryActive, activeStatusList);

    // pending
    String queryPending = queryBuilderStatus(accountId, orgId, projectId, days, pendingStatusList);
    List<DeploymentStatusInfo> pending =
        getDeploymentStatusInfo(accountId, orgId, projectId, queryPending, pendingStatusList);

    return DashboardDeploymentActiveFailedRunningInfo.builder()
        .failure(failure)
        .active(active)
        .pending(pending)
        .build();
  }

  private DeploymentStatusInfo getDeploymentStatusInfoObject(String name, String startTime, String endTime,
      String status, List<ServiceDeploymentInfo> serviceDeploymentInfos) {
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

  private WorkloadDeploymentInfo getWorkloadDeploymentInfo(String workload, String lastExecuted, long totalDeployment,
      String lastStatus, long success, long previousSuccess, List<WorkloadDateCountInfo> dateCount) {
    double percentSuccess = 0.0;
    if (totalDeployment != 0) {
      percentSuccess = success / (double) totalDeployment;
      percentSuccess = percentSuccess * 100.0;
    }
    return WorkloadDeploymentInfo.builder()
        .serviceName(workload)
        .lastExecuted(lastExecuted)
        .totalDeployments(totalDeployment)
        .lastStatus(lastStatus)
        .percentSuccess(percentSuccess)
        .rateSuccess(getRate(success, previousSuccess))
        .workload(dateCount)
        .build();
  }

  @Override
  public DashboardWorkloadDeployment getDashboardWorkloadDeployment(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String startInterval, String endInterval, String previousStartInterval) {
    LocalDate startDate = LocalDate.parse(startInterval);
    LocalDate endDate = LocalDate.parse(endInterval);
    String query = queryBuilderSelectWorkload(
        accountIdentifier, orgIdentifier, projectIdentifier, previousStartInterval, endDate.plusDays(1).toString());

    List<String> workloads = new ArrayList<>();
    List<String> status = new ArrayList<>();
    List<String> startTs = new ArrayList<>();
    List<String> endTs = new ArrayList<>();
    List<String> planExecutionIdList = new ArrayList<>();

    HashMap<String, Integer> uniqueWorkloadName = new HashMap<>();

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          String serviceName = resultSet.getString("service_name");
          String startTime = resultSet.getString("startTs");
          workloads.add(serviceName);
          status.add(resultSet.getString("status"));
          startTs.add(startTime);
          endTs.add(resultSet.getString("endTs"));
          planExecutionIdList.add(resultSet.getString("pipeline_execution_summary_cd_id"));

          if (!uniqueWorkloadName.containsKey(serviceName)) {
            uniqueWorkloadName.put(serviceName, 1);
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    List<WorkloadDeploymentInfo> workloadDeploymentInfoList = new ArrayList<>();

    for (String workload : uniqueWorkloadName.keySet()) {
      long totalDeployment = 0;
      long success = 0;
      long previousSuccess = 0;
      String lastExecuted = null;
      String lastStatus = null;

      HashMap<String, Integer> deploymentCountMap = new HashMap<>();

      LocalDate startDateCopy = startDate;
      LocalDate endDateCopy = endDate;

      while (!startDateCopy.isAfter(endDateCopy)) {
        deploymentCountMap.put(startDateCopy.toString(), 0);
        startDateCopy = startDateCopy.plusDays(1);
      }

      for (int i = 0; i < workloads.size(); i++) {
        if (workloads.get(i).contentEquals(workload)) {
          LocalDate variableDate = LocalDate.parse(startTs.get(i).substring(0, startTs.get(i).indexOf(' ')));
          if (startDate.compareTo(variableDate) <= 0 && endDate.compareTo(variableDate) >= 0) {
            totalDeployment++;
            deploymentCountMap.put(variableDate.toString(), deploymentCountMap.get(variableDate.toString()) + 1);
            if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
              success++;
            }
            if (lastExecuted == null) {
              lastExecuted = startTs.get(i);
              lastStatus = status.get(i);
            } else {
              if (lastExecuted.compareTo(variableDate.toString()) <= 0) {
                lastExecuted = startTs.get(i);
                lastStatus = status.get(i);
              }
            }
          } else {
            if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
              previousSuccess++;
            }
          }
        }
      }

      if (totalDeployment > 0) {
        List<WorkloadDateCountInfo> dateCount = new ArrayList<>();
        startDateCopy = startDate;
        endDateCopy = endDate;
        while (!startDateCopy.isAfter(endDateCopy)) {
          dateCount.add(
              WorkloadDateCountInfo.builder()
                  .date(startDateCopy.toString())
                  .execution(
                      WorkloadCountInfo.builder().count(deploymentCountMap.get(startDateCopy.toString())).build())
                  .build());
          startDateCopy = startDateCopy.plusDays(1);
        }
        workloadDeploymentInfoList.add(getWorkloadDeploymentInfo(
            workload, lastExecuted, totalDeployment, lastStatus, success, previousSuccess, dateCount));
      }
    }
    return DashboardWorkloadDeployment.builder().workloadDeploymentInfoList(workloadDeploymentInfoList).build();
  }
}
