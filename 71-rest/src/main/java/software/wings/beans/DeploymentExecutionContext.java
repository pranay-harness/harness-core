package software.wings.beans;

import software.wings.beans.artifact.Artifact;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;

public interface DeploymentExecutionContext extends ExecutionContext {
  List<Artifact> getArtifacts();

  Artifact getArtifactForService(String serviceId);

  Map<String, Artifact> getArtifactsForService(String serviceId);
}
