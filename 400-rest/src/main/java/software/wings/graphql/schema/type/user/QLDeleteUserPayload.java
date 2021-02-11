package software.wings.graphql.schema.type.user;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLDeleteUserPayload implements QLMutationPayload {
  private String clientMutationId;
}
