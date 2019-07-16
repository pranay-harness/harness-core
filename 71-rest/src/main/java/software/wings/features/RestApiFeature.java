package software.wings.features;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.features.api.AbstractPremiumFeature;
import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.Usage;
import software.wings.service.intfc.AccountService;

import java.util.Collection;
import java.util.Collections;

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
