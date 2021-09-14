/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUserGroupPermissionsKeys")
@Scope(PermissionAttribute.ResourceType.USER) // Change the scope
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLGroupPermissions {
  QLAccountPermissions accountPermissions;
  List<QLAppPermission> appPermissions; // Can have this as a set too
}
