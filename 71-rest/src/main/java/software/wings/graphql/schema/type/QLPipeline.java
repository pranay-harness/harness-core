package software.wings.graphql.schema.type;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLPipelineKeys")
@Scope(ResourceType.APPLICATION)
public class QLPipeline implements QLObject {
  private String id;
  private String name;
  private String appId;
  private String description;
  private Long createdAt;
  private QLUser createdBy;
}
