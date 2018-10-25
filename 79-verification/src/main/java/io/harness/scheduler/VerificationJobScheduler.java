package io.harness.scheduler;

import static io.harness.persistence.HPersistence.DEFAULT_STORE;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import io.harness.app.VerificationServiceConfiguration;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.persistence.ReadPref;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.scheduler.AbstractQuartzScheduler;

import java.util.Properties;

public class VerificationJobScheduler extends AbstractQuartzScheduler {
  private static final Logger logger = LoggerFactory.getLogger(VerificationJobScheduler.class);

  /**
   * Instantiates a new Cron scheduler.
   *
   * @param injector      the injector
   * @param configuration the configuration
   */
  @Inject
  private VerificationJobScheduler(Injector injector, VerificationServiceConfiguration configuration) {
    super(injector, configuration.getSchedulerConfig(), configuration.getMongoConnectionFactory());
  }

  @Override
  protected void setupScheduler() {
    try {
      final String prefix = "quartz_verification";
      final Properties properties = getDefaultProperties();
      properties.setProperty("org.quartz.jobStore.collectionPrefix", prefix);
      StdSchedulerFactory factory = new StdSchedulerFactory(properties);
      Scheduler scheduler = factory.getScheduler();

      // by default scheduler does not create all needed mongo indexes.
      // it is a bit hack but we are going to add them from here

      WingsPersistence wingsPersistence = injector.getInstance(Key.get(WingsMongoPersistence.class));

      final DBCollection triggers =
          wingsPersistence.getCollection(DEFAULT_STORE, ReadPref.NORMAL, prefix + "_triggers");
      BasicDBObject jobIdKey = new BasicDBObject("jobId", 1);
      triggers.createIndex(jobIdKey, null, false);

      BasicDBObject fireKeys = new BasicDBObject();
      fireKeys.append("state", 1);
      fireKeys.append("nextFireTime", 1);
      triggers.createIndex(fireKeys, "fire", false);

      scheduler.setJobFactory(injector.getInstance(InjectorJobFactory.class));
      scheduler.start();
      this.scheduler = scheduler;
    } catch (SchedulerException exception) {
      throw new WingsException(ErrorCode.UNKNOWN_ERROR, exception)
          .addParam("message", "Could not initialize cron scheduler");
    }
  }
}
