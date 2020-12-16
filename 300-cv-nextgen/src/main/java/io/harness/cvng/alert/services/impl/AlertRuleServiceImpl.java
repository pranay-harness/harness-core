package io.harness.cvng.alert.services.impl;

import static io.harness.cvng.alert.entities.AlertRule.convertFromDTO;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.Team;
import io.harness.cvng.alert.beans.AlertRuleDTO;
import io.harness.cvng.alert.beans.AlertRuleDTO.NotificationMethod;
import io.harness.cvng.alert.entities.AlertRule;
import io.harness.cvng.alert.entities.AlertRule.AlertRuleKeys;
import io.harness.cvng.alert.services.api.AlertRuleService;
import io.harness.cvng.alert.util.ActivityType;
import io.harness.cvng.alert.util.VerificationStatus;
import io.harness.ng.beans.PageResponse;
import io.harness.notification.channeldetails.PagerDutyChannel;
import io.harness.notification.channeldetails.SlackChannel;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.persistence.HPersistence;
import io.harness.utils.PageUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
@Slf4j
public class AlertRuleServiceImpl implements AlertRuleService {
  @Inject private HPersistence hPersistence;
  @Inject private NotificationClient notificationClient;

  @Override
  public PageResponse<AlertRuleDTO> listAlertRules(String accountId, String orgIdentifier, String projectIdentifier,
      int offset, int pageSize, List<AlertRuleDTO> alertRuleDTO) {
    List<AlertRuleDTO> alertRules = list(accountId, projectIdentifier, orgIdentifier);

    return PageUtils.offsetAndLimit(alertRules, offset, pageSize);
  }

  @Override
  public AlertRuleDTO getAlertRuleDTO(
      String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    AlertRule alertRule = getAlertRule(accountId, orgIdentifier, projectIdentifier, identifier);
    if (alertRule == null) {
      return null;
    }
    return alertRule.convertToDTO();
  }

  @Override
  public AlertRuleDTO createAlertRule(AlertRuleDTO alertRuleDTO) {
    AlertRule alertRule = convertFromDTO(alertRuleDTO);
    hPersistence.save(alertRule);

    return alertRuleDTO;
  }

  @Override
  public void updateAlertRule(
      String accountId, String orgIdentifier, String projectIdentifier, AlertRuleDTO alertRuleDTO) {
    AlertRule alertRule = convertFromDTO(alertRuleDTO);

    Preconditions.checkState(isNotEmpty(alertRuleDTO.getUuid()), "uuid must be present ");

    hPersistence.save(alertRule);
  }

  @Override
  public void deleteAlertRule(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    hPersistence.delete(hPersistence.createQuery(AlertRule.class)
                            .field(AlertRuleKeys.accountId)
                            .equal(accountId)
                            .field(AlertRuleKeys.orgIdentifier)
                            .equal(orgIdentifier)
                            .field(AlertRuleKeys.projectIdentifier)
                            .equal(projectIdentifier)
                            .field(AlertRuleKeys.identifier)
                            .equal(identifier)
                            .get());
  }

  @Override
  public void processRiskScore(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String envIdentifier, double riskScore) {
    double riskValue = BigDecimal.valueOf(riskScore * 100).setScale(2, RoundingMode.HALF_UP).doubleValue();
    List<AlertRule> alertRules = hPersistence.createQuery(AlertRule.class)
                                     .filter(AlertRuleKeys.accountId, accountId)
                                     .filter(AlertRuleKeys.orgIdentifier, orgIdentifier)
                                     .filter(AlertRuleKeys.projectIdentifier, projectIdentifier)
                                     .filter(AlertRuleKeys.enabledRisk, true)
                                     .field(AlertRuleKeys.threshold)
                                     .lessThanOrEq(riskValue)
                                     .asList();

    if (isNotEmpty(alertRules)) {
      alertRules.stream()
          .filter(alertRule
              -> (alertRule.getAlertCondition().isAllServices()
                     || alertRule.getAlertCondition().getServices().contains(serviceIdentifier))
                  && (alertRule.getAlertCondition().isAllEnvironments()
                      || alertRule.getAlertCondition().getEnvironments().contains(envIdentifier)))
          .forEach(rule -> {
            String alertMessage = "Risk detected in project " + projectIdentifier + " for service " + serviceIdentifier
                + " and environment " + envIdentifier + ".\nThe current risk score " + riskValue
                + " is greater than the threshold " + rule.getAlertCondition().getNotify().getThreshold()
                + " defined in rule " + rule.getName();
            notifyChannel(accountId, rule.getNotificationMethod(), alertMessage);
          });
    }
  }

