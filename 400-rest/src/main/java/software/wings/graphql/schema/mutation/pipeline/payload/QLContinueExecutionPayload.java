package software.wings.graphql.schema.mutation.pipeline.payload;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CDC)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLContinueExecutionPayload {
  private String clientMutationId;
  private boolean status;
}
