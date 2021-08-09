package io.harness.ng.core.entitysetupusage.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class EntitySetupUsageBatchDTO {
  String referredByEntity;
  List<EntitySetupUsageDTO> referredEntities;
}
