package io.harness.ccm.health;

import lombok.Getter;

public enum CEError {
  NO_CLUSTERS_TRACKED_BY_HARNESS_CE("No ECS cluster tracked by Harness for this AWS cloud provider %s."),

  PERPETUAL_TASK_CREATION_FAILURE("Perpetual tasks fail to be created for the cluster %s."),

  DELEGATE_NOT_AVAILABLE("Unable to collect events. The Delegate %s may be disconnected or deleted."),

  NO_ELIGIBLE_DELEGATE("No Delegate has all the requisites to access the cluster %s."),

  PERPETUAL_TASK_NOT_ASSIGNED("Initializing the Delegate to collect events. Events will arrive in a few minutes."),

  NO_RECENT_EVENTS_PUBLISHED("The cluster %s has not published events since %s."),

  CREDENTIALS_INCORRECT("Credentials are not correct."),

  METRICS_SERVER_NOT_FOUND("Metrics server is not installed in this Kubernetes Cluster %s."),

  AWS_ECS_CLUSTER_NOT_FOUND("The ECS cluster %s received error code ClusterNotFoundException from Amazon ECS service."),

  BILLING_REPORTS_MISSING("Billing Reports are not generated/available/accessible."),

  INTERNAL_ERROR("Internal error.");

  @Getter private String message;

  CEError(String message) {
    this.message = message;
  }
}
