package software.wings.scheduler;

import com.google.inject.Injector;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import software.wings.app.GuiceQuartzJobFactory;
import software.wings.beans.ErrorCodes;
import software.wings.exception.WingsException;

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

  @Inject
  public CronScheduler(Injector injector) {
    this.injector = injector;
    this.scheduler = createScheduler(getDefaultProperties());
  }

  private Scheduler createScheduler(Properties properties) {
    try {
      StdSchedulerFactory factory = new StdSchedulerFactory();
      factory.initialize(properties);
      Scheduler scheduler = factory.getScheduler();
      scheduler.setJobFactory(injector.getInstance(GuiceQuartzJobFactory.class));
      scheduler.start();
      return scheduler;
    } catch (SchedulerException e) {
      throw new WingsException(ErrorCodes.UNKNOWN_ERROR, "message", "Could not initialize cron scheduler");
    }
  }

  private static Properties getDefaultProperties() { // TODO: read from properties file
    Properties props = new Properties();
    props.setProperty("org.quartz.jobStore.class", "com.novemberain.quartz.mongodb.DynamicMongoDBJobStore");
    props.setProperty("org.quartz.jobStore.mongoUri", "mongodb://localhost:27017");
    props.setProperty("org.quartz.scheduler.idleWaitTime", "1000");
    props.setProperty("org.quartz.jobStore.dbName", "wings");
    props.setProperty("org.quartz.threadPool.threadCount", "1");
    props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
    props.setProperty("org.quartz.plugin.triggHistory.class", "org.quartz.plugins.history.LoggingTriggerHistoryPlugin");
    props.setProperty("org.quartz.plugin.jobHistory.class", "org.quartz.plugins.history.LoggingJobHistoryPlugin");
    return props;
  }

  public Scheduler getScheduler() {
    return scheduler;
  }
}
