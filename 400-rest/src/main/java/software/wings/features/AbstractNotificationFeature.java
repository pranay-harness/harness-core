/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.features.utils.WorkflowUtils.getWorkflowsPageRequest;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.Workflow;
import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.ComplianceByRefactoringUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@OwnedBy(PL)
@Singleton
public abstract class AbstractNotificationFeature
    extends AbstractPremiumFeature implements ComplianceByRefactoringUsage {
  private final WorkflowService workflowService;

  @Inject
  public AbstractNotificationFeature(
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

  protected abstract Collection<Usage> getUsages(String accountId);

  List<Workflow> getAllWorkflowsByAccountId(String accountId) {
    return workflowService.listWorkflows(getWorkflowsPageRequest(accountId)).getResponse();
  }
}
