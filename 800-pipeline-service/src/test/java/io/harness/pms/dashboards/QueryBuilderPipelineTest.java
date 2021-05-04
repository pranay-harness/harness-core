package io.harness.pms.dashboards;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.pipeline.service.PipelineDashboardServiceImpl;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class QueryBuilderPipelineTest {
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testQueryBuilderSelectStatusAndTime() {
    String expectedQueryResult =
        "select status,startts from pipeline_execution_summary_ci where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and pipelineidentifier='pipelineId' and startts between '2021-04-21' and '2021-04-26';";
    String queryResult = new PipelineDashboardServiceImpl().queryBuilderSelectStatusAndTime(
        "accountId", "orgId", "projectId", "pipelineId", "2021-04-21", "2021-04-26", "pipeline_execution_summary_ci");
    assertThat(queryResult).isEqualTo(expectedQueryResult);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testQueryBuilderMedian() {
    String expectedQueryResult =
        "select PERCENTILE_DISC(0.5) within group (order by ((DATE_PART('day', endts::timestamp - startts::timestamp) * 24 + \n"
        + "                DATE_PART('hour', endts::timestamp - startts::timestamp)) * 60 +\n"
        + "                DATE_PART('minute', endts::timestamp - startts::timestamp)) * 60 +\n"
        + "                DATE_PART('second', endts::timestamp - startts::timestamp)) from pipeline_execution_summary_ci where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and pipelineidentifier='pipelineId' and startts between '2021-04-21' and '2021-04-26' and endts is not null;";
    String queryResult = new PipelineDashboardServiceImpl().queryBuilderMedian(
        "accountId", "orgId", "projectId", "pipelineId", "2021-04-21", "2021-04-26", "pipeline_execution_summary_ci");
    assertThat(queryResult).isEqualTo(expectedQueryResult);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testQueryBuilderMean() {
    String expectedQueryResult =
        "select avg(((DATE_PART('day', endts::timestamp - startts::timestamp) * 24 +                                                                                                                                                                                      DATE_PART('hour', endts::timestamp - startts::timestamp)) * 60 +\n"
        + "                DATE_PART('minute', endts::timestamp - startts::timestamp)) * 60 +\n"
        + "                DATE_PART('second', endts::timestamp - startts::timestamp)) from pipeline_execution_summary_ci where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and pipelineidentifier='pipelineId' and startts between '2021-04-21' and '2021-04-26' and endts is not null;";
    String queryResult = new PipelineDashboardServiceImpl().queryBuilderMean(
        "accountId", "orgId", "projectId", "pipelineId", "2021-04-21", "2021-04-26", "pipeline_execution_summary_ci");
    assertThat(queryResult).isEqualTo(expectedQueryResult);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testSelectTableFromModuleInfo() {
    String ci_table_name = "pipeline_execution_summary_ci";
    String cd_table_name = "pipeline_execution_summary_cd";
    assertThat(new PipelineDashboardServiceImpl().selectTableFromModuleInfo("ci")).isEqualTo(ci_table_name);
    assertThat(new PipelineDashboardServiceImpl().selectTableFromModuleInfo("CI")).isEqualTo(ci_table_name);
    assertThat(new PipelineDashboardServiceImpl().selectTableFromModuleInfo("Ci")).isEqualTo(ci_table_name);
    assertThat(new PipelineDashboardServiceImpl().selectTableFromModuleInfo("cI")).isEqualTo(ci_table_name);

    assertThat(new PipelineDashboardServiceImpl().selectTableFromModuleInfo("cd")).isEqualTo(cd_table_name);
    assertThat(new PipelineDashboardServiceImpl().selectTableFromModuleInfo("CD")).isEqualTo(cd_table_name);
    assertThat(new PipelineDashboardServiceImpl().selectTableFromModuleInfo("Cd")).isEqualTo(cd_table_name);
    assertThat(new PipelineDashboardServiceImpl().selectTableFromModuleInfo("cD")).isEqualTo(cd_table_name);
  }
}
