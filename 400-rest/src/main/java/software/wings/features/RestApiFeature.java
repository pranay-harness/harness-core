/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.features;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.service.intfc.AccountService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collection;
import java.util.Collections;

@OwnedBy(PL)
@Singleton
public class RestApiFeature extends AbstractPremiumFeature {
  public static final String FEATURE_NAME = "REST_API";

  @Inject
  public RestApiFeature(AccountService accountService, FeatureRestrictions featureRestrictions) {
    super(accountService, featureRestrictions);
  }

  @Override
  public boolean isBeingUsed(String accountId) {
    return false;
  }

  @Override
  public Collection<Usage> getDisallowedUsages(String accountId, String targetAccountType) {
    return Collections.emptyList();
  }

  @Override
  public String getFeatureName() {
    return FEATURE_NAME;
  }
}
