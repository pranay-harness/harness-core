package software.wings.generator;

import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowBuilder;
import software.wings.service.intfc.WorkflowService;

@Singleton
public class WorkflowGenerator {
  @Inject WorkflowService workflowService;

  @Inject ApplicationGenerator applicationGenerator;
  @Inject OrchestrationWorkflowGenerator orchestrationWorkflowGenerator;

  public Workflow createWorkflow(long seed, Workflow workflow) {
    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

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
}
