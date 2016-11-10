package software.wings.scheduler;

import com.google.inject.Injector;

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
import software.wings.beans.ErrorCodes;
import software.wings.exception.WingsException;

import java.util.Date;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by anubhaw on 10/21/16.
 */
@Singleton
public class CronScheduler {
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
  public CronScheduler(Injector injector, MainConfiguration configuration) {
    this.injector = injector;
    this.configuration = configuration;
    this.scheduler = createScheduler();
  }

  private Scheduler createScheduler() {
    try {
      StdSchedulerFactory factory = new StdSchedulerFactory();
      factory.initialize(getDefaultProperties());
      Scheduler scheduler = factory.getScheduler();
      scheduler.setJobFactory(injector.getInstance(GuiceQuartzJobFactory.class));
      scheduler.start();
      return scheduler;
    } catch (SchedulerException e) {
      throw new WingsException(ErrorCodes.UNKNOWN_ERROR, "message", "Could not initialize cron scheduler");
    }
  }

  private Properties getDefaultProperties() {
    SchedulerConfig schedulerConfig = configuration.getSchedulerConfig();
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", schedulerConfig.getJobstoreclass());
    props.setProperty("org.quartz.jobStore.mongoUri", schedulerConfig.getJobstoreDbUrl());
    props.setProperty("org.quartz.jobStore.dbName", schedulerConfig.getJobstoreDbName());
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
   * @param jobName the job name
   * @return the boolean
   */
  public boolean deleteJob(String jobName) {
    if (jobName != null) {
      try {
        return scheduler.deleteJob(new JobKey(jobName));
      } catch (SchedulerException ex) {
        logger.error("Couldn't delete cron for artifact auto download " + ex);
      }
    }
    return false;
  }
}
