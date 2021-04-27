package io.harness.cdng.service.dashboard;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.Deployment.Deployment;
import io.harness.cdng.Deployment.DeploymentDateAndCount;
import io.harness.cdng.Deployment.DeploymentInfo;
import io.harness.cdng.Deployment.HealthDeploymentDashboard;
import io.harness.cdng.Deployment.HealthDeploymentInfo;
import io.harness.cdng.Deployment.TotalDeploymentInfo;
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
import java.util.ArrayList;
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
    totalTries = 0;
    successfulOperation = false;
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
}
