package io.harness.rules;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Module;

import com.deftlabs.lock.mongo.DistributedLockSvc;
import io.dropwizard.Configuration;
import io.harness.VerificationBaseIntegrationTest;
import io.harness.VerificationTestModule;
import io.harness.app.VerificationQueueModule;
import io.harness.app.VerificationServiceConfiguration;
import io.harness.app.VerificationServiceModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import software.wings.rules.SetupScheduler;
import software.wings.rules.WingsRule;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Created by rsingh on 9/25/18.
 */
public class VerificationTestRule extends WingsRule {
  @Override
  protected Configuration getConfiguration(List<Annotation> annotations, String dbName) {
    VerificationServiceConfiguration configuration = new VerificationServiceConfiguration();
    configuration.setMongoConnectionFactory(
        MongoConfig.builder().uri(System.getProperty("mongoUri", "mongodb://localhost:27017/" + dbName)).build());
    configuration.getSchedulerConfig().setAutoStart(System.getProperty("setupScheduler", "false"));
    configuration.getSchedulerConfig().setSchedulerName("verification_scheduler");
    configuration.getSchedulerConfig().setInstanceId("verification");
    configuration.getSchedulerConfig().setThreadCount("15");
    if (annotations.stream().anyMatch(SetupScheduler.class ::isInstance)) {
      configuration.getSchedulerConfig().setAutoStart("true");
      if (fakeMongo) {
        configuration.getSchedulerConfig().setJobstoreclass(org.quartz.simpl.RAMJobStore.class.getCanonicalName());
      }
    }
    return configuration;
  }

  @Override
  protected List<Module> getRequiredModules(Configuration configuration, DistributedLockSvc distributedLockSvc) {
    return Lists.newArrayList(new MongoModule(datastore, datastore, distributedLockSvc),
        new VerificationServiceModule((VerificationServiceConfiguration) configuration), new VerificationTestModule());
  }

  @Override
  protected void addQueueModules(List<Module> modules) {
    modules.add(new VerificationQueueModule());
  }

  @Override
  protected void registerScheduledJobs(Injector injector) {
    // do nothing
  }

  @Override
  protected void registerObservers() {
    // do nothing
  }

  @Override
  protected void after(List<Annotation> annotations) {
    log().info("Stopping servers...");
    closingFactory.stopServers();

    try {
      if (mongodExecutable != null) {
        mongodExecutable.stop();
      }
    } catch (IllegalStateException ise) {
      // we are   swallowing this - couldn't kill the embedded mongod process, but we don't care
      log().info("Had issues stopping embedded mongod: {}", ise.getMessage());
    }
  }

  @Override
  protected boolean isIntegrationTest(Object target) {
    return target instanceof VerificationBaseIntegrationTest;
  }
}
