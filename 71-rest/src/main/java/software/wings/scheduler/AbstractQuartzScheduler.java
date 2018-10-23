package software.wings.scheduler;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.core.maintenance.MaintenanceController.isMaintenance;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoClientURI;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.mongo.MongoConfig;
import io.harness.persistence.ReadPref;
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
import software.wings.app.SchedulerConfig;
import software.wings.core.maintenance.MaintenanceController;
import software.wings.core.maintenance.MaintenanceListener;
import software.wings.core.managerConfiguration.ConfigChangeEvent;
import software.wings.core.managerConfiguration.ConfigChangeListener;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;

import java.util.Date;
import java.util.List;
import java.util.Properties;

public class AbstractQuartzScheduler implements QuartzScheduler, MaintenanceListener, ConfigChangeListener {
  protected Injector injector;
  protected Scheduler scheduler;
  private SchedulerConfig schedulerConfig;
  private MongoConfig mongoConfig;
  private static final Logger logger = LoggerFactory.getLogger(AbstractQuartzScheduler.class);

  @Inject
  public AbstractQuartzScheduler(Injector injector, SchedulerConfig schedulerConfig, MongoConfig mongoConfig) {
    this.injector = injector;
    this.schedulerConfig = schedulerConfig;
    this.mongoConfig = mongoConfig;
    setupScheduler();
  }

  protected void setupScheduler() { // TODO: remove this. find a way to disable cronScheduler in test
    if (schedulerConfig.getAutoStart().equals("true")) {
      injector.getInstance(MaintenanceController.class).register(this);
      injector.getInstance(ConfigurationController.class).register(this, asList(ConfigChangeEvent.PrimaryChanged));
      scheduler = createScheduler();
    }
  }

  private Scheduler createScheduler() {
    try {
      final Properties properties = getDefaultProperties();
      StdSchedulerFactory factory = new StdSchedulerFactory(properties);
      Scheduler scheduler = factory.getScheduler();

      // by default scheduler does not create all needed mongo indexes.
      // it is a bit hack but we are going to add them from here

      WingsPersistence wingsPersistence = injector.getInstance(Key.get(WingsMongoPersistence.class));

      final String prefix = properties.getProperty("org.quartz.jobStore.collectionPrefix");
      final DBCollection triggers =
          wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, prefix + "_triggers");
      BasicDBObject jobIdKey = new BasicDBObject("jobId", 1);
      triggers.createIndex(jobIdKey, null, false);

      BasicDBObject fireKeys = new BasicDBObject();
      fireKeys.append("state", 1);
      fireKeys.append("nextFireTime", 1);
      triggers.createIndex(fireKeys, "fire", false);

      scheduler.setJobFactory(injector.getInstance(GuiceQuartzJobFactory.class));
      ConfigurationController configurationController = injector.getInstance(Key.get(ConfigurationController.class));
      if (!isMaintenance() && configurationController.isPrimary()) {
        scheduler.start();
      }
      return scheduler;
    } catch (SchedulerException exception) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, exception)
          .addParam("message", "Could not initialize cron scheduler");
    }
  }

  protected Properties getDefaultProperties() {
    Properties props = new Properties();
    if (schedulerConfig.getJobstoreclass().equals("com.novemberain.quartz.mongodb.DynamicMongoDBJobStore")) {
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
      props.setProperty("org.quartz.jobStore.mongoOptionWriteConcernTimeoutMillis",
          schedulerConfig.getMongoOptionWriteConcernTimeoutMillis());
      // props.setProperty("org.quartz.jobStore.isClustered", String.valueOf(schedulerConfig.isClustered()));
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
    if (scheduler == null) {
      return new Date();
    }

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
    if (scheduler == null) {
      return true;
    }

    if (groupName != null && jobName != null) {
      try {
        return scheduler.deleteJob(new JobKey(jobName, groupName));
      } catch (SchedulerException ex) {
        logger.error(format("Couldn't delete cron job [%s %s] ", groupName, jobName), ex);
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
  public Boolean checkExists(String jobName, String groupName) {
    if (scheduler == null) {
      return true;
    }
    if (groupName != null && jobName != null) {
      JobKey jobKey = new JobKey(jobName, groupName);
      try {
        return scheduler.checkExists(jobKey);
      } catch (SchedulerException e) {
        logger.error("Couldn't check for cron for trigger {}", jobKey);
      }
    }
    return false;
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

  @Override
  public void onConfigChange(List<ConfigChangeEvent> events) {
    logger.info("onConfigChange {}", events);

    if (scheduler != null) {
      if (events.contains(ConfigChangeEvent.PrimaryChanged)) {
        ConfigurationController configurationController = injector.getInstance(Key.get(ConfigurationController.class));
        try {
          if (configurationController.isPrimary()) {
            scheduler.start();
          } else {
            scheduler.standby();
          }
        } catch (SchedulerException e) {
          logger.error("Error updating scheduler.", e);
        }
      }
    }
  }
}
