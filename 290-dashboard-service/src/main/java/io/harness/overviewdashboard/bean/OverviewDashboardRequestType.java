package io.harness.overviewdashboard.bean;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(PL)
public enum OverviewDashboardRequestType {
  GET_PROJECT_LIST,
  GET_DEPLOYMENTS_STATS_SUMMARY,
  GET_MOST_ACTIVE_SERVICES,
  GET_TIME_WISE_DEPLOYMENT_INFO,
  GET_SERVICES_COUNT,
  GET_ENV_COUNT,
  GET_PIPELINES_COUNT
}
