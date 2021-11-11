package io.harness.ng.overview.dto;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.models.InstanceDetailsByBuildId;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class InstancesByBuildIdList {
  List<InstanceDetailsByBuildId> instancesByBuildIdList;
}
