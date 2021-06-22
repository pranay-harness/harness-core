package io.harness.ng.core.entitysetupusage.dto;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@OwnedBy(PIPELINE)
@Value
@Builder
public class EntityReferredByPipelineSetupUsageDetail implements SetupUsageDetail {
  String identifier;
  String type;
}
