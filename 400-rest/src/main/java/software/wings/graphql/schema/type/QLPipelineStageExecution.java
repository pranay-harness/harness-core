package software.wings.graphql.schema.type;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@OwnedBy(CDC)
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public interface QLPipelineStageExecution {
  String getPipelineStageElementId();
  String getPipelineStageName();
  String getPipelineStepName();
}
