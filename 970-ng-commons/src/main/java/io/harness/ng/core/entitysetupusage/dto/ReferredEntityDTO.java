package io.harness.ng.core.entitysetupusage.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.EntityDetail;

import lombok.Builder;
import lombok.Value;

@OwnedBy(DX)
@Value
@Builder
public class ReferredEntityDTO {
  EntityDetail referredEntity;
}
