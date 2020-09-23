package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLTagsKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLTags {
  String name;
  String[] values;
}
