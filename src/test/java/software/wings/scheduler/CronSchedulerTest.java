package software.wings.scheduler;

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

import java.util.Properties;

/**
 * Created by anubhaw on 10/21/16.
 */
public class CronSchedulerTest {
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
  public static class JobA implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
      System.out.println("Job a execution");
    }
  }

  @Test
  public void shouldCreateScheduler() throws SchedulerException, InterruptedException {
    CronScheduler cronScheduler = new CronScheduler(createProps());

    // define the job and tie it to our HelloJob class
    JobDetail job = JobBuilder.newJob(JobA.class).withIdentity("myJob", "group1").build();

    // Trigger the job to run now, and then every 40 seconds
    Trigger trigger = TriggerBuilder.newTrigger()
                          .withIdentity("myTrigger", "group1")
                          .startNow()
                          .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(2).repeatForever())
                          .build();

    // Tell quartz to schedule the job using our trigger
    cronScheduler.getScheduler().scheduleJob(job, trigger);
    Thread.sleep(10000);
    System.out.println("Completed");
  }

  @Test
  public void shouldResumeIncompleteJob() throws InterruptedException, SchedulerException {
    CronScheduler cronScheduler = new CronScheduler(createProps());
    cronScheduler.getScheduler().resumeAll();
    Thread.sleep(100000);
    System.out.println("Completed");
  }
}
