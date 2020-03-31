package io.harness.batch.processing.ccm;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import lombok.Getter;
import org.springframework.batch.core.Job;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Getter
public enum BatchJobType {
  SYNC_BILLING_REPORT_S3(100, 1, ChronoUnit.DAYS, emptyList()),
  DEPLOYMENT_EVENT(150, 1, ChronoUnit.DAYS, emptyList()),
  ECS_EVENT(200, 1, ChronoUnit.DAYS, emptyList()),
  K8S_EVENT(300, 1, ChronoUnit.DAYS, emptyList()),
  K8S_WATCH_EVENT(350, 1, ChronoUnit.DAYS, singletonList(K8S_EVENT)),
  ECS_UTILIZATION(400, 1, ChronoUnit.HOURS, singletonList(ECS_EVENT)),
  K8S_UTILIZATION(500, 1, ChronoUnit.HOURS, singletonList(K8S_EVENT)),
  INSTANCE_BILLING(600, 1, ChronoUnit.DAYS, Arrays.asList(ECS_UTILIZATION, K8S_UTILIZATION)),
  ACTUAL_IDLE_COST_BILLING(650, 1, ChronoUnit.DAYS, singletonList(INSTANCE_BILLING)),
  UNALLOCATED_BILLING(700, 1, ChronoUnit.DAYS, singletonList(INSTANCE_BILLING));

  // Specifies order in which the jobs are to be run
  private final int order;
  private final long interval;
  private final ChronoUnit intervalUnit;
  // dependency jobs that need to be run before this one.
  private final List<BatchJobType> dependentBatchJobs;

  BatchJobType(int order, long interval, ChronoUnit intervalUnit, List<BatchJobType> dependentBatchJobs) {
    this.order = order;
    this.interval = interval;
    this.intervalUnit = intervalUnit;
    this.dependentBatchJobs = dependentBatchJobs;
  }

  public static BatchJobType fromJob(Job job) {
    return BatchJobType.valueOf(job.getName());
  }
}
