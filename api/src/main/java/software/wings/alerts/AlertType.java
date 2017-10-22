package software.wings.alerts;

import static software.wings.alerts.AlertCategory.Approval;
import static software.wings.alerts.AlertCategory.ManualIntervention;
import static software.wings.alerts.AlertCategory.Setup;
import static software.wings.alerts.AlertSeverity.Error;
import static software.wings.alerts.AlertSeverity.Warning;

public enum AlertType {
  ApprovalNeeded(Approval, Warning),
  ManualInterventionNeeded(ManualIntervention, Warning),
  NoActiveDelegates(Setup, Error),
  NoEligibleDelegates(Setup, Error);

  private AlertCategory category;
  private AlertSeverity severity;

  AlertType(AlertCategory category, AlertSeverity severity) {
    this.category = category;
    this.severity = severity;
  }

  public AlertCategory getCategory() {
    return category;
  }

  public AlertSeverity getSeverity() {
    return severity;
  }
}
