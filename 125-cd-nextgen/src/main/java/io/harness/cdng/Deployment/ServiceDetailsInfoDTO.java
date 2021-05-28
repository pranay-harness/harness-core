package io.harness.cdng.Deployment;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class ServiceDetailsInfoDTO {
  List<ServiceDetailsDTO> serviceDeploymentDetailsList;
}
