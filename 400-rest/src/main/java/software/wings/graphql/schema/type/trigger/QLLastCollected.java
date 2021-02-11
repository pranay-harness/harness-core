package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLLastCollectedKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLLastCollected implements QLArtifactSelection {
  String serviceId;
  String serviceName;
  String artifactSourceId;
  String artifactSourceName;
  Boolean regex;
  String artifactFilter;
}
