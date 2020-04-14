package software.wings.beans;

import software.wings.api.artifact.ServiceArtifactVariableElement;
import software.wings.beans.artifact.Artifact;
import software.wings.sm.ExecutionContext;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;
import java.util.Map;

public interface DeploymentExecutionContext extends ExecutionContext {
  WorkflowStandardParams fetchWorkflowStandardParamsFromContext();

  List<Artifact> getArtifacts();

  List<ServiceArtifactVariableElement> getArtifactVariableElements();

  Artifact getArtifactForService(String serviceId);

  Map<String, Artifact> getArtifactsForService(String serviceId);

  Artifact getDefaultArtifactForService(String serviceId);
}
