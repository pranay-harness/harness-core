package software.wings.features.api;

public interface UsageLimitedFeature extends RestrictedFeature {
  int getMaxUsageAllowedForAccount(String accountId);

  int getMaxUsageAllowed(String accountType);

  int getUsage(String accountId);
}
