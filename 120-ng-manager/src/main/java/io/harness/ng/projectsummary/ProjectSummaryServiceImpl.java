package io.harness.ng.projectsummary;

import static io.harness.timescaledb.tables.ProjectSummaryData.PROJECT_SUMMARY_DATA;

import static org.jooq.impl.DSL.select;
import static org.jooq.impl.DSL.sum;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.timescaledb.tables.pojos.ProjectSummaryData;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectSelectStep;

@Slf4j
@Singleton
@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor = @__({ @Inject }))
public class ProjectSummaryServiceImpl {
  DSLContext dslContext;

  public void createProjectSummary(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String projectName) {
    ProjectSummaryData projectSummaryData = new ProjectSummaryData();
    projectSummaryData.setAccountIdentifier(accountIdentifier);
    projectSummaryData.setOrgIdentifier(orgIdentifier);
    projectSummaryData.setProjectIdentifier(projectIdentifier);
    projectSummaryData.setName(projectName);
    projectSummaryData.setDeploymentCount(0);
    projectSummaryData.setBuildCount(0);
    projectSummaryData.setFeatureFlagsCount(0);
    dslContext.newRecord(PROJECT_SUMMARY_DATA, projectSummaryData).insert();
  }

  public void updateDeploymentCount(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      Integer deploymentCount, Integer buildCount, Integer featureFlagCount) {
    dslContext.update(PROJECT_SUMMARY_DATA)
        .set(PROJECT_SUMMARY_DATA.DEPLOYMENT_COUNT, deploymentCount)
        .set(PROJECT_SUMMARY_DATA.BUILD_COUNT, buildCount)
        .set(PROJECT_SUMMARY_DATA.FEATURE_FLAGS_COUNT, featureFlagCount)
        .where(PROJECT_SUMMARY_DATA.ACCOUNT_IDENTIFIER.eq(accountIdentifier),
            PROJECT_SUMMARY_DATA.ORG_IDENTIFIER.eq(orgIdentifier),
            PROJECT_SUMMARY_DATA.PROJECT_IDENTIFIER.eq(projectIdentifier))
        .execute();
  }

  //  SELECT
  //   org_identifier,
  //  SUM(deployment_count) as totalDeployment,
  //  SUM(build_count) as totalBuild,
  //  SUM(feature_flags_count) as totalFeature
  //  FROM
  //          project_summary_data
  //  WHERE
  //          org_identifier = 'orgIdentifier'
  //  Group By
  //  org_identifier;
  public void aggregateTheProjectData(String orgIdentifier) {
    final String TOTAL_DEPLOYMENT_COUNT = "totalDeployment";
    final String TOTAL_BUILD_COUNT = "totalBuild";
    final String TOTAL_FEATURE_COUNT = "totalFeature";
    SelectSelectStep<? extends Record> selectStep = select(PROJECT_SUMMARY_DATA.ORG_IDENTIFIER,
        sum(PROJECT_SUMMARY_DATA.DEPLOYMENT_COUNT).as(TOTAL_DEPLOYMENT_COUNT),
        sum(PROJECT_SUMMARY_DATA.BUILD_COUNT).as(TOTAL_BUILD_COUNT),
        sum(PROJECT_SUMMARY_DATA.FEATURE_FLAGS_COUNT).as(TOTAL_FEATURE_COUNT));
    ProjectAggregation projectAggregation = dslContext.select(selectStep.getSelect())
                                                .from(PROJECT_SUMMARY_DATA)
                                                .where(PROJECT_SUMMARY_DATA.ORG_IDENTIFIER.eq(orgIdentifier))
                                                .groupBy(PROJECT_SUMMARY_DATA.ORG_IDENTIFIER)
                                                .fetchOneInto(ProjectAggregation.class);
    System.out.println(projectAggregation);
  }
}
