package io.harness.app.CIDashboard;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

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
import io.harness.app.beans.entities.RepositoryInformation;
import io.harness.app.beans.entities.StatusAndTime;
import io.harness.category.element.UnitTests;
import io.harness.core.ci.services.CIOverviewDashboardServiceImpl;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.rule.Owner;
import io.harness.timescaledb.TimeScaleDBService;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class CIDashboardsApisTest {
  @Mock TimeScaleDBService timeScaleDBService;
  @InjectMocks @Spy private CIOverviewDashboardServiceImpl ciOverviewDashboardServiceImpl;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetCountAndRate() {
    // case 1
    BuildHealth buildHealthExpected1 = BuildHealth.builder().count(100).rate(0.0).build();
    BuildHealth buildHealth1 = ciOverviewDashboardServiceImpl.getCountAndRate(100, 0);
    assertThat(buildHealth1).isEqualTo(buildHealthExpected1);

    // case 2
    BuildHealth buildHealthExpected2 = BuildHealth.builder().count(100).rate(((100 - 50) / (double) 50) * 100).build();
    BuildHealth buildHealth2 = ciOverviewDashboardServiceImpl.getCountAndRate(100, 50);
    assertThat(buildHealth2).isEqualTo(buildHealthExpected2);

    // case 3
    BuildHealth buildHealthExpected3 = BuildHealth.builder().count(100).rate(0.0).build();
    BuildHealth buildHealth3 = ciOverviewDashboardServiceImpl.getCountAndRate(100, 100);
    assertThat(buildHealth3).isEqualTo(buildHealthExpected3);

    // case 4
    BuildHealth buildHealthExpected4 =
        BuildHealth.builder().count(100).rate(((100 - 200) / (double) 200) * 100).build();
    BuildHealth buildHealth4 = ciOverviewDashboardServiceImpl.getCountAndRate(100, 200);
    assertThat(buildHealth4).isEqualTo(buildHealthExpected4);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetDashBoardBuildHealthInfoWithRate() {
    String startInterval = "2021-04-25";
    String endInterval = "2021-04-30";
    String previousInterval = "2021-04-20";

    List<String> status = new ArrayList<>();
    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.ABORTED.name());
    status.add(ExecutionStatus.EXPIRED.name());
    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.SUCCESS.name());

    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.FAILED.name());
    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.SUCCESS.name());

    List<String> time = new ArrayList<>();
    time.add("2021-04-25 16:40:21.123");
    time.add("2021-04-25 16:40:21.123");
    time.add("2021-04-27 16:40:21.123");
    time.add("2021-04-28 16:40:21.123");
    time.add("2021-04-30 16:40:21.123");

    time.add("2021-04-20 16:40:21.123");
    time.add("2021-04-24 16:40:21.123");
    time.add("2021-04-23 16:40:21.123");
    time.add("2021-04-21 16:40:21.123");

    StatusAndTime statusAndTime = StatusAndTime.builder().time(time).status(status).build();
    String queryRequired =
        "select status,startts from pipeline_execution_summary_ci where accountid='acc' and orgidentifier='org' and projectidentifier='pro' and startts between '"
        + previousInterval + "' and '2021-05-01';";

    doReturn(statusAndTime).when(ciOverviewDashboardServiceImpl).queryCalculatorForStatusAndTime(queryRequired);

    DashboardBuildsHealthInfo resultBuildHealth = ciOverviewDashboardServiceImpl.getDashBoardBuildHealthInfoWithRate(
        "acc", "org", "pro", startInterval, endInterval, previousInterval);

    DashboardBuildsHealthInfo expectedBuildHealth =
        DashboardBuildsHealthInfo.builder()
            .builds(BuildInfo.builder()
                        .total(BuildHealth.builder().count(5).rate(((5 - 4) / (double) 4) * 100).build())
                        .success(BuildHealth.builder().count(3).rate(0.0).build())
                        .failed(BuildHealth.builder().count(2).rate(100).build())
                        .build())
            .build();

    assertThat(resultBuildHealth).isEqualTo(expectedBuildHealth);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetBuildExecutionBetweenIntervals() {
    String startInterval = "2021-04-25";
    String endInterval = "2021-04-30";

    List<String> status = new ArrayList<>();
    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.ABORTED.name());
    status.add(ExecutionStatus.EXPIRED.name());
    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.FAILED.name());
    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.SUCCESS.name());

    List<String> time = new ArrayList<>();
    time.add("2021-04-25 16:40:21.123");
    time.add("2021-04-25 16:40:21.123");
    time.add("2021-04-27 16:40:21.123");
    time.add("2021-04-28 16:40:21.123");
    time.add("2021-04-30 16:40:21.123");
    time.add("2021-04-28 16:40:21.123");
    time.add("2021-04-26 16:40:21.123");
    time.add("2021-04-25 16:40:21.123");
    time.add("2021-04-25 16:40:21.123");

    StatusAndTime statusAndTime = StatusAndTime.builder().time(time).status(status).build();
    String queryRequired =
        "select status,startts from pipeline_execution_summary_ci where accountid='acc' and orgidentifier='org' and projectidentifier='pro' and startts between '"
        + startInterval + "' and '2021-05-01';";

    doReturn(statusAndTime).when(ciOverviewDashboardServiceImpl).queryCalculatorForStatusAndTime(queryRequired);

    DashboardBuildExecutionInfo resultBuildExecution = ciOverviewDashboardServiceImpl.getBuildExecutionBetweenIntervals(
        "acc", "org", "pro", startInterval, endInterval);

    List<BuildExecutionInfo> buildExecutionInfoList = new ArrayList<>();
    buildExecutionInfoList.add(BuildExecutionInfo.builder()
                                   .time("2021-04-25")
                                   .builds(BuildCount.builder().total(4).success(3).failed(1).build())
                                   .build());
    buildExecutionInfoList.add(BuildExecutionInfo.builder()
                                   .time("2021-04-26")
                                   .builds(BuildCount.builder().total(1).success(0).failed(1).build())
                                   .build());
    buildExecutionInfoList.add(BuildExecutionInfo.builder()
                                   .time("2021-04-27")
                                   .builds(BuildCount.builder().total(1).success(0).failed(1).build())
                                   .build());
    buildExecutionInfoList.add(BuildExecutionInfo.builder()
                                   .time("2021-04-28")
                                   .builds(BuildCount.builder().total(2).success(2).failed(0).build())
                                   .build());
    buildExecutionInfoList.add(BuildExecutionInfo.builder()
                                   .time("2021-04-29")
                                   .builds(BuildCount.builder().total(0).success(0).failed(0).build())
                                   .build());
    buildExecutionInfoList.add(BuildExecutionInfo.builder()
                                   .time("2021-04-30")
                                   .builds(BuildCount.builder().total(1).success(1).failed(0).build())
                                   .build());
    DashboardBuildExecutionInfo expectedBuildExecution =
        DashboardBuildExecutionInfo.builder().buildExecutionInfoList(buildExecutionInfoList).build();

    assertThat(resultBuildExecution).isEqualTo(expectedBuildExecution);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetDashboardBuildFailureInfo() {
    String queryRequired =
        "select name, moduleinfo_branch_name, moduleinfo_branch_commit_message, moduleinfo_branch_commit_id, startts, endts  from pipeline_execution_summary_ci where accountid='acc' and orgidentifier='org' and projectidentifier='pro' and status in ('FAILED','ABORTED','EXPIRED') ORDER BY startts DESC LIMIT 5;";

    List<BuildFailureInfo> buildFailureInfos = new ArrayList<>();
    buildFailureInfos.add(BuildFailureInfo.builder()
                              .piplineName("pip")
                              .branch("branch")
                              .commit("commit")
                              .commitID("commitId")
                              .startTs("2021-04-21")
                              .endTs("2021-04-22")
                              .build());

    doReturn(buildFailureInfos).when(ciOverviewDashboardServiceImpl).queryCalculatorBuildFailureInfo(queryRequired);

    assertThat(buildFailureInfos)
        .isEqualTo(ciOverviewDashboardServiceImpl.getDashboardBuildFailureInfo("acc", "org", "pro", 5));
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetDashboardBuildActiveInfo() {
    String queryRequired =
        "select name, moduleinfo_branch_name, moduleinfo_branch_commit_message, moduleinfo_branch_commit_id, startts, status  from pipeline_execution_summary_ci where accountid='acc' and orgidentifier='org' and projectidentifier='pro' and status IN ( 'RUNNING' , 'INTERVENTION_WAITING' , 'TIMED_WAITING' , 'ASYNC_WAITING' , 'TASK_WAITING' , 'DISCONTINUING' , 'APPROVAL_WAITING' , 'RESOURCE_WAITING' ) ORDER BY startts DESC LIMIT 5;";

    List<BuildActiveInfo> buildActiveInfos = new ArrayList<>();
    buildActiveInfos.add(BuildActiveInfo.builder()
                             .piplineName("pip")
                             .branch("branch")
                             .commit("commit")
                             .commitID("commitId")
                             .startTs("2021-04-21")
                             .endTs("2021-04-22")
                             .status("Running")
                             .build());

    doReturn(buildActiveInfos).when(ciOverviewDashboardServiceImpl).queryCalculatorBuildActiveInfo(queryRequired);

    assertThat(buildActiveInfos)
        .isEqualTo(ciOverviewDashboardServiceImpl.getDashboardBuildActiveInfo("acc", "org", "pro", 5));
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetDashboardBuildRepository() {
    String startInterval = "2021-04-25";
    String endInterval = "2021-04-30";
    String previousInterval = "2021-04-20";

    List<String> repoName = new ArrayList<>();
    List<String> status = new ArrayList<>();
    List<String> time = new ArrayList<>();
    List<String> commitMessage = new ArrayList<>();

    repoName.add("repo1");
    repoName.add("repo2");
    repoName.add("repo2");
    repoName.add("repo3");
    repoName.add("repo1");

    repoName.add("repo2");
    repoName.add("repo3");
    repoName.add("repo3");
    repoName.add("repo4");

    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.ABORTED.name());
    status.add(ExecutionStatus.EXPIRED.name());
    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.SUCCESS.name());

    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.FAILED.name());
    status.add(ExecutionStatus.SUCCESS.name());
    status.add(ExecutionStatus.SUCCESS.name());

    time.add("2021-04-25 16:40:21.123");
    time.add("2021-04-25 16:40:21.123");
    time.add("2021-04-25 16:50:21.123");
    time.add("2021-04-28 16:40:21.123");
    time.add("2021-04-30 16:40:21.123");

    time.add("2021-04-20 16:40:21.123");
    time.add("2021-04-24 16:40:21.123");
    time.add("2021-04-23 16:40:21.123");
    time.add("2021-04-21 16:40:21.123");

    commitMessage.add("commit101");
    commitMessage.add("commit102");
    commitMessage.add("commit103");
    commitMessage.add("commit104");
    commitMessage.add("commit105");

    commitMessage.add("commit106");
    commitMessage.add("commit107");
    commitMessage.add("commit108");
    commitMessage.add("commit109");

    String queryRequired =
        "select moduleinfo_repository, status, startts, moduleinfo_branch_commit_message  from pipeline_execution_summary_ci where accountid='acc' and orgidentifier='org' and projectidentifier='pro' and moduleinfo_repository IS NOT NULL and startts between '"
        + previousInterval + "' and '2021-05-01';";

    RepositoryInformation repositoryInformation = RepositoryInformation.builder()
                                                      .repoName(repoName)
                                                      .status(status)
                                                      .commitMessage(commitMessage)
                                                      .time(time)
                                                      .build();
    doReturn(repositoryInformation).when(ciOverviewDashboardServiceImpl).queryRepositoryCalculator(queryRequired);

    DashboardBuildRepositoryInfo resultRepoInfo = ciOverviewDashboardServiceImpl.getDashboardBuildRepository(
        "acc", "org", "pro", startInterval, endInterval, previousInterval);

    List<RepositoryBuildInfo> repo1 = new ArrayList<>();
    repo1.add(RepositoryBuildInfo.builder()
                  .time("2021-04-25")
                  .builds(BuildRepositoryCount.builder().count(1).build())
                  .build());
    repo1.add(RepositoryBuildInfo.builder()
                  .time("2021-04-26")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());
    repo1.add(RepositoryBuildInfo.builder()
                  .time("2021-04-27")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());
    repo1.add(RepositoryBuildInfo.builder()
                  .time("2021-04-28")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());
    repo1.add(RepositoryBuildInfo.builder()
                  .time("2021-04-29")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());
    repo1.add(RepositoryBuildInfo.builder()
                  .time("2021-04-30")
                  .builds(BuildRepositoryCount.builder().count(1).build())
                  .build());

    // repo 2
    List<RepositoryBuildInfo> repo2 = new ArrayList<>();
    repo2.add(RepositoryBuildInfo.builder()
                  .time("2021-04-25")
                  .builds(BuildRepositoryCount.builder().count(2).build())
                  .build());
    repo2.add(RepositoryBuildInfo.builder()
                  .time("2021-04-26")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());
    repo2.add(RepositoryBuildInfo.builder()
                  .time("2021-04-27")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());
    repo2.add(RepositoryBuildInfo.builder()
                  .time("2021-04-28")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());
    repo2.add(RepositoryBuildInfo.builder()
                  .time("2021-04-29")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());
    repo2.add(RepositoryBuildInfo.builder()
                  .time("2021-04-30")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());

    List<RepositoryBuildInfo> repo3 = new ArrayList<>();
    repo3.add(RepositoryBuildInfo.builder()
                  .time("2021-04-25")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());
    repo3.add(RepositoryBuildInfo.builder()
                  .time("2021-04-26")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());
    repo3.add(RepositoryBuildInfo.builder()
                  .time("2021-04-27")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());
    repo3.add(RepositoryBuildInfo.builder()
                  .time("2021-04-28")
                  .builds(BuildRepositoryCount.builder().count(1).build())
                  .build());
    repo3.add(RepositoryBuildInfo.builder()
                  .time("2021-04-29")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());
    repo3.add(RepositoryBuildInfo.builder()
                  .time("2021-04-30")
                  .builds(BuildRepositoryCount.builder().count(0).build())
                  .build());

    List<RepositoryInfo> repositoryInfoList = new ArrayList<>();

    repositoryInfoList.add(RepositoryInfo.builder()
                               .name("repo2")
                               .time("2021-04-25 16:50:21.123")
                               .lastCommit("commit103")
                               .buildCount(2)
                               .countList(repo2)
                               .lastStatus(ExecutionStatus.EXPIRED.name())
                               .percentSuccess(0)
                               .successRate(-100.0)
                               .build());

    repositoryInfoList.add(RepositoryInfo.builder()
                               .name("repo3")
                               .time("2021-04-28 16:40:21.123")
                               .lastCommit("commit104")
                               .buildCount(1)
                               .countList(repo3)
                               .lastStatus(ExecutionStatus.SUCCESS.name())
                               .percentSuccess(100.0)
                               .successRate(0.0)
                               .build());

    repositoryInfoList.add(RepositoryInfo.builder()
                               .name("repo1")
                               .time("2021-04-30 16:40:21.123")
                               .lastCommit("commit105")
                               .buildCount(2)
                               .countList(repo1)
                               .lastStatus(ExecutionStatus.SUCCESS.name())
                               .percentSuccess(100.0)
                               .successRate(0.0)
                               .build());

    DashboardBuildRepositoryInfo expectedRepoInfo =
        DashboardBuildRepositoryInfo.builder().repositoryInfo(repositoryInfoList).build();

    assertThat(resultRepoInfo).isEqualTo(expectedRepoInfo);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetActiveAndFailedBuild() {
    BuildFailureInfo failureBuild = BuildFailureInfo.builder()
                                        .piplineName("pip1")
                                        .commit("commit1")
                                        .endTs("endts")
                                        .startTs("startts")
                                        .commitID("id1")
                                        .branch("branch1")
                                        .build();
    BuildActiveInfo activeInfo = BuildActiveInfo.builder()
                                     .piplineName("pip2")
                                     .commit("commit2")
                                     .branch("branch2")
                                     .commitID("id2")
                                     .endTs("endts")
                                     .startTs("startts")
                                     .status(ExecutionStatus.RUNNING.name())
                                     .build();

    assertThat(failureBuild)
        .isEqualTo(ciOverviewDashboardServiceImpl.getBuildFailureInfo(
            "pip1", "branch1", "commit1", "id1", "startts", "endts"));
    assertThat(activeInfo)
        .isEqualTo(ciOverviewDashboardServiceImpl.getBuildActiveInfo(
            "pip2", "branch2", "commit2", "id2", "startts", ExecutionStatus.RUNNING.name(), "endts"));
  }
}
