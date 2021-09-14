/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.rbac;

import io.harness.EntityType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.experimental.UtilityClass;
import org.apache.groovy.util.Maps;

@UtilityClass
@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineReferredEntityPermissionHelper {
  private final String PERMISSION_PLACE_HOLDER = "core_%s_%s";

  public final Map<EntityType, String> coreEntityTypeToPermissionEntityName =
      Maps.of(EntityType.CONNECTORS, NGResourceType.CONNECTOR, EntityType.SECRETS, NGResourceType.SECRETS,
          EntityType.SERVICE, NGResourceType.SERVICE, EntityType.ENVIRONMENT, NGResourceType.ENVIRONMENT);

  public String getPermissionForGivenType(EntityType entityType, boolean isNew) {
    String permission = "access";
    if (isNew) {
      permission = "edit";
    }
    if (coreEntityTypeToPermissionEntityName.containsKey(entityType)) {
      return String.format(
          PERMISSION_PLACE_HOLDER, coreEntityTypeToPermissionEntityName.get(entityType).toLowerCase(), permission);
    }
    throw new UnsupportedOperationException();
  }

  public String getEntityName(EntityType entityType) {
    return coreEntityTypeToPermissionEntityName.get(entityType);
  }
}
