/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;

import software.wings.alerts.AlertCategory;
import software.wings.beans.Base;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;

@OwnedBy(PL)
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "AlertNotificationRulekeys")
@Entity(value = "alertNotificationRules", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class AlertNotificationRule extends Base implements AccountAccess {
  public static final String ALERT_CATEGORY = "alertCategory";

  @FdIndex @Setter String accountId;
  @FdIndex AlertCategory alertCategory;
  AlertFilter alertFilter;
  @NonNull Set<String> userGroupsToNotify = Collections.emptySet();

  public boolean isDefault() {
    return alertCategory == AlertCategory.All;
  }

  public boolean hasAlertFilter() {
    return alertFilter != null;
  }
}
