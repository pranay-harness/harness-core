package io.harness.batch.processing.ccm;

import lombok.Getter;
import org.springframework.batch.core.Job;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Getter
public enum BatchJobType {
  SYNC_BILLING_REPORT_S3(100, 1, ChronoUnit.DAYS, Collections.EMPTY_LIST),
  DEPLOYMENT_EVENT(150, 1, ChronoUnit.DAYS, Collections.EMPTY_LIST),
  ECS_EVENT(200, 1, ChronoUnit.DAYS, Collections.EMPTY_LIST),
  K8S_EVENT(300, 1, ChronoUnit.DAYS, Collections.EMPTY_LIST),
  ECS_UTILIZATION(400, 1, ChronoUnit.HOURS, Collections.singletonList(BatchJobType.ECS_EVENT)),
  K8S_UTILIZATION(500, 1, ChronoUnit.HOURS, Collections.singletonList(BatchJobType.K8S_EVENT)),
  INSTANCE_BILLING(600, 1, ChronoUnit.DAYS, Arrays.asList(BatchJobType.ECS_UTILIZATION, BatchJobType.K8S_UTILIZATION)),
  UNALLOCATED_BILLING(700, 1, ChronoUnit.DAYS, Collections.singletonList(BatchJobType.INSTANCE_BILLING));

  // Specifies order in which the jobs are to be run
  private final int order;
  private final long interval;
  private final ChronoUnit intervalUnit;
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
