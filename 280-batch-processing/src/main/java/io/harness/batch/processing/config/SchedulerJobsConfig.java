package io.harness.batch.processing.config;

import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class SchedulerJobsConfig {
  private String budgetAlertsJobCron;
  private String weeklyReportsJobCron;
}
