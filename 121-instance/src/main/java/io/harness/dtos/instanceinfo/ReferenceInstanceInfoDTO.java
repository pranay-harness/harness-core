package io.harness.dtos.instanceinfo;

import io.harness.annotations.dev.HarnessTeam;

import lombok.Builder;
import lombok.Data;

@io.harness.annotations.dev.OwnedBy(HarnessTeam.DX)
@Data
@Builder
public class ReferenceInstanceInfoDTO extends InstanceInfoDTO {
  String podName;
}
