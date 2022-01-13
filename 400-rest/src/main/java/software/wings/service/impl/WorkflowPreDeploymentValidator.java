/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.features.utils.ServiceUtils.getServicesPageRequest;
import static software.wings.features.utils.ServiceUtils.getServicesWithTemplateLibrary;
import static software.wings.features.utils.WorkflowUtils.FLOW_CONTROL_USAGE_PREDICATE;
import static software.wings.features.utils.WorkflowUtils.TEMPLATE_USAGE_PREDICATE;
import static software.wings.features.utils.WorkflowUtils.hasApprovalSteps;
import static software.wings.features.utils.WorkflowUtils.matches;
import static software.wings.features.utils.WorkflowUtils.toDisallowedApprovalSteps;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.data.structure.CollectionUtils;

import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.features.ApprovalFlowFeature;
import software.wings.features.FlowControlFeature;
import software.wings.features.TemplateLibraryFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Singleton
public class WorkflowPreDeploymentValidator {
  private static final String WORKFLOW_RESTRICTED_FEATURE_ERROR_MSG =
      "Workflow %s is using Professional features (Flow controls, approval integrations, templates, etc.).";

  private static final String WORKFLOW_SERVICE_RESTRICTED_FEATURE_ERROR_MSG =
      "A Service in Workflow %s is using Professional features (Flow controls, approval integrations, templates, etc.).";

  private final ServiceResourceService serviceResourceService;

  private final PremiumFeature templateLibraryFeature;
  private final PremiumFeature flowControlFeature;
  private final PremiumFeature approvalFlowFeature;

  @Inject
  public WorkflowPreDeploymentValidator(ServiceResourceService serviceResourceService,
      @Named(TemplateLibraryFeature.FEATURE_NAME) PremiumFeature templateLibraryFeature,
      @Named(FlowControlFeature.FEATURE_NAME) PremiumFeature flowControlFeature,
      @Named(ApprovalFlowFeature.FEATURE_NAME) PremiumFeature approvalFlowFeature) {
    this.serviceResourceService = serviceResourceService;
    this.templateLibraryFeature = templateLibraryFeature;
    this.flowControlFeature = flowControlFeature;
    this.approvalFlowFeature = approvalFlowFeature;
  }

  public List<ValidationError> validate(String accountType, @NotNull Workflow workflow) {
    List<ValidationError> validationErrorList = new ArrayList<>();

    if (!templateLibraryFeature.isAvailable(accountType) && TEMPLATE_USAGE_PREDICATE.test(workflow)) {
      validationErrorList.add(getValidationError(
          getWorkflowRestrictedFeatureErrorMsg(workflow.getName()), TemplateLibraryFeature.FEATURE_NAME));
    }

    if (!flowControlFeature.isAvailable(accountType) && matches(workflow, FLOW_CONTROL_USAGE_PREDICATE)) {
      validationErrorList.add(getValidationError(
          getWorkflowRestrictedFeatureErrorMsg(workflow.getName()), FlowControlFeature.FEATURE_NAME));
    }

    Set<ApprovalStateType> disallowedApprovalSteps =
        toDisallowedApprovalSteps(approvalFlowFeature.getRestrictions(accountType));

    if (!approvalFlowFeature.isAvailable(accountType)
        && matches(workflow, gn -> hasApprovalSteps(gn, disallowedApprovalSteps))) {
      validationErrorList.add(getValidationError(
          getWorkflowRestrictedFeatureErrorMsg(workflow.getName()), ApprovalFlowFeature.FEATURE_NAME));
    }

    if (!templateLibraryFeature.isAvailable(accountType)
        && isNotEmpty(getServicesWithTemplateLibrary(
            getServicesForAccount(workflow.getAccountId(), workflow.getOrchestrationWorkflow().getServiceIds())))) {
      validationErrorList.add(getValidationError(
          getWorkflowServiceRestrictedFeatureErrorMsg(workflow.getName()), TemplateLibraryFeature.FEATURE_NAME));
    }

    return CollectionUtils.emptyIfNull(validationErrorList);
  }

  private ValidationError getValidationError(String workflowRestrictedFeatureErrorMsg, String restrictedFeature) {
    return ValidationError.builder()
        .message(workflowRestrictedFeatureErrorMsg)
        .restrictedFeatures(Collections.singletonList(restrictedFeature))
        .build();
  }

  public static String getWorkflowRestrictedFeatureErrorMsg(String workflowName) {
    return String.format(WORKFLOW_RESTRICTED_FEATURE_ERROR_MSG, workflowName);
  }

  public static String getWorkflowServiceRestrictedFeatureErrorMsg(String workflowName) {
    return String.format(WORKFLOW_SERVICE_RESTRICTED_FEATURE_ERROR_MSG, workflowName);
  }

  private PageResponse<Service> getServicesForAccount(@NotNull String accountId, List<String> serviceIdList) {
    return serviceResourceService.list(getServicesPageRequest(accountId, serviceIdList), false, true, false, null);
  }
}
