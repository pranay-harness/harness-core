/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.dtos.instancesyncperpetualtaskinfo;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@OwnedBy(HarnessTeam.DX)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class InstanceSyncPerpetualTaskInfoDTO {
  String id;
  String accountIdentifier;
  String infrastructureMappingId;
  List<DeploymentInfoDetailsDTO> deploymentInfoDetailsDTOList;
  String perpetualTaskId;
  long createdAt;
  long lastUpdatedAt;
}
