package software.wings.graphql.schema.mutation.tag;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.QLTagLink;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLAttachTagPayload implements QLMutationPayload {
  private String clientMutationId;
  private QLTagLink tagLink;
}
