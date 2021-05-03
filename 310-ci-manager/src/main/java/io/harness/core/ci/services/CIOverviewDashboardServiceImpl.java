package io.harness.core.ci.services;

import io.harness.app.beans.entities.BuildActiveInfo;
import io.harness.app.beans.entities.BuildCount;
import io.harness.app.beans.entities.BuildExecutionInfo;
import io.harness.app.beans.entities.BuildFailureInfo;
import io.harness.app.beans.entities.BuildHealth;
import io.harness.app.beans.entities.BuildInfo;
import io.harness.app.beans.entities.BuildRepositoryCount;
import io.harness.app.beans.entities.DashboardBuildExecutionInfo;
import io.harness.app.beans.entities.DashboardBuildRepositoryInfo;
import io.harness.app.beans.entities.DashboardBuildsHealthInfo;
import io.harness.app.beans.entities.RepositoryBuildInfo;
import io.harness.app.beans.entities.RepositoryInfo;
import io.harness.app.beans.entities.StatusAndTime;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.execution.utils.StatusUtils;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CIOverviewDashboardServiceImpl implements CIOverviewDashboardService {
  @Inject TimeScaleDBService timeScaleDBService;

  private String tableName = "pipeline_execution_summary_ci";
  private String staticQuery = "select * from " + tableName + " where ";
  private List<String> failedList =
      Arrays.asList(ExecutionStatus.FAILED.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.EXPIRED.name());

  private static final int MAX_RETRY_COUNT = 5;

  private String queryBuilder(
      String accountId, String orgId, String projectId, String startInterval, String endInterval, String status) {
    StringBuilder totalBuildSqlBuilder = new StringBuilder();
    totalBuildSqlBuilder.append(staticQuery);
    if (accountId != null) {
      totalBuildSqlBuilder.append(String.format("accountid='%s' and ", accountId));
    }

    if (orgId != null) {
      totalBuildSqlBuilder.append(String.format("orgidentifier='%s' and ", orgId));
    }

    if (projectId != null) {
      totalBuildSqlBuilder.append(String.format("projectidentifier='%s' and ", projectId));
    }

    if (status != null) {
      totalBuildSqlBuilder.append("status='");
      totalBuildSqlBuilder.append(status);
      totalBuildSqlBuilder.append("' and ");
    }

    if (startInterval != null && endInterval != null) {
      totalBuildSqlBuilder.append(String.format("startts between '%s' and '%s';", startInterval, endInterval));
    }

    return totalBuildSqlBuilder.toString();
  }

  private String queryBuilderSelectStatus(
      String accountId, String orgId, String projectId, String startInterval, String endInterval) {
    String selectStatusQuery = "select status from " + tableName + " where ";
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

  private String queryBuilderSelectStatusAndTime(
      String accountId, String orgId, String projectId, String startInterval, String endInterval) {
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

    if (startInterval != null && endInterval != null) {
      totalBuildSqlBuilder.append(String.format("startts between '%s' and '%s';", startInterval, endInterval));
    }

    return totalBuildSqlBuilder.toString();
  }

  private String queryBuilderFailedStatusOrderBy(String accountId, String orgId, String projectId, long limit) {
    String selectStatusQuery =
        "select name, moduleinfo_branch_name, moduleinfo_branch_commit_message, moduleinfo_branch_commit_id, startts, endts  from "
        + tableName + " where ";

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
    for (String failed : failedList) {
      totalBuildSqlBuilder.append(String.format("'%s',", failed));
    }

    totalBuildSqlBuilder.deleteCharAt(totalBuildSqlBuilder.length() - 1);

    totalBuildSqlBuilder.append(String.format(") ORDER BY startts DESC LIMIT %s;", limit));

    return totalBuildSqlBuilder.toString();
  }

  private String queryBuilderActiveStatusOrderBy(String accountId, String orgId, String projectId, long limit) {
    String selectStatusQuery =
        "select name, moduleinfo_branch_name, moduleinfo_branch_commit_message, moduleinfo_branch_commit_id, startts, status  from "
        + tableName + " where ";

    StringBuilder totalBuildSqlBuilder = new StringBuilder(1024);
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

    totalBuildSqlBuilder.append("status IN (");
    EnumSet<Status> activeStatuses = StatusUtils.activeStatuses();
    for (Status activeStatus : activeStatuses) {
      totalBuildSqlBuilder.append(" '" + activeStatus.name() + "' ,");
    }

    totalBuildSqlBuilder.deleteCharAt(totalBuildSqlBuilder.length() - 1);

    totalBuildSqlBuilder.append(") ORDER BY startts DESC LIMIT " + limit + ";");

    return totalBuildSqlBuilder.toString();
  }

  private String queryBuilderSelectRepoInfo(
      String accountId, String orgId, String projectId, String previousStartInterval, String endInterval) {
    String selectStatusQuery = "select moduleinfo_repository, status, startts, moduleinfo_branch_commit_message  from "
        + tableName + " where ";

    StringBuilder totalBuildSqlBuilder = new StringBuilder(1024);
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

    totalBuildSqlBuilder.append(String.format("moduleinfo_repository IS NOT NULL and "));

    if (previousStartInterval != null && endInterval != null) {
      totalBuildSqlBuilder.append(String.format("startts between '%s' and '%s';", previousStartInterval, endInterval));
    }

    return totalBuildSqlBuilder.toString();
  }

  private long queryCalculator(String query) {
    long totalTries = 0;
    long resultCount = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          resultCount++;
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return resultCount;
  }

  private List<String> queryCalculatorForStatus(String query) {
    long totalTries = 0;

    List<String> statusList = new ArrayList<>();
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          statusList.add(resultSet.getString("status"));
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return statusList;
  }

  private StatusAndTime queryCalculatorForStatusAndTime(String query) {
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

  BuildFailureInfo getBuildFailureInfo(
      String name, String branch_name, String commit, String commit_id, String startTs, String endTs) {
    return BuildFailureInfo.builder()
        .piplineName(name)
        .branch(branch_name)
        .commit(commit)
        .commitID(commit_id)
        .startTs(startTs)
        .endTs(endTs)
        .build();
  }

  BuildActiveInfo getBuildActiveInfo(
      String name, String branch_name, String commit, String commit_id, String startTs, String status, String endTs) {
    return BuildActiveInfo.builder()
        .piplineName(name)
        .branch(branch_name)
        .commit(commit)
        .commitID(commit_id)
        .startTs(startTs)
        .status(status)
        .endTs(endTs)
        .build();
  }

  @Override
  public long totalBuilds(String accountId, String orgId, String projectId, String startInterval, String endInterval) {
    String query = queryBuilder(accountId, orgId, projectId, startInterval, endInterval, null);
    return queryCalculator(query);
  }

  @Override
  public long successfulBuilds(
      String accountId, String orgId, String projectId, String startInterval, String endInterval) {
    String query =
        queryBuilder(accountId, orgId, projectId, startInterval, endInterval, ExecutionStatus.SUCCESS.name());
    return queryCalculator(query);
  }

  @Override
  public long failedBuilds(String accountId, String orgId, String projectId, String startInterval, String endInterval) {
    String query = queryBuilder(accountId, orgId, projectId, startInterval, endInterval, ExecutionStatus.FAILED.name());
    return queryCalculator(query);
  }

  @Override
  public BuildHealth getCountAndRate(long currentCount, long previousCount) {
    double rate = 0.0;
    if (previousCount != 0) {
      rate = (currentCount - previousCount) / (double) previousCount;
    }
    rate = rate * 100;
    return BuildHealth.builder().count(currentCount).rate(rate).build();
  }

  @Override
  public DashboardBuildsHealthInfo getDashBoardBuildHealthInfoWithRate(String accountId, String orgId, String projectId,
      String startInterval, String endInterval, String previousStartInterval) {
    LocalDate startDate = LocalDate.parse(startInterval);
    LocalDate endDate = LocalDate.parse(endInterval);
    LocalDate previousStartDate = LocalDate.parse(previousStartInterval);
    String query = queryBuilderSelectStatusAndTime(
        accountId, orgId, projectId, previousStartInterval, endDate.plusDays(1).toString());
    StatusAndTime statusAndTime = queryCalculatorForStatusAndTime(query);
    List<String> status = statusAndTime.getStatus();
    List<String> time = statusAndTime.getTime();

    long currentTotal = 0, currentSuccess = 0, currentFailed = 0;
    long previousTotal = 0, previousSuccess = 0, previousFailed = 0;
    for (int i = 0; i < time.size(); i++) {
      // make time.get(i) in yyyy-mm-dd format
      LocalDate variableDate = LocalDate.parse(time.get(i).substring(0, time.get(i).indexOf(' ')));
      if (startDate.compareTo(variableDate) <= 0 && endDate.compareTo(variableDate) >= 0) {
        currentTotal++;
        if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
          currentSuccess++;
        } else if (failedList.contains(status.get(i))) {
          currentFailed++;
        }
      }

      // previous interval record
      if (previousStartDate.compareTo(variableDate) <= 0 && startDate.compareTo(variableDate) > 0) {
        previousTotal++;
        if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
          previousSuccess++;
        } else if (failedList.contains(status.get(i))) {
          previousFailed++;
        }
      }
    }

    return DashboardBuildsHealthInfo.builder()
        .builds(BuildInfo.builder()
                    .total(getCountAndRate(currentTotal, previousTotal))
                    .success(getCountAndRate(currentSuccess, previousSuccess))
                    .failed(getCountAndRate(currentFailed, previousFailed))
                    .build())
        .build();
  }

  @Override
  public DashboardBuildExecutionInfo getBuildExecutionBetweenIntervals(
      String accountId, String orgId, String projectId, String startInterval, String endInterval) {
    List<BuildExecutionInfo> buildExecutionInfoList = new ArrayList<>();
    LocalDate startDate = LocalDate.parse(startInterval);
    LocalDate endDate = LocalDate.parse(endInterval);

    String query =
        queryBuilderSelectStatusAndTime(accountId, orgId, projectId, startInterval, endDate.plusDays(1).toString());
    StatusAndTime statusAndTime = queryCalculatorForStatusAndTime(query);
    List<String> status = statusAndTime.getStatus();
    List<String> time = statusAndTime.getTime();

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
      BuildCount buildCount = BuildCount.builder().total(total).success(success).failed(failed).build();
      buildExecutionInfoList.add(BuildExecutionInfo.builder().time(startDate.toString()).builds(buildCount).build());
      startDate = startDate.plusDays(1);
    }

    return DashboardBuildExecutionInfo.builder().buildExecutionInfoList(buildExecutionInfoList).build();
  }

  private long getDuration(String endts, String startts) {
    // assuming endts and starts in yyyy-mm-dd HH:MM:SS.{number}
    // replacing space with T
    long duration = ChronoUnit.SECONDS.between(
        LocalDateTime.parse(endts.replace(' ', 'T')), LocalDateTime.parse(startts.replace(' ', 'T')));
    if (duration < 0) {
      duration = duration * (-1);
    }
    return duration;
  }

  @Override
  public List<BuildFailureInfo> getDashboardBuildFailureInfo(
      String accountId, String orgId, String projectId, long days) {
    String query = queryBuilderFailedStatusOrderBy(accountId, orgId, projectId, days);
    List<BuildFailureInfo> buildFailureInfos = new ArrayList<>();
    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          buildFailureInfos.add(getBuildFailureInfo(resultSet.getString("name"),
              resultSet.getString("moduleinfo_branch_name"), resultSet.getString("moduleinfo_branch_commit_message"),
              resultSet.getString("moduleinfo_branch_commit_id"), resultSet.getString("startts"),
              resultSet.getString("endts")));
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }

    return buildFailureInfos;
  }

  @Override
  public List<BuildActiveInfo> getDashboardBuildActiveInfo(
      String accountId, String orgId, String projectId, long days) {
    String query = queryBuilderActiveStatusOrderBy(accountId, orgId, projectId, days);
    List<BuildActiveInfo> buildActiveInfos = new ArrayList<>();
    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          buildActiveInfos.add(getBuildActiveInfo(resultSet.getString("name"),
              resultSet.getString("moduleinfo_branch_name"), resultSet.getString("moduleinfo_branch_commit_message"),
              resultSet.getString("moduleinfo_branch_commit_id"), resultSet.getString("startts"),
              resultSet.getString("status"), LocalDateTime.now().toString().replace('T', ' ')));
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }

    return buildActiveInfos;
  }

  @Override
  public DashboardBuildRepositoryInfo getDashboardBuildRepository(String accountId, String orgId, String projectId,
      String startInterval, String endInterval, String previousStartInterval) {
    LocalDate startDate = LocalDate.parse(startInterval);
    LocalDate endDate = LocalDate.parse(endInterval);
    LocalDate previousStartDate = LocalDate.parse(previousStartInterval);
    String query =
        queryBuilderSelectRepoInfo(accountId, orgId, projectId, previousStartInterval, endDate.plusDays(1).toString());
    List<String> repoName = new ArrayList<>();
    List<String> status = new ArrayList<>();
    List<String> time = new ArrayList<>();
    List<String> commitMessage = new ArrayList<>();

    HashMap<String, Integer> uniqueRepoName = new HashMap<>();

    int totalTries = 0;
    boolean successfulOperation = false;
    while (!successfulOperation && totalTries <= MAX_RETRY_COUNT) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           PreparedStatement statement = connection.prepareStatement(query)) {
        resultSet = statement.executeQuery();
        while (resultSet != null && resultSet.next()) {
          repoName.add(resultSet.getString("moduleinfo_repository"));
          status.add(resultSet.getString("status"));
          time.add(resultSet.getString("startts"));
          commitMessage.add(resultSet.getString("moduleinfo_branch_commit_message"));

          if (!uniqueRepoName.containsKey(resultSet.getString("moduleinfo_repository"))) {
            uniqueRepoName.put(resultSet.getString("moduleinfo_repository"), 1);
          }
        }
        successfulOperation = true;
      } catch (SQLException ex) {
        totalTries++;
      } finally {
        DBUtils.close(resultSet);
      }
    }

    List<RepositoryInfo> repositoryInfoList = new ArrayList<>();
    for (String repositoryName : uniqueRepoName.keySet()) {
      long totalBuild = 0;
      long success = 0;
      long previousSuccess = 0;
      String lastCommit = null;
      String lastCommitTime = null;
      String lastStatus = null;

      HashMap<String, Integer> buildCountMap = new HashMap<>();
      LocalDate startDateCopy = startDate;
      LocalDate endDateCopy = endDate;

      while (!startDateCopy.isAfter(endDateCopy)) {
        buildCountMap.put(startDateCopy.toString(), 0);
        startDateCopy = startDateCopy.plusDays(1);
      }

      for (int i = 0; i < repoName.size(); i++) {
        if (repoName.get(i).contentEquals(repositoryName)) {
          LocalDate variableDate = LocalDate.parse(time.get(i).substring(0, time.get(i).indexOf(' ')));
          if (startDate.compareTo(variableDate) <= 0 && endDate.compareTo(variableDate) >= 0) {
            totalBuild++;

            buildCountMap.put(variableDate.toString(), buildCountMap.get(variableDate.toString()) + 1);

            if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
              success++;
            }

            if (lastCommit == null) {
              lastCommit = commitMessage.get(i);
              lastCommitTime = time.get(i);
              lastStatus = status.get(i);
            } else {
              if (lastCommitTime.compareTo(time.get(i)) <= 0) {
                lastCommitTime = time.get(i);
                lastCommit = commitMessage.get(i);
                lastStatus = status.get(i);
              }
            }
          } else if (status.get(i).contentEquals(ExecutionStatus.SUCCESS.name())) {
            previousSuccess++;
          }
        }
      }

      List<RepositoryBuildInfo> buildCount = new ArrayList<>();
      startDateCopy = startDate;
      endDateCopy = endDate;

      while (!startDateCopy.isAfter(endDateCopy)) {
        buildCount.add(
            RepositoryBuildInfo.builder()
                .time(startDateCopy.toString())
                .builds(BuildRepositoryCount.builder().count(buildCountMap.get(startDateCopy.toString())).build())
                .build());
        startDateCopy = startDateCopy.plusDays(1);
      }

      if (totalBuild > 0) {
        repositoryInfoList.add(getRepositoryInfo(
            repositoryName, totalBuild, success, previousSuccess, lastCommit, lastCommitTime, buildCount, lastStatus));
      }
    }

    return DashboardBuildRepositoryInfo.builder().repositoryInfo(repositoryInfoList).build();
  }

  private RepositoryInfo getRepositoryInfo(String repoName, long totalBuild, long success, long previousSuccess,
      String lastCommit, String lastCommitTime, List<RepositoryBuildInfo> buildCount, String lastStatus) {
    // percentOfSuccess
    double percentOfSuccess = 0.0;
    if (totalBuild != 0) {
      percentOfSuccess = success / (double) totalBuild;
      percentOfSuccess = percentOfSuccess * 100.0;
    }

    // successRate
    double successRate = 0.0;
    if (previousSuccess != 0) {
      successRate = (success - previousSuccess) / (double) previousSuccess;
      successRate = successRate * 100;
    }

    return RepositoryInfo.builder()
        .name(repoName)
        .buildCount(totalBuild)
        .successRate(successRate)
        .percentSuccess(percentOfSuccess)
        .lastCommit(lastCommit)
        .time(lastCommitTime)
        .countList(buildCount)
        .lastStatus(lastStatus)
        .build();
  }
}
