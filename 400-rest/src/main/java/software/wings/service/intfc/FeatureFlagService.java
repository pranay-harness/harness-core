package software.wings.service.intfc;

public interface FeatureFlagService {
  boolean isFeatureFlagEnabled(String name, String accountId);
}
