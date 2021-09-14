/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.alert.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.cvng.alert.util.VerificationStatus;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.notification.NotificationSettingType;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "AlertRuleDTOKeys")
public class AlertRuleDTO {
  String uuid;
  boolean enabled;
  String name;
  AlertCondition alertCondition;
  NotificationMethod notificationMethod;

  String identifier;
  String accountId;
  String orgIdentifier;
  String projectIdentifier;

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "AlertConditionKeys")
  public static class AlertCondition {
    boolean isAllServices;
    boolean isAllEnvironments;
    List<String> services;
    List<String> environments;

    boolean enabledVerifications;
    VerificationsNotify verificationsNotify;

    boolean enabledRisk;
    RiskNotify notify;
  }

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "VerificationsNotifyKeys")
  public static class VerificationsNotify {
    boolean isAllActivityTpe;
    boolean isAllVerificationStatuses;
    List<ActivityType> activityTypes;
    List<VerificationStatus> verificationStatuses;
  }

  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "RiskNotifyKeys")
  public static class RiskNotify {
    int threshold;
  }

  @Value
  @Builder
  @FieldNameConstants(innerTypeName = "NotificationMethodKeys")
  public static class NotificationMethod {
    NotificationSettingType notificationSettingType;

    String slackWebhook;
    String slackChannelName;

    String pagerDutyKey;

    List<String> emails;
  }

  public void validate() {
    if (isEmpty(alertCondition.getServices())) {
      alertCondition.setAllServices(true);
    }

    if (alertCondition.isAllServices()) {
      alertCondition.setServices(Lists.newArrayList());
    }

    if (isEmpty(alertCondition.getEnvironments())) {
      alertCondition.setAllEnvironments(true);
    }

    if (alertCondition.isAllEnvironments()) {
      alertCondition.setEnvironments(Lists.newArrayList());
    }

    if (isEmpty(alertCondition.getVerificationsNotify().getActivityTypes())) {
      alertCondition.getVerificationsNotify().setAllActivityTpe(true);
    }

    if (alertCondition.getVerificationsNotify().isAllActivityTpe()) {
      alertCondition.getVerificationsNotify().setActivityTypes(Lists.newArrayList());
    }

    if (isEmpty(alertCondition.getVerificationsNotify().getVerificationStatuses())) {
      alertCondition.getVerificationsNotify().setAllVerificationStatuses(true);
    }

    if (alertCondition.getVerificationsNotify().isAllVerificationStatuses()) {
      alertCondition.getVerificationsNotify().setVerificationStatuses(Lists.newArrayList());
    }
  }
}
