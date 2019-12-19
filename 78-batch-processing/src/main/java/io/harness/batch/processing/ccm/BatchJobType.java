package io.harness.batch.processing.ccm;

import org.springframework.batch.core.Job;

public enum BatchJobType {
  SYNC_BILLING_REPORT_S3(100),
  ECS_EVENT(200),
  K8S_EVENT(300),
  ECS_UTILIZATION(400),
  K8S_UTILIZATION(500),
  INSTANCE_BILLING(600);

  // Specifies order in which the jobs are to be run
  private final int order;

  BatchJobType(int order) {
    this.order = order;
  }

  public static BatchJobType fromJob(Job job) {
    return BatchJobType.valueOf(job.getName());
  }

  public int getOrder() {
    return order;
  }
}
