package io.harness.pms.pipeline.observer;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.events.PipelineCreateEvent;
import io.harness.pms.events.PipelineDeleteEvent;
import io.harness.pms.events.PipelineUpdateEvent;

@OwnedBy(PIPELINE)
public interface PipelineActionObserver {
  default void onDelete(PipelineDeleteEvent pipelineDeleteEvent) {
    // do nothing
  }

  default void onUpdate(PipelineUpdateEvent pipelineUpdateEvent) {
    // do nothing
  }

  default void onCreate(PipelineCreateEvent pipelineCreateEvent) {
    // do Nothing
  }
}
