package software.wings.graphql.schema.type.aggregation.billing;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.graphql.datafetcher.billing.QLEntityData;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@Scope(ResourceType.USER)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLFilterValuesData implements QLData {
  List<QLEntityData> cloudServiceNames;
  List<QLEntityData> launchTypes;
  List<QLEntityData> instanceIds;
  List<QLEntityData> namespaces;
  List<QLEntityData> workloadNames;
  List<QLEntityData> cloudProviders;
  List<QLEntityData> applications;
  List<QLEntityData> environments;
  List<QLEntityData> services;
  List<QLEntityData> clusters;
}
