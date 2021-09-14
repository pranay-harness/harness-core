/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.permissions;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLAppFilter;

import java.util.Set;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAppPermissionsKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLAppPermission {
  QLPermissionType permissionType;
  QLAppFilter applications;
  QLServicePermissions services;
  QLEnvPermissions environments;
  QLWorkflowPermissions workflows;
  QLDeploymentPermissions deployments;
  QLPipelinePermissions pipelines;
  QLProivionerPermissions provisioners;
  Set<QLActions> actions;
}
