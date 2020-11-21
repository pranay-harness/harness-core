package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLFromTriggeringPipelineKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLFromTriggeringPipeline implements QLArtifactSelection {
  String serviceId;
  String serviceName;
}
