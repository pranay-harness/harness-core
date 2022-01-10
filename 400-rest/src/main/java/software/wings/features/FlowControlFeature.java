/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features;

import static software.wings.features.utils.WorkflowUtils.FLOW_CONTROL_USAGE_PREDICATE;
import static software.wings.features.utils.WorkflowUtils.getWorkflowsPageRequest;

import static java.util.stream.Collectors.toList;

import software.wings.beans.Workflow;
import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.ComplianceByRefactoringUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.features.utils.WorkflowUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Singleton
public class FlowControlFeature extends AbstractPremiumFeature implements ComplianceByRefactoringUsage {
  public static final String FEATURE_NAME = "FLOW_CONTROL";

  private WorkflowService workflowService;

  @Inject
  public FlowControlFeature(
      AccountService accountService, FeatureRestrictions featureRestrictions, WorkflowService workflowService) {
    super(accountService, featureRestrictions);
    this.workflowService = workflowService;
  }

  @Override
  public boolean isBeingUsed(String accountId) {
    return !getUsages(accountId).isEmpty();
  }

  @Override
  public Collection<Usage> getDisallowedUsages(String accountId, String targetAccountType) {
    if (isAvailable(targetAccountType)) {
      return Collections.emptyList();
    }

    return getUsages(accountId);
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  private List<Usage> getUsages(String accountId) {
    return getWorkflowsWithFlowControl(accountId).stream().map(WorkflowUtils::toUsage).collect(toList());
  }

  private List<Workflow> getWorkflowsWithFlowControl(String accountId) {
    return WorkflowUtils.getMatchingWorkflows(getAllWorkflowsByAccountId(accountId), FLOW_CONTROL_USAGE_PREDICATE);
  }

  private List<Workflow> getAllWorkflowsByAccountId(String accountId) {
    return workflowService.listWorkflows(getWorkflowsPageRequest(accountId)).getResponse();
  }
}
