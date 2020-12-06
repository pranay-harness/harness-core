package software.wings.graphql.schema.type.cloudProvider;

import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCloudProviderConnectionKeys")
@Scope(ResourceType.SETTING)
public class QLCloudProviderConnection implements QLObject {
  private QLPageInfo pageInfo;
  @Singular private List<QLCloudProvider> nodes;
}
