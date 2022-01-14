package software.wings.features.api;

public interface ComplianceByRemovingUsage {
  boolean removeUsageForCompliance(String accountId, String targetAccountType);
}
