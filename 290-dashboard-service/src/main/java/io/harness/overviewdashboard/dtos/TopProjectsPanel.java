package io.harness.overviewdashboard.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PL)
public class TopProjectsPanel {
  ExecutionResponse<List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>>> CITopProjectsInfo;
  ExecutionResponse<List<TopProjectsDashboardInfo<CountWithSuccessFailureDetails>>> CDTopProjectsInfo;
  ExecutionResponse<List<TopProjectsDashboardInfo<CountInfo>>> CFTopProjectsInfo;
}
