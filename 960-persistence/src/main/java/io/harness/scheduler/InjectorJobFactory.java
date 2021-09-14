/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.scheduler;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

public class InjectorJobFactory implements JobFactory {
  @Inject private Injector injector;

  @Override
  public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler) throws SchedulerException {
    JobDetail jobDetail = triggerFiredBundle.getJobDetail();
    Class jobClass = jobDetail.getJobClass();
    try {
      return (Job) injector.getInstance(jobClass);
    } catch (Exception e) {
      throw new UnsupportedOperationException(e);
    }
  }
}
