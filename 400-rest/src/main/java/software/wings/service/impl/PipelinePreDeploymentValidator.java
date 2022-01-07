/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static software.wings.features.utils.WorkflowUtils.hasApprovalSteps;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.data.structure.CollectionUtils;

import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.features.ApprovalFlowFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.features.utils.PipelineUtils;
import software.wings.features.utils.WorkflowUtils;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public class PipelinePreDeploymentValidator {
  public static final String APPROVAL_ERROR_MSG =
      "Pipeline %s is using Professional features (approval integrations, etc.).";

  private final WorkflowPreDeploymentValidator workflowPreDeploymentValidator;
  private final WorkflowService workflowService;

  private final PremiumFeature approvalFlowFeature;

  @Inject
  public PipelinePreDeploymentValidator(WorkflowService workflowService,
      WorkflowPreDeploymentValidator workflowPreDeploymentValidator,
      @Named(ApprovalFlowFeature.FEATURE_NAME) PremiumFeature approvalFlowFeature) {
    this.workflowService = workflowService;
    this.workflowPreDeploymentValidator = workflowPreDeploymentValidator;
    this.approvalFlowFeature = approvalFlowFeature;
  }

  public List<ValidationError> validate(String accountType, Pipeline pipeline) {
    if (!approvalFlowFeature.isAvailable(accountType)) {
      Set<ApprovalStateType> disallowedApprovalSteps =
          WorkflowUtils.toDisallowedApprovalSteps(approvalFlowFeature.getRestrictions(accountType));

      if (PipelineUtils.matches(pipeline, pse -> hasApprovalSteps(pse, disallowedApprovalSteps))) {
        return Lists.newArrayList(ValidationError.builder()
                                      .message(APPROVAL_ERROR_MSG)
                                      .restrictedFeature(ApprovalFlowFeature.FEATURE_NAME)
                                      .build());
      }
    } else {
      return getPipelineWorkflows(pipeline)
          .stream()
          .map(w -> workflowPreDeploymentValidator.validate(accountType, w))
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  private List<Workflow> getPipelineWorkflows(Pipeline pipeline) {
    List<Workflow> workflowList = null;
    PageRequest<Workflow> workflowPageRequest = PipelineUtils.getPipelineWorkflowsPageRequest(pipeline);
    if (workflowPageRequest != null) {
      workflowList = workflowService.listWorkflows(workflowPageRequest).getResponse();
    }

    return CollectionUtils.emptyIfNull(workflowList);
  }
}
