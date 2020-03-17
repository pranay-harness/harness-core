package software.wings.graphql.schema.type.permissions;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLAppFilter;

import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAppPermissionsKeys")
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