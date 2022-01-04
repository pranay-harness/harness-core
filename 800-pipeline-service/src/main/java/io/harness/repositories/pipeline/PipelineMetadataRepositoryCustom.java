package io.harness.repositories.pipeline;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.pipeline.ExecutionSummaryInfo;
import io.harness.pms.pipeline.PipelineMetadata;

import java.util.Optional;

@OwnedBy(HarnessTeam.PIPELINE)
public interface PipelineMetadataRepositoryCustom {
  PipelineMetadata incCounter(String accountId, String orgId, String projectIdentifier, String pipelineId);

  long getRunSequence(String accountId, String orgId, String projectIdentifier, String pipelineId,
      ExecutionSummaryInfo executionSummaryInfo);

  Optional<PipelineMetadata> getPipelineMetadata(
      String accountId, String orgId, String projectIdentifier, String identifier);
}
