package software.wings.alerts;

import static software.wings.alerts.AlertCategory.Approval;
import static software.wings.alerts.AlertCategory.ManualIntervention;
import static software.wings.alerts.AlertCategory.Setup;
import static software.wings.alerts.AlertSeverity.Error;
import static software.wings.alerts.AlertSeverity.Warning;

public enum AlertType {
  ApprovalNeeded(Approval, Warning, "%s needs approval"),
  ManualInterventionNeeded(ManualIntervention, Warning, "%s requires manual action"),
  NoActiveDelegates(Setup, Error, "No delegates are available"),
  NoEligibleDelegates(Setup, Error, "No delegates are eligible to execute %s tasks");

  private String title;
  private AlertCategory category;
  private AlertSeverity severity;

  AlertType(AlertCategory category, AlertSeverity severity, String title) {
    this.category = category;
    this.severity = severity;
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  public AlertCategory getCategory() {
    return category;
  }

  public AlertSeverity getSeverity() {
    return severity;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
