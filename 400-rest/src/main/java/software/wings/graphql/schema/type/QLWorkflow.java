package software.wings.graphql.schema.type;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWorkflowKeys")
@Scope(ResourceType.APPLICATION)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLWorkflow implements QLObject {
  private String id;
  private String applicationId;
  private String name;
  private String description;
  private Long createdAt;
  private QLUser createdBy;
}
