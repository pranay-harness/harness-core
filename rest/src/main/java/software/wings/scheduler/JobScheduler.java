package software.wings.scheduler;

import com.google.inject.Injector;

import com.mongodb.MongoClientURI;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.GuiceQuartzJobFactory;
import software.wings.app.MainConfiguration;
import software.wings.app.SchedulerConfig;
import software.wings.beans.ErrorCode;
import software.wings.dl.MongoConfig;
import software.wings.exception.WingsException;
import software.wings.utils.Misc;

import java.util.Date;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 10/21/16.
 */
@Singleton
public class JobScheduler {
  private Injector injector;
  private Scheduler scheduler;
  private MainConfiguration configuration;
  private final Logger logger = LoggerFactory.getLogger(getClass());

  /**
   * Instantiates a new Cron scheduler.
   *
   * @param injector      the injector
   * @param configuration the configuration
   */
  @Inject
  public JobScheduler(Injector injector, MainConfiguration configuration) {
    this.injector = injector;
    this.configuration = configuration;
    setupScheduler();
  }

  private void setupScheduler() { // TODO: remove this. find a way to disable cronScheduler in test
    SchedulerConfig schedulerConfig = configuration.getSchedulerConfig();
    if (schedulerConfig.getAutoStart().equals("true")) {
      this.scheduler = createScheduler();
    }
  }

  private Scheduler createScheduler() {
    try {
      StdSchedulerFactory factory = new StdSchedulerFactory(getDefaultProperties());
      Scheduler scheduler = factory.getScheduler();
      scheduler.setJobFactory(injector.getInstance(GuiceQuartzJobFactory.class));
      scheduler.start();
      return scheduler;
    } catch (SchedulerException e) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, "message", "Could not initialize cron scheduler");
    }
  }

  private Properties getDefaultProperties() {
    SchedulerConfig schedulerConfig = configuration.getSchedulerConfig();
    MongoConfig mongoConfig = configuration.getMongoConnectionFactory();
    MongoClientURI uri = new MongoClientURI(mongoConfig.getUri());
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", schedulerConfig.getJobstoreclass());
    props.setProperty("org.quartz.jobStore.mongoUri", uri.getURI());
    props.setProperty("org.quartz.jobStore.dbName", uri.getDatabase());
    props.setProperty("org.quartz.scheduler.idleWaitTime", schedulerConfig.getIdleWaitTime());
    props.setProperty("org.quartz.threadPool.threadCount", schedulerConfig.getThreadCount());
    props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
    props.setProperty("org.quartz.plugin.triggHistory.class", "org.quartz.plugins.history.LoggingTriggerHistoryPlugin");
    props.setProperty("org.quartz.plugin.jobHistory.class", "org.quartz.plugins.history.LoggingJobHistoryPlugin");
    return props;
  }

  /**
   * Gets scheduler.
   *
   * @return the scheduler
   */
  public Scheduler getScheduler() {
    return scheduler;
  }

  /**
   * Schedule job date.
   *
   * @param jobDetail the job detail
   * @param trigger   the trigger
   * @return the date
   */
  public Date scheduleJob(JobDetail jobDetail, Trigger trigger) {
    try {
      return scheduler.scheduleJob(jobDetail, trigger);
    } catch (SchedulerException ex) {
      logger.error("Couldn't schedule cron for job {} with trigger {}", jobDetail.toString(), trigger.toString());
    }
    return null;
  }

  /**
   * Delete job boolean.
   *
   *
   * @param jobName the job name
   * @param groupName the group name
   * @return the boolean
   */
  public boolean deleteJob(String jobName, String groupName) {
    if (groupName != null && jobName != null) {
      try {
        return scheduler.deleteJob(new JobKey(jobName, groupName));
      } catch (SchedulerException ex) {
        logger.error(String.format("Couldn't delete cron job [%s %s] ", groupName, jobName), ex);
      }
    }
    return false;
  }
}
