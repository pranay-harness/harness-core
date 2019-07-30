package io.harness.jobs.sg247;

import io.harness.scheduler.PersistentScheduler;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;

/**
 * Verification job that handles scheduling jobs related to APM and Logs
 *
 * Created by Pranjal on 10/04/2018
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
@Slf4j
public class ServiceGuardMainJob implements Job {
  // Cron name to uniquely identify the cron
  public static final String SERVICE_GUARD_MAIN_CRON = "SERVICE_GUARD_MAIN_CRON";

  public static void removeJob(PersistentScheduler jobScheduler) {
    jobScheduler.deleteJob(SERVICE_GUARD_MAIN_CRON, SERVICE_GUARD_MAIN_CRON);
  }

  @Override
  public void execute(JobExecutionContext jobExecutionContext) {
    // do nothjng
  }
}
