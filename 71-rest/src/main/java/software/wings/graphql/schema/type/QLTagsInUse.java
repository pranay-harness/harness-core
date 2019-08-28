package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLTagsInUseKeys")
@Scope(ResourceType.SETTING)
public class QLTagsInUse implements QLObject {
  private String name;
  private List<String> values;
}
