package software.wings.service.intfc;

public interface FeatureFlagService {
  boolean isEnabled(String featureName, String accountId);

  void initializeFeatureFlags();
}
