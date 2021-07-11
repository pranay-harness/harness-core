package io.harness.dtos.instancesyncperpetualtaskinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
public class InstanceSyncPerpetualTaskInfoDTO {
  String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String infrastructureMappingId;
  List<String> deploymentSummaryIdList;
  List<DeploymentInfoDetailsDTO> deploymentInfoDetailsDTOList;
  long createdAt;
  long lastUpdatedAt;
}
