package io.harness.scheduler;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;

import com.google.inject.Injector;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientOptions.Builder;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import io.harness.maintenance.MaintenanceListener;
import io.harness.mongo.MongoModule;
import org.bson.Document;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Properties;

public class AbstractQuartzScheduler implements PersistentScheduler, MaintenanceListener {
  private static final Logger logger = LoggerFactory.getLogger(AbstractQuartzScheduler.class);

  protected Injector injector;
  protected Scheduler scheduler;

  private SchedulerConfig schedulerConfig;
  private String defaultMongoUri;

  public AbstractQuartzScheduler(Injector injector, SchedulerConfig schedulerConfig, String defaultMongoUri) {
    this.injector = injector;
    this.schedulerConfig = schedulerConfig;
    this.defaultMongoUri = defaultMongoUri;
  }

  private String getMongoUri() {
    if (isEmpty(schedulerConfig.getMongoUri())) {
      return defaultMongoUri;
    }
    return schedulerConfig.getMongoUri();
  }

  protected Scheduler createScheduler(Properties properties) throws SchedulerException {
    StdSchedulerFactory factory = new StdSchedulerFactory(properties);
    Scheduler scheduler = factory.getScheduler();

    // by default scheduler does not create all needed mongo indexes.
    // it is a bit hack but we are going to add them from here

    if (schedulerConfig.getJobStoreClass().equals("com.novemberain.quartz.mongodb.DynamicMongoDBJobStore")) {
      MongoClientURI uri =
          new MongoClientURI(getMongoUri(), MongoClientOptions.builder(MongoModule.mongoClientOptions));
      try (MongoClient mongoClient = new MongoClient(uri)) {
        final MongoDatabase database = mongoClient.getDatabase(uri.getDatabase());

        final String prefix = properties.getProperty("org.quartz.jobStore.collectionPrefix");

        final MongoCollection<Document> collection = database.getCollection(prefix + "_triggers");

        BasicDBObject jobIdKey = new BasicDBObject("jobId", 1);
        collection.createIndex(jobIdKey, new IndexOptions().background(false));

        BasicDBObject fireKeys = new BasicDBObject();
        fireKeys.append("state", 1);
        fireKeys.append("nextFireTime", 1);
        collection.createIndex(fireKeys, new IndexOptions().background(false).name("fire"));
      }
    }

    scheduler.setJobFactory(injector.getInstance(InjectorJobFactory.class));
    return scheduler;
  }

  protected Properties getDefaultProperties() {
    Properties props = new Properties();
    if (schedulerConfig.getJobStoreClass().equals("com.novemberain.quartz.mongodb.DynamicMongoDBJobStore")) {
      Builder mongoClientOptions = MongoClientOptions.builder()
                                       .connectTimeout(30000)
                                       .serverSelectionTimeout(90000)
                                       .maxConnectionIdleTime(600000)
                                       .connectionsPerHost(50);
      MongoClientURI uri = new MongoClientURI(getMongoUri(), mongoClientOptions);
      props.setProperty("org.quartz.jobStore.class", schedulerConfig.getJobStoreClass());
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
  public void onShutdown() {}

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
