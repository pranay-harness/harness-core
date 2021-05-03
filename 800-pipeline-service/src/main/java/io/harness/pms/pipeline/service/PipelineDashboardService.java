package io.harness.pms.pipeline.service;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.Dashboard.DashboardPipelineExecutionInfo;
import io.harness.pms.Dashboard.DashboardPipelineHealthInfo;

@OwnedBy(HarnessTeam.CDC)
public interface PipelineDashboardService {
  DashboardPipelineHealthInfo getDashboardPipelineHealthInfo(String accountId, String orgId, String projectId,
      String pipelineId, String startInterval, String endInterval, String previousStartInterval, String moduleInfo);

  DashboardPipelineExecutionInfo getDashboardPipelineExecutionInfo(String accountId, String orgId, String projectId,
      String pipelineId, String startInterval, String endInterval, String moduleInfo);
}
