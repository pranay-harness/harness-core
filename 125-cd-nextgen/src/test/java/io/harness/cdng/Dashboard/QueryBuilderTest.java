package io.harness.cdng.Dashboard;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.service.dashboard.CDOverviewDashboardServiceImpl;
import io.harness.rule.Owner;

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
}
