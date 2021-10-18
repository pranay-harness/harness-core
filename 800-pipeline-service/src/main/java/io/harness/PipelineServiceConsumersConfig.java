package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineServiceConsumersConfig {
  PipelineServiceConsumerConfig interrupt;
  PipelineServiceConsumerConfig orchestrationEvent;
  PipelineServiceConsumerConfig facilitatorEvent;
  PipelineServiceConsumerConfig nodeStart;
  PipelineServiceConsumerConfig progress;
  PipelineServiceConsumerConfig advise;
  PipelineServiceConsumerConfig resume;
  PipelineServiceConsumerConfig sdkResponse;
  PipelineServiceConsumerConfig graphUpdate;
  PipelineServiceConsumerConfig partialPlanResponse;
  PipelineServiceConsumerConfig createPlan;
  PipelineServiceConsumerConfig planNotify;
  PipelineServiceConsumerConfig pmsNotify;
}
