package software.wings.generator;

import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.BasicOrchestrationWorkflow;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.service.intfc.WorkflowService;

@Singleton
public class WorkflowGenerator {
  @Inject WorkflowService workflowService;

  @Inject ApplicationGenerator applicationGenerator;
  @Inject OrchestrationWorkflowGenerator orchestrationWorkflowGenerator;

  public Workflow createWorkflow(long seed, Workflow workflow) {
    EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).build();

    WorkflowBuilder builder = aWorkflow();

    if (workflow != null && workflow.getAppId() != null) {
      builder.withAppId(workflow.getAppId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (workflow != null && workflow.getEnvId() != null) {
      builder.withEnvId(workflow.getEnvId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (workflow != null && workflow.getName() != null) {
      builder.withName(workflow.getName());
    } else {
      throw new UnsupportedOperationException();
    }

    if (workflow != null && workflow.getWorkflowType() != null) {
      builder.withWorkflowType(workflow.getWorkflowType());
    } else {
      throw new UnsupportedOperationException();
    }

    if (workflow != null && workflow.getOrchestrationWorkflow() != null) {
      builder.withOrchestrationWorkflow(workflow.getOrchestrationWorkflow());
    } else {
      throw new UnsupportedOperationException();
    }

    if (workflow != null && workflow.getServiceId() != null) {
      builder.withServiceId(workflow.getServiceId());
    } else {
      throw new UnsupportedOperationException();
    }

    if (workflow != null && workflow.getInfraMappingId() != null) {
      builder.withInfraMappingId(workflow.getInfraMappingId());
    } else {
      throw new UnsupportedOperationException();
    }

    return workflowService.createWorkflow(builder.build());
  }

  @Value
  @Builder
  public static class PostProcessInfo {
    private Integer selectNodeCount;
  }

  public Workflow postProcess(Workflow workflow, PostProcessInfo params) {
    if (params.getSelectNodeCount() != null) {
      if (workflow.getOrchestrationWorkflow() instanceof BasicOrchestrationWorkflow) {
        final GraphNode selectNodes = ((BasicOrchestrationWorkflow) workflow.getOrchestrationWorkflow())
                                          .getGraph()
                                          .getSubworkflows()
                                          .entrySet()
                                          .stream()
                                          .filter(entry -> "Provision Nodes".equals(entry.getValue().getGraphName()))
                                          .findFirst()
                                          .get()
                                          .getValue()
                                          .getNodes()
                                          .get(0);

        selectNodes.getProperties().put("instanceCount", params.getSelectNodeCount());
      }
    }

    return workflowService.updateWorkflow(workflow);
  }
}
