package software.wings.graphql.schema.type.deploymentfreezewindow;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.graphql.schema.type.QLObject;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@Scope(PermissionAttribute.ResourceType.USER)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLDeploymentFreezeWindowConnection implements QLObject {
  QLPageInfo pageInfo;
  List<QLDeploymentFreezeWindow> nodes;
}
