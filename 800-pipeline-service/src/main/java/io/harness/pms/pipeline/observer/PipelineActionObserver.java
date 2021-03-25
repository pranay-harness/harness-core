package io.harness.pms.pipeline.observer;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.pipeline.PipelineEntity;

@OwnedBy(PIPELINE)
public interface PipelineActionObserver {
  void onDelete(PipelineEntity pipelineEntity);
}
