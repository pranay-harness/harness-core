package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.iterator.IteratorConfig;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.PIPELINE)
@Value
@Builder
public class PipelineServiceIteratorsConfig {
  IteratorConfig approvalInstanceIteratorConfig;
}
