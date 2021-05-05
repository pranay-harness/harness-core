package io.harness.pms.pipeline.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.Dashboard.DashboardPipelineExecutionInfo;
import io.harness.pms.Dashboard.DashboardPipelineHealthInfo;
import io.harness.pms.Dashboard.MeanMedianInfo;
import io.harness.pms.Dashboard.PipelineCountInfo;
import io.harness.pms.Dashboard.PipelineExecutionInfo;
import io.harness.pms.Dashboard.PipelineHealthInfo;
import io.harness.pms.Dashboard.StatusAndTime;
import io.harness.pms.Dashboard.SuccessHealthInfo;
import io.harness.pms.Dashboard.TotalHealthInfo;
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
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.CDC)
@Singleton
@Slf4j
public class PipelineDashboardServiceImpl implements PipelineDashboardService {
  @Inject TimeScaleDBService timeScaleDBService;

  private String tableName_default = "pipeline_execution_summary_ci";
  private String CI_TableName = "pipeline_execution_summary_ci";
  private String CD_TableName = "pipeline_execution_summary_cd";
  private List<String> failedList =
      Arrays.asList(ExecutionStatus.FAILED.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.EXPIRED.name());

  private static final int MAX_RETRY_COUNT = 5;

  public String queryBuilderSelectStatusAndTime(String accountId, String orgId, String projectId, String pipelineId,
      String startInterval, String endInterval, String tableName) {
    String selectStatusQuery = "select status,startts from " + tableName + " where ";
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

    if (pipelineId != null) {
      totalBuildSqlBuilder.append(String.format("pipelineidentifier='%s' and ", pipelineId));
    }

    if (startInterval != null && endInterval != null) {
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

  public double successPercent(long success, long total) {
    double rate = 0.0;
    if (total != 0) {
      rate = success / (double) total;
    }
    rate = rate * 100.0;
    return rate;
  }

  public String queryBuilderMean(String accountId, String orgId, String projectId, String pipelineId,
      String startInterval, String endInterval, String tableName) {
    String selectMeanQuery =
        "select avg(((DATE_PART('day', endts::timestamp - startts::timestamp) * 24 +                                                                                                                                                                                      DATE_PART('hour', endts::timestamp - startts::timestamp)) * 60 +\n"
        + "                DATE_PART('minute', endts::timestamp - startts::timestamp)) * 60 +\n"
        + "                DATE_PART('second', endts::timestamp - startts::timestamp)) from " + tableName + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectMeanQuery);

    if (accountId != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountId));
    }

