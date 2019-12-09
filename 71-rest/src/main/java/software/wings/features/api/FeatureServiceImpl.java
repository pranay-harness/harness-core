package software.wings.features.api;

import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AccountType;
import software.wings.service.intfc.AccountService;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class FeatureServiceImpl implements FeatureService {
  private final Map<String, Provider<Feature>> featuresByName;
  private final FeatureRestrictions featureRestrictions;
  private final AccountService accountService;

  @Inject
  public FeatureServiceImpl(Map<String, Provider<Feature>> featuresByName, FeatureRestrictions featureRestrictions,
      AccountService accountService) {
    this.featuresByName = featuresByName;
    this.featureRestrictions = featureRestrictions;
    this.accountService = accountService;
  }

  @Override
  public FeatureRestrictions getFeatureRestrictions() {
    return featureRestrictions;
  }

  @Override
  public boolean complyFeatureUsagesWithRestrictions(
      String accountId, Map<String, Map<String, Object>> requiredInfoToComply) {
    String accountType = accountService.getAccountType(accountId).orElse(AccountType.PAID);

    return complyFeatureUsagesWithRestrictions(accountId, accountType, requiredInfoToComply);
  }

  @Override
  public boolean complyFeatureUsagesWithRestrictions(
      String accountId, String targetAccountType, Map<String, Map<String, Object>> requiredInfoToComply) {
    boolean result = true;
    Stopwatch overallWatch = Stopwatch.createStarted();
    for (Provider<Feature> f : featuresByName.values()) {
      Stopwatch featureWatch = Stopwatch.createStarted();
      if (!comply(f.get(), accountId, targetAccountType, requiredInfoToComply)) {
        logger.warn("Failed to comply feature [{}]", f.get().getFeatureName());
        result = false;
      }
      logger.info("Total Time taken to comply feature [{}] : {}", f.get().getFeatureName(),
          featureWatch.elapsed(TimeUnit.SECONDS));
    }
    logger.info("Total Time taken to comply account: {}", overallWatch.elapsed(TimeUnit.SECONDS));

    return result;
  }

  private boolean comply(Feature feature, String accountId, String targetAccountType,
      Map<String, Map<String, Object>> requiredInfoToComply) {
    if (feature instanceof ComplianceByLimitingUsage) {
      return ((ComplianceByLimitingUsage) feature)
          .limitUsageForCompliance(accountId, targetAccountType,
              requiredInfoToComply.getOrDefault(feature.getFeatureName(), Collections.emptyMap()));

    } else if (feature instanceof ComplianceByRemovingUsage) {
      return ((ComplianceByRemovingUsage) feature).removeUsageForCompliance(accountId, targetAccountType);
    }

    return true;
  }

  @Override
  public FeaturesUsageComplianceReport getFeaturesUsageComplianceReport(String accountId) {
    String accountType = accountService.getAccountType(accountId).orElse(AccountType.PAID);

    return getFeaturesUsageComplianceReport(accountId, accountType);
  }

  @Override
  public FeaturesUsageComplianceReport getFeaturesUsageComplianceReport(String accountId, String targetAccountType) {
    Set<FeatureUsageComplianceReport> reports =
        featuresByName.values()
            .parallelStream()
            .filter(feature -> feature.get() instanceof RestrictedFeature)
            .map(feature -> ((RestrictedFeature) feature.get()).getUsageComplianceReport(accountId, targetAccountType))
            .collect(Collectors.toSet());

    return FeaturesUsageComplianceReport.builder()
        .accountId(accountId)
        .targetAccountType(targetAccountType)
        .featureUsageComplianceReports(reports)
        .build();
  }
}
