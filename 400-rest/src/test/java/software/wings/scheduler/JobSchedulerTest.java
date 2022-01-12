/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.scheduler;

import static io.harness.rule.OwnerRule.ANUBHAW;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;

@Slf4j
public class JobSchedulerTest extends WingsBaseTest {
  @Inject private BackgroundJobScheduler jobScheduler;

  @Before
  public void setUp() throws Exception {
    jobScheduler.getScheduler().getJobGroupNames().forEach(groupName -> {
      try {
        jobScheduler.getScheduler().getJobKeys(GroupMatcher.groupEquals(groupName)).forEach(jobKey -> {
          try {
            jobScheduler.getScheduler().deleteJob(jobKey);
          } catch (SchedulerException e) {
            log.error("", e);
          }
        });
      } catch (SchedulerException e) {
        log.error("", e);
      }
    });
  }

  public static class JobA implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      log.info("Job a execution");
    }
  }

  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  @Ignore("TODO: please provide clear motivation why this test is ignored")
  public void shouldResumeIncompleteJob() throws InterruptedException, SchedulerException {
    jobScheduler.getScheduler().resumeAll();
    Thread.sleep(100000);
    log.info("Completed");
  }
}
