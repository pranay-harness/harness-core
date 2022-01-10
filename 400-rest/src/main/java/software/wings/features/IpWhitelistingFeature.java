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

import software.wings.beans.EntityType;
import software.wings.beans.security.access.Whitelist;
import software.wings.beans.security.access.Whitelist.WhitelistKeys;
import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.ComplianceByRemovingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.WhitelistService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@OwnedBy(PL)
@Singleton
public class IpWhitelistingFeature extends AbstractPremiumFeature implements ComplianceByRemovingUsage {
  public static final String FEATURE_NAME = "IP_WHITELISTING";

  private final WhitelistService whitelistService;

  @Inject
  public IpWhitelistingFeature(
      AccountService accountService, FeatureRestrictions featureRestrictions, WhitelistService whitelistService) {
    super(accountService, featureRestrictions);
    this.whitelistService = whitelistService;
  }

  @Override
  public boolean removeUsageForCompliance(String accountId, String targetAccountType) {
    if (isUsageCompliantWithRestrictions(accountId, targetAccountType)) {
      return true;
    }

    whitelistService.deleteAll(accountId);

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
    return getWhitelistedIPs(accountId).stream().map(IpWhitelistingFeature::toUsage).collect(Collectors.toList());
  }

  private static Usage toUsage(Whitelist ip) {
    return Usage.builder()
        .entityId(ip.getUuid())
        .entityName(ip.getFilter())
        .entityType(EntityType.WHITELISTED_IP.name())
        .build();
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }

  private List<Whitelist> getWhitelistedIPs(String accountId) {
    PageRequest<Whitelist> request = aPageRequest().addFilter(WhitelistKeys.accountId, EQ, accountId).build();

    return whitelistService.list(accountId, request).getResponse();
  }
}
