package software.wings.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.quartz.JobKey.jobKey;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Test;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import software.wings.WingsBaseTest;
import software.wings.rules.SetupScheduler;

@SetupScheduler
public class VerificationJobSchedulerTest extends WingsBaseTest {
  @Inject @Named("VerificationJobScheduler") private QuartzScheduler jobScheduler;

  @Test
  public void config() throws SchedulerException {
    assertEquals("verification", ((JobScheduler) jobScheduler).getScheduler().getMetaData().getSchedulerInstanceId());
    assertEquals(
        "verification_scheduler", ((JobScheduler) jobScheduler).getScheduler().getMetaData().getSchedulerName());
    assertEquals(15, ((JobScheduler) jobScheduler).getScheduler().getMetaData().getThreadPoolSize());
  }
  @Test
  public void defaultJobs() throws SchedulerException {
    if (((JobScheduler) jobScheduler).getScheduler() == null
        || ((JobScheduler) jobScheduler)
               .getScheduler()
               .getJobKeys(GroupMatcher.anyGroup())
               .contains(jobKey("LEARNING_ENGINE_TASK_QUEUE_DEL_CRON"))) {
      fail("Learning engine task queue delete cron not started");
    }

    if (((JobScheduler) jobScheduler).getScheduler() == null
        || ((JobScheduler) jobScheduler)
               .getScheduler()
               .getJobKeys(GroupMatcher.anyGroup())
               .contains(jobKey("NEW_RELIC_METRIC_NAME_COLLECT_CRON"))) {
      fail("NewRelic metric name collection cron not started");
    }
  }
}
