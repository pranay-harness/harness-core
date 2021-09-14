/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.alert.entities;

import io.harness.annotation.StoreIn;
import io.harness.cvng.alert.beans.AlertRuleDTO;
import io.harness.cvng.alert.beans.AlertRuleDTO.AlertCondition;
import io.harness.cvng.alert.beans.AlertRuleDTO.AlertCondition.AlertConditionKeys;
import io.harness.cvng.alert.beans.AlertRuleDTO.NotificationMethod;
import io.harness.cvng.alert.beans.AlertRuleDTO.RiskNotify.RiskNotifyKeys;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.UtilityClass;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "AlertRuleKeys")
@Entity(value = "alertRule", noClassnameStored = true)
@StoreIn(DbAliases.CVNG)
public final class AlertRule implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;

  @FdUniqueIndex @NotNull private String identifier;
  @NotNull private String accountId;
  @NotNull private String orgIdentifier;
  @NotNull private String projectIdentifier;
  @NotNull private String name;

  private boolean enabled;

  private AlertCondition alertCondition;

  private NotificationMethod notificationMethod;

  public AlertRuleDTO convertToDTO() {
    return AlertRuleDTO.builder()
        .uuid(this.getUuid())
        .accountId(this.getAccountId())
        .identifier(this.getIdentifier())
        .orgIdentifier(this.getOrgIdentifier())
        .projectIdentifier(this.getProjectIdentifier())
        .enabled(this.isEnabled())
        .name(this.getName())
        .alertCondition(this.getAlertCondition())
        .notificationMethod(this.getNotificationMethod())
        .build();
  }

  public static AlertRule convertFromDTO(AlertRuleDTO alertRuleDTO) {
    alertRuleDTO.validate();
    return AlertRule.builder()
        .uuid(alertRuleDTO.getUuid())
        .accountId(alertRuleDTO.getAccountId())
        .identifier(alertRuleDTO.getIdentifier())
        .orgIdentifier(alertRuleDTO.getOrgIdentifier())
        .projectIdentifier(alertRuleDTO.getProjectIdentifier())
        .enabled(alertRuleDTO.isEnabled())
        .name(alertRuleDTO.getName())
        .alertCondition(alertRuleDTO.getAlertCondition())
        .notificationMethod(alertRuleDTO.getNotificationMethod())
        .build();
  }

  @UtilityClass
  public static final class AlertRuleKeys {
    public static final String enabledRisk = alertCondition + "." + AlertConditionKeys.enabledRisk;
    public static final String threshold =
        alertCondition + "." + AlertConditionKeys.notify + "." + RiskNotifyKeys.threshold;
    public static final String enabledVerifications = alertCondition + "." + AlertConditionKeys.enabledVerifications;
  }
}
