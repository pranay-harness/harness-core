package software.wings.graphql.datafetcher.cloudefficiencyevents;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
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
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class QLK8sEventYamlDiff {
  QLK8sEventYamls data;
}
