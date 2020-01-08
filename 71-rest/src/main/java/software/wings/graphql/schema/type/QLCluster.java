package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLClusterKeys")
@Scope(ResourceType.CLUSTERRECORD)
public class QLCluster {
  String id;
  String name;
  String cloudProviderId;
  String clusterType;
}
