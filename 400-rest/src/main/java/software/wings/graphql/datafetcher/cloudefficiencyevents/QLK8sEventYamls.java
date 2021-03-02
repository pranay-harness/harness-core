package software.wings.graphql.datafetcher.cloudefficiencyevents;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

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
@TargetModule(Module._375_CE_GRAPHQL)
public class QLK8sEventYamls {
  String oldYaml;
  String newYaml;
}
