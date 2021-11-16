package io.harness.repositories.pipeline;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.PipelineMetadata;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PipelineMetadataRepositoryCustom {
  PipelineMetadata incCounter(String accountId, String orgId, String projectIdentifier, String pipelineId);

  long updateExecutionInfo(String accountId, String orgId, String projectIdentifier, String pipelineId,
      ExecutionSummaryInfo executionSummaryInfo);
}
