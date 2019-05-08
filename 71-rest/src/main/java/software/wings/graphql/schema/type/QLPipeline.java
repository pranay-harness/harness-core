package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.time.ZonedDateTime;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLPipelineKeys")
@Scope(ResourceType.APPLICATION)
public class QLPipeline implements QLObject {
  private String id;
  private String name;
  private String description;
  private ZonedDateTime createdAt;
  private QLUser createdBy;
}
