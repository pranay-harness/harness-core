package software.wings.generator;

import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.inject.Inject;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import software.wings.beans.Application;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.dl.WingsPersistence;

public class WorkflowGenerator {
  @Inject WingsPersistence wingsPersistence;

  @Inject ApplicationGenerator applicationGenerator;
  @Inject OrchestrationWorkflowGenerator orchestrationWorkflowGenerator;

  public Workflow createWorkflow(long seed, Application application, OrchestrationWorkflow orchestrationWorkflow) {
    if (application == null) {
      application = applicationGenerator.createApplication(seed);
    }

    if (orchestrationWorkflow == null) {
      orchestrationWorkflow = orchestrationWorkflowGenerator.createOrchestrationWorkflow(seed);
    }

    EnhancedRandom random =
        EnhancedRandomBuilder.aNewEnhancedRandomBuilder().seed(seed).scanClasspathForConcreteTypes(true).build();

    Workflow workflow = aWorkflow()
                            .withAppId(application.getAppId())
                            .withName(random.nextObject(String.class))
                            .withDescription(random.nextObject(String.class))
                            .withWorkflowType(WorkflowType.ORCHESTRATION)
                            .withOrchestrationWorkflow(orchestrationWorkflow)
                            .build();

    wingsPersistence.save(workflow);
    return workflow;
  }
}
