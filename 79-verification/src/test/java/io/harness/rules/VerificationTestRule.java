package io.harness.rules;

import com.google.inject.Injector;
import com.google.inject.Module;

import com.mongodb.MongoClient;
import io.dropwizard.Configuration;
import io.harness.VerificationBaseIntegrationTest;
import io.harness.VerificationTestModule;
import io.harness.app.VerificationQueueModule;
import io.harness.app.VerificationServiceConfiguration;
import io.harness.app.VerificationServiceModule;
import io.harness.app.VerificationServiceSchedulerModule;
import io.harness.mongo.MongoConfig;
import io.harness.testlib.module.TestMongoModule;
import software.wings.rules.SetupScheduler;
import software.wings.rules.WingsRule;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
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
      if (mongoType == MongoType.FAKE) {
        configuration.getSchedulerConfig().setJobStoreClass(org.quartz.simpl.RAMJobStore.class.getCanonicalName());
      }
    }
    return configuration;
  }

  @Override
  protected List<Module> getRequiredModules(
      Configuration configuration, MongoClient locksMongoClient, String locksDatabase) {
    List<Module> modules = new ArrayList<>();
    modules.addAll(new TestMongoModule(datastore).cumulativeDependencies());
    modules.add(new VerificationServiceModule((VerificationServiceConfiguration) configuration));
    modules.add(new VerificationTestModule());
    modules.add(new VerificationServiceSchedulerModule((VerificationServiceConfiguration) configuration));
    return modules;
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
  }

  @Override
  protected boolean isIntegrationTest(Object target) {
    return target instanceof VerificationBaseIntegrationTest;
  }
}
