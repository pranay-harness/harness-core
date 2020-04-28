package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.api.DeploymentType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;
import software.wings.utils.ArtifactType;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLServiceKeys")
@Scope(ResourceType.APPLICATION)
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
