package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.time.OffsetDateTime;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLWorkflowKeys")
@Scope(ResourceType.APPLICATION)
public class QLWorkflow implements QLObject {
  private String id;
  private String name;
  private String description;
  private OffsetDateTime createdAt;
  private QLUser createdBy;
}
