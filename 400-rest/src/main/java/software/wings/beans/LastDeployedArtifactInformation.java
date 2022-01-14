package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.WorkflowType;

import software.wings.beans.artifact.Artifact;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class LastDeployedArtifactInformation {
  Artifact artifact;
  Long executionStartTime;
  String envId;
  String executionId;
  String executionEntityId;
  WorkflowType executionEntityType;
  String executionEntityName;
}
