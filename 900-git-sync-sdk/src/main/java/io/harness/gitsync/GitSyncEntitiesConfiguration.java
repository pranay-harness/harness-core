/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.EntityType;
import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.beans.YamlDTO;
import io.harness.gitsync.entityInfo.GitSdkEntityHandlerInterface;
import io.harness.gitsync.persistance.GitSyncableEntity;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class GitSyncEntitiesConfiguration {
  EntityType entityType;
  Class<? extends GitSyncableEntity> entityClass;
  Class<? extends YamlDTO> yamlClass;
  Class<? extends GitSdkEntityHandlerInterface<? extends GitSyncableEntity, ? extends YamlDTO>> entityHelperClass;
}
