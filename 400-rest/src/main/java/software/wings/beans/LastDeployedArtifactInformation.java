package software.wings.beans;

import io.harness.beans.WorkflowType;

import software.wings.beans.artifact.Artifact;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LastDeployedArtifactInformation {
  Artifact artifact;
  Long executionStartTime;
  String envId;
  String executionId;
  String executionEntityId;
  WorkflowType executionEntityType;
  String executionEntityName;
}
