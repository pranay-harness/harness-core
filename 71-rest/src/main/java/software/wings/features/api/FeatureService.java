package software.wings.features.api;

import java.util.Map;

public interface FeatureService {
  FeatureRestrictions getFeatureRestrictions();

  boolean complyFeatureUsagesWithRestrictions(String accountId, Map<String, Map<String, Object>> requiredInfoToComply);

  boolean complyFeatureUsagesWithRestrictions(
      String accountId, String targetAccountType, Map<String, Map<String, Object>> requiredInfoToComply);

  FeaturesUsageComplianceReport getFeaturesUsageComplianceReport(String accountId);

  FeaturesUsageComplianceReport getFeaturesUsageComplianceReport(String accountId, String targetAccountType);
}
