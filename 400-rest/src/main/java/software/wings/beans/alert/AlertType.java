/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.alert;

import static software.wings.alerts.AlertCategory.Approval;
import static software.wings.alerts.AlertCategory.ContinuousVerification;
import static software.wings.alerts.AlertCategory.ManualIntervention;
import static software.wings.alerts.AlertCategory.Setup;
import static software.wings.alerts.AlertSeverity.Error;
import static software.wings.alerts.AlertSeverity.Warning;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.alerts.AlertCategory;
import software.wings.alerts.AlertSeverity;

import lombok.Getter;

@TargetModule(HarnessModule._955_ALERT_BEANS)
public enum AlertType {
  ApprovalNeeded(Approval, Warning),
  ManualInterventionNeeded(ManualIntervention, Warning),
  DelegatesDown(Setup, Error, 2),
  InvalidKMS(Setup, Error),
  GitSyncError(Setup, Error),
  GitConnectionError(Setup, Error),
  INVALID_SMTP_CONFIGURATION(Setup, Error),
  EMAIL_NOT_SENT_ALERT(Setup, Warning),
  USERGROUP_SYNC_FAILED(Setup, Error),
  USAGE_LIMIT_EXCEEDED(Setup, Error),
  INSTANCE_USAGE_APPROACHING_LIMIT(Setup, Warning),
  RESOURCE_USAGE_APPROACHING_LIMIT(Setup, Warning),
  DEPLOYMENT_RATE_APPROACHING_LIMIT(Setup, Warning),
  SETTING_ATTRIBUTE_VALIDATION_FAILED(Setup, Warning),
  ARTIFACT_COLLECTION_FAILED(Setup, Error),
  CONTINUOUS_VERIFICATION_ALERT(ContinuousVerification, Error),
  CONTINUOUS_VERIFICATION_DATA_COLLECTION_ALERT(ContinuousVerification, Error),
  MANIFEST_COLLECTION_FAILED(Setup, Error),
  DEPLOYMENT_FREEZE_EVENT(Setup, Warning);

  @Getter private AlertCategory category;
  @Getter private AlertSeverity severity;
  @Getter private int pendingCount;
  @Getter private AlertReconciliation alertReconciliation;

  AlertType(AlertCategory category, AlertSeverity severity) {
    this(category, severity, 0, AlertReconciliation.noop);
  }

  AlertType(AlertCategory category, AlertSeverity severity, int pendingCount) {
    this(category, severity, pendingCount, AlertReconciliation.noop);
  }

  AlertType(AlertCategory category, AlertSeverity severity, int pendingCount, AlertReconciliation alertReconciliation) {
    this.category = category;
    this.severity = severity;
    this.pendingCount = pendingCount;
    this.alertReconciliation = alertReconciliation;
  }
}
