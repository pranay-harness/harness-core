package software.wings.features.api;

import java.util.Map;

public interface ComplianceByLimitingUsage {
  boolean limitUsageForCompliance(
      String accountId, String targetAccountType, Map<String, Object> requiredInfoToLimitUsage);
}
