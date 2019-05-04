package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.api.DeploymentType;
import software.wings.utils.ArtifactType;

import java.time.ZonedDateTime;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLServiceKeys")
public class QLService implements QLObject {
  private String id;
  private String name;
  private String description;
  private ArtifactType artifactType;
  private DeploymentType deploymentType;
  private ZonedDateTime createdAt;
  private QLUser createdBy;
}
