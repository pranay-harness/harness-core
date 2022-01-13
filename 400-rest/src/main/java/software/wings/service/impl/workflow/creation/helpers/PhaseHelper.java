/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.workflow.creation.helpers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder.aWorkflowPhase;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase;
import software.wings.service.impl.workflow.WorkflowServiceTemplateHelper;

import com.google.inject.Inject;
import java.util.List;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
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
