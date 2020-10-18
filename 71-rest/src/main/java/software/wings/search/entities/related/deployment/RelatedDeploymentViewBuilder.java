package software.wings.search.entities.related.deployment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.google.inject.Singleton;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.WorkflowExecution;

import java.util.Map;

@OwnedBy(PL)
@Singleton
public class RelatedDeploymentViewBuilder {
  public Map<String, Object> getDeploymentRelatedEntityViewMap(WorkflowExecution workflowExecution) {
    RelatedDeploymentView relatedDeploymentView = new RelatedDeploymentView(workflowExecution.getUuid(),
        workflowExecution.getStatus(), workflowExecution.getName(), workflowExecution.getCreatedAt(),
        workflowExecution.getPipelineExecutionId(), workflowExecution.getWorkflowId(),
        workflowExecution.getWorkflowType().name(), workflowExecution.getEnvId());
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(relatedDeploymentView, new TypeReference<Object>() {});
  }
}
