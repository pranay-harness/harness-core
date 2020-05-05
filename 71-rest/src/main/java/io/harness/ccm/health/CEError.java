package io.harness.ccm.health;

import lombok.Getter;

public enum CEError {
  NO_CLUSTERS_TRACKED_BY_HARNESS_CE("No clusters are tracked by Harness CE."),

  PERPETUAL_TASK_CREATION_FAILURE("Perpetual tasks fail to be created for the cluster %s."),

  PERPETUAL_TASK_NOT_ASSIGNED("The task to collect events from the cluster %s has not been assigned to any Delegate."),

  NO_ELIGIBLE_DELEGATE("No eligible delegates can collect events for this cluster %s."),

  DELEGATE_NOT_AVAILABLE("The Delegate for collecting events from the cluster %s has been disconnected."),

  NO_RECENT_EVENTS_PUBLISHED("The cluster %s has not published events since %s."),

  CREDENTIALS_INCORRECT("Credentials are not correct."),

  METRICS_SERVER_NOT_FOUND("Metrics server is not installed in this Kubernetes Cluster %s."),

  BILLING_REPORTS_MISSING("Billing Reports are not generated/available/accessible."),

  INTERNAL_ERROR("Internal error.");

  @Getter private String message;

  CEError(String message) {
    this.message = message;
  }
}
