package io.harness.pms.pipeline.observer;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.pipeline.PipelineEntity;

@OwnedBy(PIPELINE)
public interface PipelineActionObserver {
  default void onDelete(PipelineEntity pipelineEntity) {
    // do nothing
  }

  default void onUpdate(PipelineEntity pipelineEntity) {
    // do nothing
  }
}
