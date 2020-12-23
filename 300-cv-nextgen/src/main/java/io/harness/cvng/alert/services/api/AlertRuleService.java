package io.harness.cvng.alert.services.api;

import io.harness.cvng.alert.beans.AlertRuleDTO;
import io.harness.cvng.alert.util.ActivityType;
import io.harness.cvng.alert.util.VerificationStatus;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.ng.beans.PageResponse;

import java.time.Instant;
import java.util.List;

public interface AlertRuleService {
  PageResponse<AlertRuleDTO> listAlertRules(String accountId, String orgIdentifier, String projectIdentifier,
      int offset, int pageSize, List<AlertRuleDTO> alertRuleDTO);

  AlertRuleDTO getAlertRuleDTO(String accountId, String orgIdentifier, String projectIdentifier, String identifier);

  AlertRuleDTO createAlertRule(AlertRuleDTO alertRuleDTO);

  void updateAlertRule(String accountId, String orgIdentifier, String projectIdentifier, AlertRuleDTO alertRuleDTO);

  void deleteAlertRule(String accountId, String orgIdentifier, String projectIdentifier, String identifier);

  void processDeploymentVerification(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String envIdentifier, ActivityType activityType, VerificationStatus verificationStatus);

  List<ActivityType> getActivityTypes(String accountId, String orgIdentifier, String projectIdentifier);

  void processRiskScore(String accountId, String orgIdentifier, String projectIdentifier, String serviceIdentifier,
      String envIdentifier, CVMonitoringCategory category, Instant timeStamp, double riskScore);
}
