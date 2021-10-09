package software.wings.graphql.schema.type.deploymentfreezewindow;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Value;

@Value
@OwnedBy(HarnessTeam.CDC)
public class QLDeploymentFreezeWindowQueryParameters {
  private String id;
  private String name;
}
