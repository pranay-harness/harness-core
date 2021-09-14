/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLProivionerPermissionsKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLProivionerPermissions {
  private QLPermissionsFilterType filterType;
  private Set<String> provisionerIds;
}
