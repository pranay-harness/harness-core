/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.scheduler;

import java.util.Date;
import java.util.List;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

@Deprecated
public interface PersistentScheduler {
  void ensureJob__UnderConstruction(JobDetail jobDetail, Trigger trigger);
  Date scheduleJob(JobDetail jobDetail, Trigger trigger);
  boolean deleteJob(String jobName, String groupName);
  boolean pauseJob(String jobName, String groupName);
  boolean resumeJob(String jobName, String groupName);
  Date rescheduleJob(TriggerKey triggerKey, Trigger newTrigger);
  Boolean checkExists(String jobName, String groupName);
  List<JobKey> getAllJobKeysForAccount(String accountId) throws SchedulerException;
  void pauseAllQuartzJobsForAccount(String accountId) throws SchedulerException;
  void resumeAllQuartzJobsForAccount(String accountId) throws SchedulerException;
  void deleteAllQuartzJobsForAccount(String accountId) throws SchedulerException;
}
