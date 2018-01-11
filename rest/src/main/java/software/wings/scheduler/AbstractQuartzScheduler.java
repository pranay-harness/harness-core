package software.wings.scheduler;

import static software.wings.core.maintenance.MaintenanceController.isMaintenance;

import com.google.inject.Inject;
import com.google.inject.Injector;

import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoClientURI;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.GuiceQuartzJobFactory;
import software.wings.app.MainConfiguration;
import software.wings.app.SchedulerConfig;
import software.wings.beans.ErrorCode;
import software.wings.core.maintenance.MaintenanceController;
import software.wings.core.maintenance.MaintenanceListener;
import software.wings.dl.MongoConfig;
import software.wings.exception.WingsException;

import java.util.Date;
import java.util.Properties;

public class AbstractQuartzScheduler implements QuartzScheduler, MaintenanceListener {
  private Injector injector;
  private Scheduler scheduler;
  private MainConfiguration configuration;
  private static final Logger logger = LoggerFactory.getLogger(AbstractQuartzScheduler.class);

  /**
   * Instantiates a new Cron scheduler.
   *
   * @param injector      the injector
   * @param configuration the configuration
   */
  @Inject
  public AbstractQuartzScheduler(Injector injector, MainConfiguration configuration) {
    this.injector = injector;
    this.configuration = configuration;
    setupScheduler();
  }

  private void setupScheduler() { // TODO: remove this. find a way to disable cronScheduler in test
    SchedulerConfig schedulerConfig = configuration.getSchedulerConfig();
    if (schedulerConfig.getAutoStart().equals("true")) {
      injector.getInstance(MaintenanceController.class).register(this);
      scheduler = createScheduler();
    }
  }

  private Scheduler createScheduler() {
    try {
      StdSchedulerFactory factory = new StdSchedulerFactory(getDefaultProperties());
      Scheduler scheduler = factory.getScheduler();
      scheduler.setJobFactory(injector.getInstance(GuiceQuartzJobFactory.class));
      if (!isMaintenance()) {
        scheduler.start();
      }
      return scheduler;
    } catch (SchedulerException e) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR).addParam("message", "Could not initialize cron scheduler");
    }
  }

  private Properties getDefaultProperties() {
    SchedulerConfig schedulerConfig = configuration.getSchedulerConfig();

    Properties props = new Properties();
    if (schedulerConfig.getJobstoreclass().equals("com.novemberain.quartz.mongodb.DynamicMongoDBJobStore")) {
      MongoConfig mongoConfig = configuration.getMongoConnectionFactory();
      Builder mongoClientOptions = MongoClientOptions.builder()
                                       .connectTimeout(30000)
                                       .serverSelectionTimeout(90000)
                                       .maxConnectionIdleTime(600000)
                                       .connectionsPerHost(50)
                                       .socketKeepAlive(true);
      MongoClientURI uri = new MongoClientURI(mongoConfig.getUri(), mongoClientOptions);
      props.setProperty("org.quartz.jobStore.class", schedulerConfig.getJobstoreclass());
      props.setProperty("org.quartz.jobStore.mongoUri", uri.getURI());
      props.setProperty("org.quartz.jobStore.dbName", uri.getDatabase());
      props.setProperty("org.quartz.jobStore.collectionPrefix", schedulerConfig.getTablePrefix());
    }

    props.setProperty("org.quartz.scheduler.idleWaitTime", schedulerConfig.getIdleWaitTime());
    props.setProperty("org.quartz.threadPool.threadCount", schedulerConfig.getThreadCount());
    props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
    props.setProperty("org.quartz.plugin.triggHistory.class", "org.quartz.plugins.history.LoggingTriggerHistoryPlugin");
    props.setProperty("org.quartz.plugin.jobHistory.class", "org.quartz.plugins.history.LoggingJobHistoryPlugin");
    props.setProperty("org.quartz.scheduler.instanceName", schedulerConfig.getSchedulerName());
    props.setProperty("org.quartz.scheduler.instanceId", schedulerConfig.getInstanceId());

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
  @Override
  public Date scheduleJob(JobDetail jobDetail, Trigger trigger) {
    try {
      return scheduler.scheduleJob(jobDetail, trigger);
    } catch (org.quartz.ObjectAlreadyExistsException ex) {
      // We do not need to pollute the logs with error logs, just the job already exists.
      // TODO: add additional check if the about to add job properties are the same with the already existing one.
      //       we should update the job if they differ.
    } catch (SchedulerException ex) {
      logger.error("Couldn't schedule cron for job {} with trigger {}", jobDetail.toString(), trigger.toString(), ex);
    }
    return null;
  }

  /**
   * Delete job boolean.
   *
   * @param jobName   the job name
   * @param groupName the group name
   * @return the boolean
   */
  @Override
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

  @Override
  public Date rescheduleJob(TriggerKey triggerKey, Trigger newTrigger) {
    try {
      return scheduler.rescheduleJob(triggerKey, newTrigger);
    } catch (SchedulerException e) {
      logger.error("Couldn't reschedule cron for trigger {} with trigger {}", triggerKey, newTrigger);
    }
    return null;
  }

  @Override
  public void onEnterMaintenance() {
    if (scheduler != null) {
      try {
        scheduler.standby();
      } catch (SchedulerException e) {
        logger.error("Error putting scheduler into standby.", e);
      }
    }
  }

  @Override
  public void onLeaveMaintenance() {
    if (scheduler != null) {
      try {
        scheduler.start();
      } catch (SchedulerException e) {
        logger.error("Error starting scheduler.", e);
      }
    }
  }
}
