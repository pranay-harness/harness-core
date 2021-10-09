package software.wings.graphql.schema.mutation.deploymentfreezewindow.payload;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.deploymentfreezewindow.QLDeploymentFreezeWindow;
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
public class QLUpdateDeploymentFreezeWindowPayload implements QLMutationPayload {
  String clientMutationId;
  QLDeploymentFreezeWindow deploymentFreezeWindow;
}
