package software.wings.scheduler;

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
import software.wings.WingsBaseTest;

import java.util.Properties;
import javax.inject.Inject;

/**
 * Created by anubhaw on 10/21/16.
 */
public class CronSchedulerTest extends WingsBaseTest {
  @Inject private CronScheduler cronScheduler;

  @Before
  public void setUp() throws Exception {
    cronScheduler.getScheduler().getJobGroupNames().forEach(groupName -> {
      try {
        cronScheduler.getScheduler().getJobKeys(GroupMatcher.groupEquals(groupName)).forEach(jobKey -> {
          try {
            cronScheduler.getScheduler().deleteJob(jobKey);
          } catch (SchedulerException e) {
            e.printStackTrace();
          }
        });
      } catch (SchedulerException e) {
        e.printStackTrace();
      }
    });
  }

  public static class JobA implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      System.out.println("Job a execution");
    }
  }

  @Test
  @Ignore
  public void shouldCreateScheduler() throws SchedulerException, InterruptedException {
    // define the job and tie it to our HelloJob class
    JobDetail job = JobBuilder.newJob(ArtifactCollectionJob.class)
                        .withIdentity("myJob", "group1")
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
    cronScheduler.getScheduler().scheduleJob(job, trigger2);
    Thread.sleep(1000000);
    System.out.println("Completed");
  }

  @Test
  @Ignore
  public void shouldResumeIncompleteJob() throws InterruptedException, SchedulerException {
    cronScheduler.getScheduler().resumeAll();
    Thread.sleep(100000);
    System.out.println("Completed");
  }

  private static Properties createProps() {
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", "com.novemberain.quartz.mongodb.DynamicMongoDBJobStore");
    props.setProperty("org.quartz.jobStore.mongoUri", "mongodb://localhost:27017");
    //;; Often check for triggers to speed up collisions:
    props.setProperty("org.quartz.scheduler.idleWaitTime", "1000");
    props.setProperty("org.quartz.jobStore.dbName", "wings");
    props.setProperty("org.quartz.threadPool.threadCount", "1");
    props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
    props.setProperty("org.quartz.plugin.triggHistory.class", "org.quartz.plugins.history.LoggingTriggerHistoryPlugin");
    props.setProperty("org.quartz.plugin.jobHistory.class", "org.quartz.plugins.history.LoggingJobHistoryPlugin");
    return props;
  }
}
