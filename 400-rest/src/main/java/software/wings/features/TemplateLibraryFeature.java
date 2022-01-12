/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features;

import static software.wings.features.utils.ServiceUtils.getServicesWithTemplateLibrary;
import static software.wings.features.utils.WorkflowUtils.getWorkflowsWithTemplateLibrary;

import io.harness.beans.PageResponse;

import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.ComplianceByRefactoringUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.features.utils.ServiceUtils;
import software.wings.features.utils.WorkflowUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Singleton
public class TemplateLibraryFeature extends AbstractPremiumFeature implements ComplianceByRefactoringUsage {
  public static final String FEATURE_NAME = "TEMPLATE_LIBRARY";

  private final ServiceResourceService serviceResourceService;
  private final WorkflowService workflowService;

  @Inject
  public TemplateLibraryFeature(WorkflowService workflowService, AccountService accountService,
      FeatureRestrictions featureRestrictions, ServiceResourceService serviceResourceService) {
    super(accountService, featureRestrictions);

    this.serviceResourceService = serviceResourceService;
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

  private Collection<Usage> getUsages(String accountId) {
    Collection<Usage> usages = new ArrayList<>();

    usages.addAll(getWorkflowsWithTemplateLibrary(getWorkflows(accountId))
                      .stream()
                      .map(WorkflowUtils::toUsage)
                      .collect(Collectors.toList()));

    usages.addAll(getServicesWithTemplateLibrary(getServicesForAccount(accountId))
                      .stream()
                      .map(ServiceUtils::toUsage)
                      .collect(Collectors.toList()));

    return usages;
  }

  private List<Workflow> getWorkflows(String accountId) {
    return workflowService.listWorkflows(WorkflowUtils.getWorkflowsPageRequest(accountId)).getResponse();
  }

  private PageResponse<Service> getServicesForAccount(@NotNull String accountId) {
    return serviceResourceService.list(ServiceUtils.getServicesPageRequest(accountId, null), false, true, false, null);
  }
}
