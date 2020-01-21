package io.harness.ccm.health;

import lombok.Getter;

public enum CEError {
  DELEGATE_NOT_AVAILABLE("Delegates are not available."),

  CREDENTIALS_INCORRECT("Credentials are not correct."),

  PERPETUAL_TASK_CREATION_FAILURE("Perpetual tasks fail to be created for this cluster."),

  PERPETUAL_TASK_MISSING_HEARTBEAT("Perpetual tasks are missing heartbeats."),

  BILLING_REPORTS_MISSING("Billing Reports are not generated/available/accessible."),

  INTERNAL_ERROR("Internal error.");

  @Getter private String message;

  CEError(String message) {
    this.message = message;
  }
}
