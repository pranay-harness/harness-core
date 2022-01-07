/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.features.utils.WorkflowUtils.SERVICENOW_USAGE_PREDICATE;
import static software.wings.features.utils.WorkflowUtils.getMatchingWorkflows;

import io.harness.annotations.dev.OwnedBy;

import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.features.utils.WorkflowUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(PL)
@Singleton
public class ServiceNowNotificationFeature extends AbstractNotificationFeature {
  public static final String FEATURE_NAME = "SERVICENOW_NOTIFICATION";

  @Inject
  public ServiceNowNotificationFeature(
      AccountService accountService, FeatureRestrictions featureRestrictions, WorkflowService workflowService) {
    super(accountService, featureRestrictions, workflowService);
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  // gets usages of Service Now state under workflows
  @Override
  protected List<Usage> getUsages(String accountId) {
    return getMatchingWorkflows(getAllWorkflowsByAccountId(accountId), SERVICENOW_USAGE_PREDICATE)
        .stream()
        .map(WorkflowUtils::toUsage)
        .collect(Collectors.toList());
  }
}
