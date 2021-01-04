package software.wings.graphql.datafetcher.cloudefficiencyevents;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.K8S_EVENT_YAML_DIFF)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QLK8sEventYamlDiff {
  QLK8sEventYamls data;
}
