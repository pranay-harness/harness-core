/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.dashboards;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.pipeline.service.PipelineDashboardServiceImpl;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class QueryBuilderPipelineTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testQueryBuilderSelectStatusAndTime() {
    String expectedQueryResult =
        "select status,startts from pipeline_execution_summary_ci where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and pipelineidentifier='pipelineId' and startts>=10 and startts<13;";
    String queryResult = new PipelineDashboardServiceImpl().queryBuilderSelectStatusAndTime(
        "accountId", "orgId", "projectId", "pipelineId", 10L, 13L, "pipeline_execution_summary_ci");
    assertThat(queryResult).isEqualTo(expectedQueryResult);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testQueryBuilderMedian() {
    String expectedQueryResult =
        "select PERCENTILE_DISC(0.5) within group (order by (endts-startts)/1000) as percentile_disc from pipeline_execution_summary_ci where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and pipelineidentifier='pipelineId' and startts>=10 and startts<13 and endts is not null;";
    String queryResult = new PipelineDashboardServiceImpl().queryBuilderMedian(
        "accountId", "orgId", "projectId", "pipelineId", 10L, 13L, "pipeline_execution_summary_ci");
    assertThat(queryResult).isEqualTo(expectedQueryResult);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testQueryBuilderMean() {
    String expectedQueryResult =
        "select avg(endts-startts)/1000 as avg from pipeline_execution_summary_ci where accountid='accountId' and orgidentifier='orgId' and projectidentifier='projectId' and pipelineidentifier='pipelineId' and startts>=10 and startts<13 and endts is not null;";
    String queryResult = new PipelineDashboardServiceImpl().queryBuilderMean(
        "accountId", "orgId", "projectId", "pipelineId", 10L, 13L, "pipeline_execution_summary_ci");
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
