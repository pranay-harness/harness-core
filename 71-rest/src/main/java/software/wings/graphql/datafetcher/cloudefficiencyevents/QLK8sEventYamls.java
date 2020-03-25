package software.wings.graphql.datafetcher.cloudefficiencyevents;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.K8S_EVENT_YAML_DIFF)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLK8sEventYamls {
  String oldYaml;
  String newYaml;
}
