package software.wings.graphql.schema.mutation.deploymentfreezewindow.input;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLDeleteDeploymentFreezeWindowInput implements QLMutationInput {
  String clientMutationId;
  String id;
}
