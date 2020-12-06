package software.wings.graphql.schema.type;

import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

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
