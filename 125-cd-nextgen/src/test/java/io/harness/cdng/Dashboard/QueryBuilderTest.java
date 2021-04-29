package io.harness.cdng.Dashboard;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.service.dashboard.CDOverviewDashboardServiceImpl;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.rule.Owner;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.CDC)
public class QueryBuilderTest {
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testSelectStatusTime() {
    String expectedQueryResult =
        "select status,startts from pipeline_execution_summary_cd where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and startts between '2021-04-21' and '2021-04-26';";
    String queryResult = new CDOverviewDashboardServiceImpl().queryBuilderSelectStatusTime(
        "accountId", "orgId", "projectId", "2021-04-21", "2021-04-26");
    assertThat(queryResult).isEqualTo(expectedQueryResult);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testQueryBuilderEnvironmentType() {
    String expectedQueryResult =
        "select env_type from pipeline_execution_summary_cd, service_infra_info where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and pipeline_execution_summary_cd.id=pipeline_execution_summary_cd_id and env_type is not null and startts between '2021-04-21' and '2021-04-26';";
    String queryResult = new CDOverviewDashboardServiceImpl().queryBuilderEnvironmentType(
        "accountId", "orgId", "projectId", "2021-04-21", "2021-04-26");
    assertThat(queryResult).isEqualTo(expectedQueryResult);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testGetRate() {
    long current = 0;
    long previous = 0;
    assertThat(new CDOverviewDashboardServiceImpl().getRate(current, previous)).isEqualTo(0.0);

    current = 5;
    assertThat(new CDOverviewDashboardServiceImpl().getRate(current, previous)).isEqualTo(0.0);

    current = 10;
    previous = 5;
    double rateExpected = (current - previous) / (double) previous;
    rateExpected = rateExpected * 100.0;
    assertThat(new CDOverviewDashboardServiceImpl().getRate(current, previous)).isEqualTo(rateExpected);

    current = 10;
    previous = 15;
    rateExpected = (current - previous) / (double) previous;
    rateExpected = rateExpected * 100.0;
    assertThat(new CDOverviewDashboardServiceImpl().getRate(current, previous)).isEqualTo(rateExpected);

    current = 15;
    previous = 15;
    assertThat(new CDOverviewDashboardServiceImpl().getRate(current, previous)).isEqualTo(0.0);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testQueryBuilderStatus() {
    List<String> failedStatusList =
        Arrays.asList(ExecutionStatus.FAILED.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.EXPIRED.name());
    List<String> activeStatusList = Arrays.asList(ExecutionStatus.RUNNING.name());
    List<String> pendingStatusList =
        Arrays.asList(ExecutionStatus.INTERVENTION_WAITING.name(), ExecutionStatus.APPROVAL_WAITING.name());

    // failedStatusList
    String expectedQueryResult =
        "select id,name,startts,endTs,status from pipeline_execution_summary_cd where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and status in ('FAILED','ABORTED','EXPIRED') ORDER BY startts DESC LIMIT 20;";
    String queryResult = new CDOverviewDashboardServiceImpl().queryBuilderStatus(
        "accountId", "orgId", "projectId", 20, failedStatusList);
    assertThat(queryResult).isEqualTo(expectedQueryResult);

    // activeStatusList
    expectedQueryResult =
        "select id,name,startts,endTs,status from pipeline_execution_summary_cd where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and status in ('RUNNING') ORDER BY startts DESC LIMIT 20;";
    queryResult = new CDOverviewDashboardServiceImpl().queryBuilderStatus(
        "accountId", "orgId", "projectId", 20, activeStatusList);
    assertThat(queryResult).isEqualTo(expectedQueryResult);

    // pending
    expectedQueryResult =
        "select id,name,startts,endTs,status from pipeline_execution_summary_cd where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and status in ('INTERVENTION_WAITING','APPROVAL_WAITING') ORDER BY startts DESC LIMIT 20;";
    queryResult = new CDOverviewDashboardServiceImpl().queryBuilderStatus(
        "accountId", "orgId", "projectId", 20, pendingStatusList);
    assertThat(queryResult).isEqualTo(expectedQueryResult);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testQueryBuilderServiceTag() {
    List<String> failedStatusList =
        Arrays.asList(ExecutionStatus.FAILED.name(), ExecutionStatus.ABORTED.name(), ExecutionStatus.EXPIRED.name());
    List<String> activeStatusList = Arrays.asList(ExecutionStatus.RUNNING.name());
    List<String> pendingStatusList =
        Arrays.asList(ExecutionStatus.INTERVENTION_WAITING.name(), ExecutionStatus.APPROVAL_WAITING.name());

    List<String> planExecutionId = Arrays.asList("123", "345", "456");

    // failedStatusList
    String expectedQueryResult =
        "select service_name,tag,pipeline_execution_summary_cd_id from service_infra_info, pipeline_execution_summary_cd where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and status in ('FAILED','ABORTED','EXPIRED') and pipeline_execution_summary_cd_id in ('123','345','456') and pipeline_execution_summary_cd.id=pipeline_execution_summary_cd_id and service_name is not null;";
    String queryResult = new CDOverviewDashboardServiceImpl().queryBuilderServiceTag(
        "accountId", "orgId", "projectId", planExecutionId, failedStatusList);
    assertThat(queryResult).isEqualTo(expectedQueryResult);

    // activeStatusList
    expectedQueryResult =
        "select service_name,tag,pipeline_execution_summary_cd_id from service_infra_info, pipeline_execution_summary_cd where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and status in ('RUNNING') and pipeline_execution_summary_cd_id in ('123','345','456') and pipeline_execution_summary_cd.id=pipeline_execution_summary_cd_id and service_name is not null;";
    queryResult = new CDOverviewDashboardServiceImpl().queryBuilderServiceTag(
        "accountId", "orgId", "projectId", planExecutionId, activeStatusList);
    assertThat(queryResult).isEqualTo(expectedQueryResult);

    // pending
    expectedQueryResult =
        "select service_name,tag,pipeline_execution_summary_cd_id from service_infra_info, pipeline_execution_summary_cd where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and status in ('INTERVENTION_WAITING','APPROVAL_WAITING') and pipeline_execution_summary_cd_id in ('123','345','456') and pipeline_execution_summary_cd.id=pipeline_execution_summary_cd_id and service_name is not null;";
    queryResult = new CDOverviewDashboardServiceImpl().queryBuilderServiceTag(
        "accountId", "orgId", "projectId", planExecutionId, pendingStatusList);
    assertThat(queryResult).isEqualTo(expectedQueryResult);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testQueryBuilderSelectWorkload() {
    String queryExpected =
        "select service_name,status,startts,endts,pipeline_execution_summary_cd_id from service_infra_info, pipeline_execution_summary_cd where accountid='acc' and orgidentifier='org' and projectidentifier='pro' and pipeline_execution_summary_cd.id=pipeline_execution_summary_cd_id and service_name is not null and startts between '2021-04-21' and '2021-04-28';";

    assertThat(queryExpected)
        .isEqualTo(new CDOverviewDashboardServiceImpl().queryBuilderSelectWorkload(
            "acc", "org", "pro", "2021-04-21", "2021-04-28"));

    // all parameters as null
    queryExpected =
        "select service_name,status,startts,endts,pipeline_execution_summary_cd_id from service_infra_info, pipeline_execution_summary_cd where ";
    assertThat(queryExpected)
        .isEqualTo(new CDOverviewDashboardServiceImpl().queryBuilderSelectWorkload(null, null, null, null, null));

    // accountIdentifier as null
    queryExpected =
        "select service_name,status,startts,endts,pipeline_execution_summary_cd_id from service_infra_info, pipeline_execution_summary_cd where orgidentifier='org' and projectidentifier='pro' and pipeline_execution_summary_cd.id=pipeline_execution_summary_cd_id and service_name is not null and startts between '2021-04-21' and '2021-04-28';";
    assertThat(queryExpected)
        .isEqualTo(new CDOverviewDashboardServiceImpl().queryBuilderSelectWorkload(
            null, "org", "pro", "2021-04-21", "2021-04-28"));

    // org as null
    queryExpected =
        "select service_name,status,startts,endts,pipeline_execution_summary_cd_id from service_infra_info, pipeline_execution_summary_cd where accountid='acc' and projectidentifier='pro' and pipeline_execution_summary_cd.id=pipeline_execution_summary_cd_id and service_name is not null and startts between '2021-04-21' and '2021-04-28';";

    assertThat(queryExpected)
        .isEqualTo(new CDOverviewDashboardServiceImpl().queryBuilderSelectWorkload(
            "acc", null, "pro", "2021-04-21", "2021-04-28"));

    // proId as null
    queryExpected =
        "select service_name,status,startts,endts,pipeline_execution_summary_cd_id from service_infra_info, pipeline_execution_summary_cd where accountid='acc' and orgidentifier='org' and pipeline_execution_summary_cd.id=pipeline_execution_summary_cd_id and service_name is not null and startts between '2021-04-21' and '2021-04-28';";

    assertThat(queryExpected)
        .isEqualTo(new CDOverviewDashboardServiceImpl().queryBuilderSelectWorkload(
            "acc", "org", null, "2021-04-21", "2021-04-28"));

    // startInterval as null
    queryExpected =
        "select service_name,status,startts,endts,pipeline_execution_summary_cd_id from service_infra_info, pipeline_execution_summary_cd where accountid='acc' and orgidentifier='org' and projectidentifier='pro' and ";

    assertThat(queryExpected)
        .isEqualTo(
            new CDOverviewDashboardServiceImpl().queryBuilderSelectWorkload("acc", "org", "pro", null, "2021-04-28"));

    // startInterval as null
    queryExpected =
        "select service_name,status,startts,endts,pipeline_execution_summary_cd_id from service_infra_info, pipeline_execution_summary_cd where accountid='acc' and orgidentifier='org' and projectidentifier='pro' and ";

    assertThat(queryExpected)
        .isEqualTo(
            new CDOverviewDashboardServiceImpl().queryBuilderSelectWorkload("acc", "org", "pro", "2021-04-28", null));

    // both interval as null
    queryExpected =
        "select service_name,status,startts,endts,pipeline_execution_summary_cd_id from service_infra_info, pipeline_execution_summary_cd where accountid='acc' and orgidentifier='org' and projectidentifier='pro' and ";

    assertThat(queryExpected)
        .isEqualTo(new CDOverviewDashboardServiceImpl().queryBuilderSelectWorkload("acc", "org", "pro", null, null));
  }
}
