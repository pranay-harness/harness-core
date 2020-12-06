package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLOutcomeConnectionKeys")
@Scope(ResourceType.APPLICATION)
public class QLOutcomeConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLOutcome> nodes;
}
