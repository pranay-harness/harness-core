package io.harness.ccm.health;

import lombok.Getter;

public enum CEError {
  PERPETUAL_TASK_CREATION_FAILURE("Perpetual tasks fail to be created for this cluster."),

  PERPETUAL_TASK_NOT_ASSIGNED(
      "The task to collect events from the cluster with id=%s has not been assigned to any Delegate."),

  DELEGATE_NOT_AVAILABLE("The Delegate for collecting events from the cluster with id=%s has been disconnected."),

  PERPETUAL_TASK_MISSING_HEARTBEAT("The task to collect events from the cluster with id=%s is missing heartbeats."),

  CREDENTIALS_INCORRECT("Credentials are not correct."),

  BILLING_REPORTS_MISSING("Billing Reports are not generated/available/accessible."),

  INTERNAL_ERROR("Internal error.");

  @Getter private String message;

  CEError(String message) {
    this.message = message;
  }
}
