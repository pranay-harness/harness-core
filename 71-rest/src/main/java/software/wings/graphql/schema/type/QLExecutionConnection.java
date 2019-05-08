package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLExecutionConnectionKeys")
@Scope(ResourceType.APPLICATION)
public class QLExecutionConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLExecution> nodes;
}
