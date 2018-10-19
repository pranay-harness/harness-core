package software.wings.beans.alert;

import static software.wings.alerts.AlertCategory.Approval;
import static software.wings.alerts.AlertCategory.ManualIntervention;
import static software.wings.alerts.AlertCategory.Setup;
import static software.wings.alerts.AlertSeverity.Error;
import static software.wings.alerts.AlertSeverity.Warning;

import software.wings.alerts.AlertCategory;
import software.wings.alerts.AlertSeverity;

public enum AlertType {
  ApprovalNeeded(Approval, Warning, ApprovalNeededAlert.class),
  ManualInterventionNeeded(ManualIntervention, Warning, ManualInterventionNeededAlert.class),
  NoActiveDelegates(Setup, Error, NoActiveDelegatesAlert.class),
  DelegatesDown(Setup, Error, DelegatesDownAlert.class),
  DelegateProfileError(Setup, Error, DelegateProfileErrorAlert.class),
  NoEligibleDelegates(Setup, Error, NoEligibleDelegatesAlert.class),
  InvalidKMS(Setup, Error, KmsSetupAlert.class),
  GitSyncError(Setup, Error, GitSyncErrorAlert.class),
  GitConnectionError(Setup, Error, GitConnectionErrorAlert.class),
  INVALID_SMTP_CONFIGURATION(Setup, Error, InvalidSMTPConfigAlert.class),
  EMAIL_NOT_SENT_ALERT(Setup, Warning, EmailSendingFailedAlert.class),
  USERGROUP_SYNC_FAILED(Setup, Error, SSOSyncFailedAlert.class);

  private AlertCategory category;
  private AlertSeverity severity;
  private Class<? extends AlertData> alertDataClass;

  AlertType(AlertCategory category, AlertSeverity severity, Class<? extends AlertData> alertDataClass) {
    this.category = category;
    this.severity = severity;
    this.alertDataClass = alertDataClass;
  }

  public AlertCategory getCategory() {
    return category;
  }

  public AlertSeverity getSeverity() {
    return severity;
  }

  public Class<? extends AlertData> getAlertDataClass() {
    return alertDataClass;
  }
}