  @Override
  public void processDeploymentVerification(String accountId, String orgIdentifier, String projectIdentifier,
      String serviceIdentifier, String envIdentifier, ActivityType activityType,
      VerificationStatus verificationStatus) {
    List<AlertRule> alertRules = hPersistence.createQuery(AlertRule.class)
                                     .filter(AlertRuleKeys.accountId, accountId)
                                     .filter(AlertRuleKeys.orgIdentifier, orgIdentifier)
                                     .filter(AlertRuleKeys.projectIdentifier, projectIdentifier)
                                     .filter(AlertRuleKeys.enabledVerifications, true)
                                     .asList();

    if (isNotEmpty(alertRules)) {
      alertRules.stream()
          .filter(alertRule
              -> (alertRule.getAlertCondition().isAllServices()
                     || alertRule.getAlertCondition().getServices().contains(serviceIdentifier))
                  && (alertRule.getAlertCondition().isAllEnvironments()
                      || alertRule.getAlertCondition().getEnvironments().contains(envIdentifier))
                  && (alertRule.getAlertCondition().getVerificationsNotify().isAllVerificationStatuses()
                      || alertRule.getAlertCondition().getVerificationsNotify().getVerificationStatuses().contains(
                          verificationStatus))
                  && (alertRule.getAlertCondition().getVerificationsNotify().isAllActivityTpe()
                      || alertRule.getAlertCondition().getVerificationsNotify().getActivityTypes().contains(
                          activityType)))
          .forEach(rule -> {
            String alertMessage = "Verification finished in project " + projectIdentifier + " for service "
                + serviceIdentifier + " and environment " + envIdentifier;
            notifyChannel(rule.getAccountId(), rule.getNotificationMethod(), alertMessage);
          });
    }
  }

  @VisibleForTesting
  public void notifyChannel(String accountId, NotificationMethod notificationMethod, String alertMessage) {
    switch (notificationMethod.getNotificationSettingType()) {
      case Slack:
        slackNotification(accountId, notificationMethod, alertMessage);
        break;
      case PagerDuty:
        pagerDutyNotification(accountId, notificationMethod);
        break;
      default:
        throw new IllegalArgumentException(
            "This is not a valid Notification Type: " + notificationMethod.getNotificationSettingType());
    }
  }

  private void pagerDutyNotification(String accountId, NotificationMethod notificationMethod) {
    notificationClient.sendNotificationAsync(
        PagerDutyChannel.builder()
            .accountId(accountId)
            .pagerDutyIntegrationKeys(Collections.singletonList(notificationMethod.getPagerDutyKey()))
            .team(Team.CV)
            .templateId("pd_test")
            .templateData(Collections.emptyMap())
            .userGroupIds(Collections.emptyList())
            .build());
  }

  private void slackNotification(String accountId, NotificationMethod notificationMethod, String alertMessage) {
    notificationClient.sendNotificationAsync(
        SlackChannel.builder()
            .accountId(accountId)
            .slackWebHookURLs(Collections.singletonList(notificationMethod.getSlackWebhook()))
            .team(Team.CV)
            .templateId("slack_vanilla")
            .templateData(Collections.singletonMap("message", alertMessage))
            .userGroupIds(Collections.emptyList())
            .build());
  }

  @Override
  public List<ActivityType> getActivityTypes(String accountId, String orgIdentifier, String projectIdentifier) {
    return Arrays.asList(ActivityType.values());
  }

  private List<AlertRuleDTO> list(String accountId, String projectIdentifier, String orgIdentifier) {
    return hPersistence.createQuery(AlertRule.class)
        .filter(AlertRuleKeys.accountId, accountId)
        .filter(AlertRuleKeys.orgIdentifier, orgIdentifier)
        .filter(AlertRuleKeys.projectIdentifier, projectIdentifier)
        .asList()
        .stream()
        .map(AlertRule::convertToDTO)
        .collect(Collectors.toList());
  }

  private AlertRule getAlertRule(String accountId, String orgIdentifier, String projectIdentifier, String identifier) {
    return hPersistence.createQuery(AlertRule.class)
        .field(AlertRuleKeys.accountId)
        .equal(accountId)
        .field(AlertRuleKeys.orgIdentifier)
        .equal(orgIdentifier)
        .field(AlertRuleKeys.projectIdentifier)
        .equal(projectIdentifier)
        .field(AlertRuleKeys.identifier)
        .equal(identifier)
        .get();
  }
}
