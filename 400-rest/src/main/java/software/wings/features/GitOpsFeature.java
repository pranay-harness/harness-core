/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.PageRequest;
import io.harness.beans.SearchFilter.Operator;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.features.api.AbstractUsageLimitedFeature;
import software.wings.features.api.ComplianceByLimitingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class GitOpsFeature extends AbstractUsageLimitedFeature implements ComplianceByLimitingUsage {
  public static final String FEATURE_NAME = "GIT_OPS";

  private final SettingsService settingsService;

  @Inject
  public GitOpsFeature(
      AccountService accountService, FeatureRestrictions featureRestrictions, SettingsService settingsService) {
    super(accountService, featureRestrictions);

    this.settingsService = settingsService;
  }

  @Override
  public int getMaxUsageAllowed(String accountType) {
    return (int) getRestrictions(accountType).getOrDefault("maxSourceReposAllowed", Integer.MAX_VALUE);
  }

  @Override
  public int getUsage(String accountId) {
    return getCurrentNumberOfSourceRepos(accountId);
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  @Override
  public boolean limitUsageForCompliance(
      String accountId, String targetAccountType, Map<String, Object> requiredInfoToLimitUsage) {
    if (isUsageCompliantWithRestrictions(accountId, targetAccountType)) {
      return true;
    }
    @SuppressWarnings("unchecked")
    List<String> sourceReposToRetain = (List<String>) requiredInfoToLimitUsage.get("sourceReposToRetain");
    if (!isEmpty(sourceReposToRetain)) {
      settingsService.retainSelectedGitConnectorsAndDeleteRest(accountId, sourceReposToRetain);
    }

    return isUsageCompliantWithRestrictions(accountId, targetAccountType);
  }

  private int getCurrentNumberOfSourceRepos(String accountId) {
    PageRequest<SettingAttribute> request =
        aPageRequest()
            .addFilter(SettingAttribute.ACCOUNT_ID_KEY, Operator.EQ, accountId)
            .addFilter(SettingAttributeKeys.value_type, Operator.EQ, SettingVariableTypes.GIT)
            .build();

    return settingsService.list(request, null, null).getResponse().size();
  }
}
