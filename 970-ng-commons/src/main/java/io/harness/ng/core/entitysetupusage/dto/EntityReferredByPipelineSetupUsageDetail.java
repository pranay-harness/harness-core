package io.harness.ng.core.entitysetupusage.dto;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PIPELINE)
@Value
@Builder
@TypeAlias("EntityReferredByPipelineSetupUsageDetail")
public class EntityReferredByPipelineSetupUsageDetail implements SetupUsageDetail {
  String identifier;
  String referenceType;
}
