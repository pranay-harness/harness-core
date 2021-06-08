// package software.wings.service.impl.workflow.creation.argo;
//
// import static io.harness.validation.Validator.notNullCheck;
//
// import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
//
// import io.harness.serializer.MapperUtils;
//
// import software.wings.beans.CanaryOrchestrationWorkflow;
// import software.wings.beans.OrchestrationWorkflow;
// import software.wings.beans.Workflow;
// import software.wings.beans.WorkflowPhase;
// import software.wings.service.impl.workflow.creation.WorkflowCreator;
// import software.wings.service.impl.workflow.creation.helpers.K8RollingWorkflowPhaseHelper;
//
// import com.google.inject.Inject;
// import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
// public class ArgoWorkflowCreator extends WorkflowCreator {
//  private static final String PHASE_NAME = "Rolling";
//  @Inject private K8RollingWorkflowPhaseHelper k8RollingWorkflowPhaseHelper;
//
//  @Override
//  public Workflow createWorkflow(Workflow clientWorkflow) {
//    Workflow workflow = aWorkflow().build();
//    MapperUtils.mapObject(clientWorkflow, workflow);
//
//    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestrationWorkflow();
//
//    CanaryOrchestrationWorkflow canaryOrchestrationWorkflow = (CanaryOrchestrationWorkflow) orchestrationWorkflow;
//    notNullCheck("orchestrationWorkflow", canaryOrchestrationWorkflow);
//    if (k8RollingWorkflowPhaseHelper.isCreationRequired(canaryOrchestrationWorkflow)) {
//      addLinkedPreOrPostDeploymentSteps(canaryOrchestrationWorkflow);
//      addWorkflowPhases(workflow);
//    }
//    return workflow;
//  }
//
//  private void addWorkflowPhases(Workflow workflow) {
//    CanaryOrchestrationWorkflow orchestrationWorkflow =
//        (CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow();
//    WorkflowPhase workflowRollingPhase = k8RollingWorkflowPhaseHelper.getWorkflowPhase(workflow, PHASE_NAME);
//    orchestrationWorkflow.getWorkflowPhases().add(workflowRollingPhase);
//    orchestrationWorkflow.getRollbackWorkflowPhaseIdMap().put(workflowRollingPhase.getUuid(),
//        k8RollingWorkflowPhaseHelper.getRollbackPhaseForWorkflowPhase(workflowRollingPhase));
//
//    for (WorkflowPhase phase : orchestrationWorkflow.getWorkflowPhases()) {
//      attachWorkflowPhase(workflow, phase);
//    }
//  }
//
//  @Override
//  public void attachWorkflowPhase(Workflow workflow, WorkflowPhase workflowPhase) {
//    // Add Drift, Approval and Sync here.
//  }
//}
