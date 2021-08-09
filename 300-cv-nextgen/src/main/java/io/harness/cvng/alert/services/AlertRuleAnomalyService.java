package io.harness.cvng.alert.services;

import io.harness.cvng.alert.entities.AlertRuleAnomaly;
import io.harness.cvng.beans.CVMonitoringCategory;

public interface AlertRuleAnomalyService {
  AlertRuleAnomaly openAnomaly(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String envIdentifier, CVMonitoringCategory category);

  void closeAnomaly(String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier,
      String envIdentifier, CVMonitoringCategory category);

  void updateLastNotificationSentAt(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String envIdentifier, CVMonitoringCategory category);
}