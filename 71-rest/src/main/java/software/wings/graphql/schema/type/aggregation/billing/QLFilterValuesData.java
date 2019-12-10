package software.wings.graphql.schema.type.aggregation.billing;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLFilterValuesData implements QLData {
  String[] cloudServiceNames;
  String[] launchTypes;
  String[] instanceIds;
  String[] clusterIds;
  String[] namespaces;
  String[] workloadNames;
}
