package software.wings.scheduler;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;

/**
 * Created by anubhaw on 10/21/16.
 */
@Ignore
public class JobSchedulerTest extends WingsBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(JobSchedulerTest.class);

  @Inject private JobScheduler jobScheduler;

  @Before
  public void setUp() throws Exception {
    jobScheduler.getScheduler().getJobGroupNames().forEach(groupName -> {
      try {
        jobScheduler.getScheduler().getJobKeys(GroupMatcher.groupEquals(groupName)).forEach(jobKey -> {
          try {
            jobScheduler.getScheduler().deleteJob(jobKey);
          } catch (SchedulerException e) {
            logger.error("", e);
          }
        });
      } catch (SchedulerException e) {
        logger.error("", e);
      }
    });
  }

  public static class JobA implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      logger.info("Job a execution");
    }
  }

  @Test
  @Ignore
  public void shouldCreateScheduler() throws SchedulerException, InterruptedException {
    // define the job and tie it to our HelloJob class
    JobDetail job = JobBuilder.newJob(ArtifactCollectionJob.class)
                        .withIdentity("myJob")
                        .usingJobData("artifactStreamId", "AnKfDNKdReaQoDVLJbeWpQ")
                        .usingJobData("appId", "vwWDnKjhSzml8GuBPQq-6Q")
                        .build();

    // Trigger the job to run now, and then every 40 seconds
    Trigger trigger =
        TriggerBuilder.newTrigger()
            .withIdentity("myTrigger", "group1")
            .startNow()
            .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(20000).withRepeatCount(1))
            .build();

    Trigger trigger2 = TriggerBuilder.newTrigger().withIdentity("a", "v").startNow().build();

    // Tell quartz to schedule the job using our trigger
    jobScheduler.getScheduler().scheduleJob(job, trigger2);
    Thread.sleep(1000000);
    logger.info("Completed");
  }

  @Test
  @Ignore
  public void shouldResumeIncompleteJob() throws InterruptedException, SchedulerException {
    jobScheduler.getScheduler().resumeAll();
    Thread.sleep(100000);
    logger.info("Completed");
  }
}
