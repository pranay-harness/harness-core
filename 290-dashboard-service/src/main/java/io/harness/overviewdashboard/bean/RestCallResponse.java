package io.harness.overviewdashboard.bean;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(PL)
public class RestCallResponse<T> {
  T response;
  OverviewDashboardRequestType requestType;
  Exception ex;
}
