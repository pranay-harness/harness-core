package software.wings.graphql.schema.type.permissions;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.Set;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLApplicationFilterKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLAppFilter {
  private QLPermissionsFilterType filterType;
  private Set<String> appIds;
}
