package software.wings.graphql.schema.type;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.api.DeploymentType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.utils.ArtifactType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLServiceKeys")
@Scope(ResourceType.APPLICATION)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLService implements QLObject {
  private String id;
  private String applicationId;
  private String name;
  private String description;
  private ArtifactType artifactType;
  private DeploymentType deploymentType;
  private Long createdAt;
  private QLUser createdBy;
}
