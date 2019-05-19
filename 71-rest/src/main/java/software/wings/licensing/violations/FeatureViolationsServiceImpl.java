package software.wings.licensing.violations;

import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import software.wings.beans.AccountType;
import software.wings.beans.FeatureViolation;
import software.wings.service.intfc.AccountService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Singleton
public class FeatureViolationsServiceImpl implements FeatureViolationsService {
  @Inject private Injector injector;
  @Inject private AccountService accountService;

  @Override
  public List<FeatureViolation> getViolations(String accountId, String targetAccountType) {
    Optional<String> currentAccountType = accountService.getAccountType(accountId);
    if (!currentAccountType.isPresent() || currentAccountType.get().equals(targetAccountType)
        || !targetAccountType.equals(AccountType.COMMUNITY)) {
      return Collections.emptyList();
    }

    return Stream.of(RestrictedFeature.values())
        .parallel()
        .map(restrictedFeature -> getViolations(restrictedFeature, accountId, targetAccountType))
        .filter(EmptyPredicate::isNotEmpty)
        .flatMap(Collection::stream)
        .collect(toList());
  }

  private List<FeatureViolation> getViolations(
      RestrictedFeature restrictedFeature, String accountId, String targetAccountType) {
    return injector.getInstance(restrictedFeature.getViolationsCheckerClass()).check(accountId, targetAccountType);
  }

  @Override
  public List<RestrictedFeature> getRestrictedFeatures(String accountId) {
    return Arrays.asList(RestrictedFeature.values());
  }
}
