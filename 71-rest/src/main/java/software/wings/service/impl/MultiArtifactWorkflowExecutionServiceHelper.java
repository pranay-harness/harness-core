package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.service.intfc.ArtifactService;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class MultiArtifactWorkflowExecutionServiceHelper {
  @Inject private ArtifactService artifactService;

  public List<Artifact> filterArtifactsForExecution(
      List<ArtifactVariable> artifactVariables, WorkflowExecution workflowExecution, String accountId) {
    List<Artifact> artifacts = new ArrayList<>();
    if (isNotEmpty(artifactVariables)) {
      for (ArtifactVariable variable : artifactVariables) {
        if (variable.getEntityType() == null) {
          throw new InvalidRequestException(
              "Artifact variable [" + variable.getName() + "] does not have an associated entity type", USER);
        }
        if (variable.getEntityId() == null) {
          throw new InvalidRequestException(
              "Artifact variable [" + variable.getName() + "] does not have an associated entity id", USER);
        }
        switch (variable.getEntityType()) {
          case WORKFLOW:
            if (isNotEmpty(workflowExecution.getWorkflowIds())
                && workflowExecution.getWorkflowIds().contains(variable.getEntityId())) {
              findArtifactForArtifactVariable(accountId, artifacts, variable);
            }
            break;
          case ENVIRONMENT:
            if (isNotEmpty(workflowExecution.getEnvIds())
                && workflowExecution.getEnvIds().contains(variable.getEntityId())) {
              findArtifactForArtifactVariable(accountId, artifacts, variable);
            }
            break;
          case SERVICE:
            if (isNotEmpty(workflowExecution.getServiceIds())
                && workflowExecution.getServiceIds().contains(variable.getEntityId())) {
              findArtifactForArtifactVariable(accountId, artifacts, variable);
            }
            break;
          default:
            throw new InvalidRequestException(format("Unexpected value: %s", variable.getEntityType()), USER);
        }
      }
    }
    return artifacts;
  }

  private void findArtifactForArtifactVariable(String accountId, List<Artifact> artifacts, ArtifactVariable variable) {
    Artifact artifact = artifactService.get(accountId, variable.getValue());
    if (artifact == null) {
      throw new InvalidRequestException(format("Unable to get artifact for artifact variable: [%s], value: [%s]",
                                            variable.getName(), variable.getValue()),
          USER);
    }
    artifacts.add(artifact);
  }
}
