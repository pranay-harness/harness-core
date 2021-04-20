package software.wings.graphql.schema.type.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;

@Scope(PermissionAttribute.ResourceType.SETTING)
@Builder
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLTriggerPayload {
  String clientMutationId;
  QLTrigger trigger;
}
