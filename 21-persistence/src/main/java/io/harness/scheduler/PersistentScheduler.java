package io.harness.scheduler;

import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import java.util.Date;

public interface PersistentScheduler {
  Date scheduleJob(JobDetail jobDetail, Trigger trigger);
  boolean deleteJob(String jobName, String groupName);
  Date rescheduleJob(TriggerKey triggerKey, Trigger newTrigger);
  Boolean checkExists(String jobName, String groupName);
}
