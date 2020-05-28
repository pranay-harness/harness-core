package software.wings.beans.alert;

import static software.wings.alerts.AlertCategory.Approval;
import static software.wings.alerts.AlertCategory.ContinuousVerification;
import static software.wings.alerts.AlertCategory.ManualIntervention;
import static software.wings.alerts.AlertCategory.Setup;
import static software.wings.alerts.AlertSeverity.Error;
import static software.wings.alerts.AlertSeverity.Warning;

import lombok.Getter;
import software.wings.alerts.AlertCategory;
import software.wings.alerts.AlertSeverity;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.beans.alert.cv.ContinuousVerificationDataCollectionAlert;

public enum AlertType {
  ApprovalNeeded(Approval, Warning, ApprovalNeededAlert.class),
  ManualInterventionNeeded(ManualIntervention, Warning, ManualInterventionNeededAlert.class),
  NoActiveDelegates(Setup, Error, NoActiveDelegatesAlert.class, 2),
  DelegatesDown(Setup, Error, DelegatesDownAlert.class, 2),
  DelegateProfileError(Setup, Error, DelegateProfileErrorAlert.class),
  NoEligibleDelegates(
      Setup, Error, NoEligibleDelegatesAlert.class, 0, NoEligibleDelegatesAlertReconciliation.builder().build()),
  InvalidKMS(Setup, Error, KmsSetupAlert.class),
  GitSyncError(Setup, Error, GitSyncErrorAlert.class),
  GitConnectionError(Setup, Error, GitConnectionErrorAlert.class),
  INVALID_SMTP_CONFIGURATION(Setup, Error, InvalidSMTPConfigAlert.class),
  EMAIL_NOT_SENT_ALERT(Setup, Warning, EmailSendingFailedAlert.class),
  USERGROUP_SYNC_FAILED(Setup, Error, SSOSyncFailedAlert.class),
  USAGE_LIMIT_EXCEEDED(Setup, Error, UsageLimitExceededAlert.class),
  INSTANCE_USAGE_APPROACHING_LIMIT(Setup, Warning, InstanceUsageLimitAlert.class),
  RESOURCE_USAGE_APPROACHING_LIMIT(Setup, Warning, ResourceUsageApproachingLimitAlert.class),
  DEPLOYMENT_RATE_APPROACHING_LIMIT(Setup, Warning, DeploymentRateApproachingLimitAlert.class),
  SETTING_ATTRIBUTE_VALIDATION_FAILED(Setup, Warning, SettingAttributeValidationFailedAlert.class),
  ARTIFACT_COLLECTION_FAILED(Setup, Error, ArtifactCollectionFailedAlert.class),
  CONTINUOUS_VERIFICATION_ALERT(ContinuousVerification, Error, ContinuousVerificationAlertData.class),
  CONTINUOUS_VERIFICATION_DATA_COLLECTION_ALERT(
      ContinuousVerification, Error, ContinuousVerificationDataCollectionAlert.class);

  @Getter private AlertCategory category;
  @Getter private AlertSeverity severity;
  @Getter private Class<? extends AlertData> alertDataClass;
  @Getter private int pendingCount;
  @Getter private AlertReconciliation alertReconciliation;

  AlertType(AlertCategory category, AlertSeverity severity, Class<? extends AlertData> alertDataClass) {
    this(category, severity, alertDataClass, 0, AlertReconciliation.noop);
  }

  AlertType(
      AlertCategory category, AlertSeverity severity, Class<? extends AlertData> alertDataClass, int pendingCount) {
    this(category, severity, alertDataClass, pendingCount, AlertReconciliation.noop);
  }

  AlertType(AlertCategory category, AlertSeverity severity, Class<? extends AlertData> alertDataClass, int pendingCount,
      AlertReconciliation alertReconciliation) {
    this.category = category;
    this.severity = severity;
    this.alertDataClass = alertDataClass;
    this.pendingCount = pendingCount;
    this.alertReconciliation = alertReconciliation;
  }
}
