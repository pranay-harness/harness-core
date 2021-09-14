/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.repositories.instancesyncperpetualtaskinfo;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
@OwnedBy(HarnessTeam.DX)
public interface InstanceSyncPerpetualTaskInfoInfoRepository
    extends CrudRepository<InstanceSyncPerpetualTaskInfo, String>, InstanceSyncPerpetualTaskInfoRepositoryCustom {
  Optional<InstanceSyncPerpetualTaskInfo> findByInfrastructureMappingId(String infrastructureMappingId);

  Optional<InstanceSyncPerpetualTaskInfo> findByAccountIdentifierAndPerpetualTaskId(
      String accountIdentifier, String perpetualTaskId);

  void deleteByInfrastructureMappingId(String infrastructureMappingId);

  void deleteByAccountIdentifierAndId(String accountIdentifier, String id);
}
