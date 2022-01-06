/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;

import software.wings.beans.ApiKeyEntry;
import software.wings.beans.ApiKeyEntry.ApiKeyEntryKeys;
import software.wings.beans.EntityType;
import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.ComplianceByRemovingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.ApiKeyService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(PL)
@Singleton
public class ApiKeysFeature extends AbstractPremiumFeature implements ComplianceByRemovingUsage {
  public static final String FEATURE_NAME = "API_KEYS";

  private final ApiKeyService apiKeyService;

  @Inject
  public ApiKeysFeature(
      AccountService accountService, FeatureRestrictions featureRestrictions, ApiKeyService apiKeyService) {
    super(accountService, featureRestrictions);
    this.apiKeyService = apiKeyService;
  }

  @Override
  public boolean removeUsageForCompliance(String accountId, String targetAccountType) {
    if (isUsageCompliantWithRestrictions(accountId, targetAccountType)) {
      return true;
    }
    apiKeyService.deleteByAccountId(accountId);

    return isUsageCompliantWithRestrictions(accountId, targetAccountType);
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

  private Collection<Usage> getUsages(String accountId) {
    return getApiKeys(accountId).stream().map(ApiKeysFeature::toUsage).collect(Collectors.toList());
  }

  private static Usage toUsage(ApiKeyEntry apiKey) {
    return Usage.builder()
        .entityId(apiKey.getUuid())
        .entityName(apiKey.getName())
        .entityType(EntityType.API_KEY.name())
        .build();
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  private List<ApiKeyEntry> getApiKeys(String accountId) {
    PageRequest<ApiKeyEntry> request = aPageRequest().addFilter(ApiKeyEntryKeys.accountId, EQ, accountId).build();
    return apiKeyService.list(request, accountId, false, false).getResponse();
  }
}
