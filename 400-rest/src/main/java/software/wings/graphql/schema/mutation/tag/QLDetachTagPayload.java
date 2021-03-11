package software.wings.graphql.schema.mutation.tag;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLDetachTagPayload implements QLMutationPayload {
  private String clientMutationId;
}
