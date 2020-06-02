package io.harness.batch.processing.ccm;

import static io.harness.batch.processing.ccm.BatchJobBucket.IN_CLUSTER;
import static io.harness.batch.processing.ccm.BatchJobBucket.OUT_OF_CLUSTER;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import lombok.Getter;
import org.springframework.batch.core.Job;

import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Getter
public enum BatchJobType {
  BILLING_DATA_PIPELINE(50, 1, ChronoUnit.DAYS, emptyList(), OUT_OF_CLUSTER),
  SYNC_BILLING_REPORT_S3(100, 1, ChronoUnit.DAYS, emptyList(), OUT_OF_CLUSTER),
  DEPLOYMENT_EVENT(150, 1, ChronoUnit.DAYS, emptyList(), IN_CLUSTER),
  ECS_EVENT(200, 1, ChronoUnit.DAYS, emptyList(), IN_CLUSTER),
  K8S_EVENT(300, 1, ChronoUnit.DAYS, emptyList(), IN_CLUSTER),
  K8S_WATCH_EVENT(350, 1, ChronoUnit.DAYS, singletonList(K8S_EVENT), IN_CLUSTER),
  ECS_UTILIZATION(400, 1, ChronoUnit.HOURS, singletonList(ECS_EVENT), IN_CLUSTER),
  K8S_UTILIZATION(500, 1, ChronoUnit.HOURS, singletonList(K8S_EVENT), IN_CLUSTER),
  INSTANCE_BILLING(600, 1, ChronoUnit.DAYS, Arrays.asList(ECS_UTILIZATION, K8S_UTILIZATION), IN_CLUSTER),
  ACTUAL_IDLE_COST_BILLING(650, 1, ChronoUnit.DAYS, singletonList(INSTANCE_BILLING), IN_CLUSTER),
  UNALLOCATED_BILLING(700, 1, ChronoUnit.DAYS, singletonList(INSTANCE_BILLING), IN_CLUSTER),
  INSTANCE_BILLING_HOURLY(750, 1, ChronoUnit.HOURS, Arrays.asList(INSTANCE_BILLING), IN_CLUSTER),
  ACTUAL_IDLE_COST_BILLING_HOURLY(800, 1, ChronoUnit.HOURS, singletonList(INSTANCE_BILLING_HOURLY), IN_CLUSTER),
  UNALLOCATED_BILLING_HOURLY(850, 1, ChronoUnit.HOURS, singletonList(INSTANCE_BILLING_HOURLY), IN_CLUSTER);

  // Specifies order in which the jobs are to be run
  private final int order;
  private final long interval;
  private final ChronoUnit intervalUnit;
  // dependency jobs that need to be run before this one.
  private final List<BatchJobType> dependentBatchJobs;
  private final BatchJobBucket batchJobBucket;

  BatchJobType(int order, long interval, ChronoUnit intervalUnit, List<BatchJobType> dependentBatchJobs,
      BatchJobBucket batchJobBucket) {
    this.order = order;
    this.interval = interval;
    this.intervalUnit = intervalUnit;
    this.dependentBatchJobs = dependentBatchJobs;
    this.batchJobBucket = batchJobBucket;
  }

  public static BatchJobType fromJob(Job job) {
    return BatchJobType.valueOf(job.getName());
  }
}
