package software.wings.service.impl.workflow.creation.helpers;

import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;

import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;

import com.google.inject.Inject;
import java.util.List;

public abstract class PhaseHelper {
  @Inject private WorkflowPhaseHelper workflowPhaseHelper;
  @Inject private WorkflowServiceTemplateHelper workflowServiceTemplateHelper;

  public WorkflowPhase getWorkflowPhase(Workflow workflow, String phaseName) {
    WorkflowPhase workflowPhase = aWorkflowPhase()
                                      .name(phaseName)
                                      .infraMappingId(workflow.getInfraMappingId())
                                      .infraDefinitionId(workflow.getInfraDefinitionId())
                                      .serviceId(workflow.getServiceId())
                                      .build();
    workflowPhaseHelper.setCloudProvider(workflow.getAppId(), workflowPhase);
    List<PhaseStep> phaseSteps = workflowPhase.getPhaseSteps();
    phaseSteps.addAll(getWorkflowPhaseSteps());
    workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(workflowPhase);
    return workflowPhase;
  }

  public WorkflowPhase getRollbackPhaseForWorkflowPhase(WorkflowPhase workflowPhase) {
    WorkflowPhase rollbackPhase = workflowPhaseHelper.createRollbackPhase(workflowPhase);
    List<PhaseStep> rollbackPhaseSteps = rollbackPhase.getPhaseSteps();
    rollbackPhaseSteps.addAll(getRollbackPhaseSteps());
    workflowServiceTemplateHelper.addLinkedWorkflowPhaseTemplate(rollbackPhase);
    return rollbackPhase;
  }

  public abstract List<PhaseStep> getWorkflowPhaseSteps();

  public abstract List<PhaseStep> getRollbackPhaseSteps();
}
