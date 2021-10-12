package io.harness.overviewdashboard.bean;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum OverviewDashboardRequestType {
  GET_PROJECT_LIST,
  GET_CD_TOP_PROJECT_LIST,
  GET_ACTIVE_DEPLOYMENTS_INFO,
  GET_MOST_ACTIVE_SERVICES,
  GET_DEPLOYMENT_STATS_SUMMARY,
  GET_SERVICES_COUNT,
  GET_ENV_COUNT,
  GET_PIPELINES_COUNT,
  GET_PROJECTS_COUNT
}
