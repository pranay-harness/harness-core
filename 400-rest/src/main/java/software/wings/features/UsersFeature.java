/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;

import software.wings.features.api.AbstractUsageLimitedFeature;
import software.wings.features.api.ComplianceByLimitingUsage;
import software.wings.features.api.FeatureRestrictions;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;

@OwnedBy(PL)
@Singleton
public class UsersFeature extends AbstractUsageLimitedFeature implements ComplianceByLimitingUsage {
  public static final String FEATURE_NAME = "USERS";
  private static final int DEFAULT_MAX_USERS_ALLOWED = 1500;

  private final UserService userService;

  @Inject
  public UsersFeature(AccountService accountService, FeatureRestrictions featureRestrictions, UserService userService) {
    super(accountService, featureRestrictions);
    this.userService = userService;
  }

  @Override
  public int getMaxUsageAllowed(String accountType) {
    return (int) getRestrictions(accountType).getOrDefault("maxUsersAllowed", DEFAULT_MAX_USERS_ALLOWED);
  }

  @Override
  public int getUsage(String accountId) {
    return userService.getUsersOfAccount(accountId).size();
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
    List<String> usersToRetain = (List<String>) requiredInfoToLimitUsage.get("usersToRetain");
    if (!isEmpty(usersToRetain)) {
      userService.deleteUsersByEmailAddress(accountId, usersToRetain);
    }

    return isUsageCompliantWithRestrictions(accountId, targetAccountType);
  }
}