    if (orgId != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgId));
    }

    if (projectId != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s' and ", projectId));
    }

    if (pipelineId != null) {
      totalBuildSqlBuilder.append(String.format("pipelineidentifier='%s' and ", pipelineId));
    }

    if (startInterval != null && endInterval != null) {
      totalBuildSqlBuilder.append(
          String.format("startts between '%s' and '%s' and endts is not null;", startInterval, endInterval));
    }

    return totalBuildSqlBuilder.toString();
  }

  public String queryBuilderMedian(String accountId, String orgId, String projectId, String pipelineId,
      String startInterval, String endInterval, String tableName) {
    String selectMedianQuery =
        "select PERCENTILE_DISC(0.5) within group (order by ((DATE_PART('day', endts::timestamp - startts::timestamp) * 24 + \n"
        + "                DATE_PART('hour', endts::timestamp - startts::timestamp)) * 60 +\n"
        + "                DATE_PART('minute', endts::timestamp - startts::timestamp)) * 60 +\n"
        + "                DATE_PART('second', endts::timestamp - startts::timestamp)) from " + tableName + " where ";
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(selectMedianQuery);

    if (accountId != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountId));
    }

    if (orgId != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgId));
    }

    if (projectId != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s' and ", projectId));
    }

    if (pipelineId != null) {
      totalBuildSqlBuilder.append(String.format("pipelineidentifier='%s' and ", pipelineId));
    }

    if (startInterval != null && endInterval != null) {
      totalBuildSqlBuilder.append(
          String.format("startts between '%s' and '%s' and endts is not null;", startInterval, endInterval));
    }

    return totalBuildSqlBuilder.toString();
  }

  public StatusAndTime queryCalculatorForStatusAndTime(String query) {
    long totalTries = 0;

    List<String> status = new ArrayList<>();
    List<String> time = new ArrayList<>();
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
    return StatusAndTime.builder().status(status).time(time).build();
  }

  public long queryCalculatorMean(String query) {
    long totalTries = 0;

    long mean = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          if (resultSet.getString("avg") != null) {
            mean = (long) Double.parseDouble(resultSet.getString("avg"));
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return mean;
  }

  public long queryCalculatorMedian(String query) {
    long totalTries = 0;

    long mdedian = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          if (resultSet.getString("percentile_disc") != null) {
            mdedian = (long) Double.parseDouble(resultSet.getString("percentile_disc"));
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return mdedian;
  }

  public String selectTableFromModuleInfo(String moduleInfo) {
    String tableName = tableName_default;
    if (moduleInfo.equalsIgnoreCase("CI")) {
      tableName = CI_TableName;
    } else if (moduleInfo.equalsIgnoreCase("CD")) {
      tableName = CD_TableName;
    }
    return tableName;
  }

  @Override
  public DashboardPipelineHealthInfo getDashboardPipelineHealthInfo(String accountId, String orgId, String projectId,
      String pipelineId, String startInterval, String endInterval, String previousStartInterval, String moduleInfo) {
    LocalDate startDate = LocalDate.parse(startInterval);
    LocalDate endDate = LocalDate.parse(endInterval);
    LocalDate previousStartDate = LocalDate.parse(previousStartInterval);

    String tableName = selectTableFromModuleInfo(moduleInfo);

    String query = queryBuilderSelectStatusAndTime(
        accountId, orgId, projectId, pipelineId, previousStartInterval, endDate.plusDays(1).toString(), tableName);

    StatusAndTime statusAndTime = queryCalculatorForStatusAndTime(query);
    List<String> status = statusAndTime.getStatus();
    List<String> time = statusAndTime.getTime();

    long currentTotal = 0, currentSuccess = 0;
    long previousTotal = 0, previousSuccess = 0;
    for (int i = 0; i < time.size(); i++) {
      // make time.get(i) in yyyy-mm-dd format
      LocalDate variableDate = LocalDate.parse(time.get(i).substring(0, time.get(i).indexOf(' ')));
      if (startDate.compareTo(variableDate) <= 0 && endDate.compareTo(variableDate) >= 0) {
        currentTotal++;
        if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
          currentSuccess++;
        }
      }

      // previous interval record
      if (previousStartDate.compareTo(variableDate) <= 0 && startDate.compareTo(variableDate) > 0) {
        previousTotal++;
        if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
          previousSuccess++;
        }
      }
    }

    // mean calculation
    String queryMeanCurrent = queryBuilderMean(
        accountId, orgId, projectId, pipelineId, startInterval, endDate.plusDays(1).toString(), tableName);
    long currentMean = queryCalculatorMean(queryMeanCurrent);

    String queryMeanPrevious =
        queryBuilderMean(accountId, orgId, projectId, pipelineId, previousStartInterval, startInterval, tableName);
    long previousMean = queryCalculatorMean(queryMeanPrevious);

    // Median calculation
    String queryMedianCurrent = queryBuilderMedian(
        accountId, orgId, projectId, pipelineId, startInterval, endDate.plusDays(1).toString(), tableName);
    long currentMedian = queryCalculatorMedian(queryMedianCurrent);

    String queryMedianPrevious =
        queryBuilderMedian(accountId, orgId, projectId, pipelineId, previousStartInterval, startInterval, tableName);
    long previousMedian = queryCalculatorMedian(queryMedianPrevious);

    return DashboardPipelineHealthInfo.builder()
        .executions(
            PipelineHealthInfo.builder()
                .total(TotalHealthInfo.builder().count(currentTotal).rate(getRate(currentTotal, previousTotal)).build())
                .success(SuccessHealthInfo.builder()
                             .rate(getRate(currentSuccess, previousSuccess))
                             .percent(successPercent(currentSuccess, currentTotal))
                             .build())
                .meanInfo(MeanMedianInfo.builder()
                              .duration(String.valueOf(currentMean))
                              .rate(String.valueOf(currentMean - previousMean))
                              .build())
                .medianInfo(MeanMedianInfo.builder()
                                .duration(String.valueOf(currentMedian))
                                .rate(String.valueOf(currentMedian - previousMedian))
                                .build())
                .build())
        .build();
  }

  @Override
  public DashboardPipelineExecutionInfo getDashboardPipelineExecutionInfo(String accountId, String orgId,
      String projectId, String pipelineId, String startInterval, String endInterval, String moduleInfo) {
    LocalDate startDate = LocalDate.parse(startInterval);
    LocalDate endDate = LocalDate.parse(endInterval);

    String tableName = selectTableFromModuleInfo(moduleInfo);

    String query = queryBuilderSelectStatusAndTime(
        accountId, orgId, projectId, pipelineId, startInterval, endDate.plusDays(1).toString(), tableName);
    StatusAndTime statusAndTime = queryCalculatorForStatusAndTime(query);
    List<String> status = statusAndTime.getStatus();
    List<String> time = statusAndTime.getTime();

    List<PipelineExecutionInfo> pipelineExecutionInfoList = new ArrayList<>();

    while (!startDate.isAfter(endDate)) {
      long total = 0, success = 0, failed = 0;
      for (int i = 0; i < time.size(); i++) {
        if (time.get(i).contains(startDate.toString())) {
          total++;
          if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
            success++;
          } else if (failedList.contains(status.get(i))) {
            failed++;
          }
        }
      }
      pipelineExecutionInfoList.add(
          PipelineExecutionInfo.builder()
              .date(startDate.toString())
              .count(PipelineCountInfo.builder().total(total).success(success).failure(failed).build())
              .build());
      startDate = startDate.plusDays(1);
    }

    return DashboardPipelineExecutionInfo.builder().pipelineExecutionInfoList(pipelineExecutionInfoList).build();
  }
}
