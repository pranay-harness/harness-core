package software.wings.graphql.schema.type.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLFromTriggeringArtifactSourceKeys")
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLFromTriggeringArtifactSource implements QLArtifactSelection {
  String serviceId;
  String serviceName;
}
