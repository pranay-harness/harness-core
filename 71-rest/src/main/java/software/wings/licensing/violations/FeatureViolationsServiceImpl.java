package software.wings.licensing.violations;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import software.wings.beans.AccountType;
import software.wings.beans.FeatureViolation;
import software.wings.service.intfc.AccountService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Singleton
public class FeatureViolationsServiceImpl implements FeatureViolationsService {
  @Inject private Injector injector;
  @Inject private AccountService accountService;

  @Override
  public List<FeatureViolation> getViolations(String accountId, String targetAccountType) {
    List<FeatureViolation> featureViolations = new ArrayList<>();

    Optional<String> currentAccountType = accountService.getAccountType(accountId);
    if (!currentAccountType.isPresent() || currentAccountType.get().equals(targetAccountType)
        || !targetAccountType.equals(AccountType.COMMUNITY)) {
      return Collections.emptyList();
    }

    for (RestrictedFeature restrictedFeature : RestrictedFeature.values()) {
      FeatureViolationChecker violationsChecker = injector.getInstance(restrictedFeature.getViolationsCheckerClass());
      featureViolations.addAll(violationsChecker.check(accountId, targetAccountType));
    }

    return featureViolations;
  }

  @Override
  public List<RestrictedFeature> getRestrictedFeatures(String accountId) {
    return Arrays.asList(RestrictedFeature.values());
  }
}
