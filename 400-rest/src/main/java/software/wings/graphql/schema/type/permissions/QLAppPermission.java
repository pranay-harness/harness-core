package software.wings.graphql.schema.type.permissions;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
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
@OwnedBy(PL)
public class QLAppPermission {
  QLPermissionType permissionType;
  QLAppFilter applications;
  QLServicePermissions services;
  QLEnvPermissions environments;
  QLWorkflowPermissions workflows;
  QLDeploymentPermissions deployments;
  QLPipelinePermissions pipelines;
  QLProivionerPermissions provisioners;
  QLTemplatePermissions templates;
  Set<QLActions> actions;
}
