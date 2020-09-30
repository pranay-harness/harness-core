package io.harness.rules;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.dropwizard.Configuration;
import io.harness.VerificationBaseIntegrationTest;
import io.harness.VerificationTestModule;
import io.harness.app.VerificationQueueModule;
import io.harness.app.VerificationServiceConfiguration;
import io.harness.app.VerificationServiceModule;
import io.harness.app.VerificationServiceSchedulerModule;
import io.harness.factory.ClosingFactoryModule;
import io.harness.mongo.MongoConfig;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.VerificationRegistrars;
import io.harness.serializer.morphia.VerificationMorphiaRegistrar;
import io.harness.testlib.RealMongo;
import io.harness.testlib.module.TestMongoModule;
import software.wings.rules.SetupScheduler;
import software.wings.rules.WingsRule;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
      if (!annotations.stream().anyMatch(RealMongo.class ::isInstance)) {
        configuration.getSchedulerConfig().setJobStoreClass(org.quartz.simpl.RAMJobStore.class.getCanonicalName());
      }
    }
    return configuration;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) {
    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(mongoTypeModule(annotations));

    modules.add(TestMongoModule.getInstance());
    modules.add(new VerificationServiceModule((VerificationServiceConfiguration) configuration));
    modules.add(new VerificationTestModule());
    modules.add(new VerificationServiceSchedulerModule((VerificationServiceConfiguration) configuration));
    return modules;
  }

  @Override
  protected Set<Class<? extends KryoRegistrar>> getKryoRegistrars() {
    return ImmutableSet.<Class<? extends KryoRegistrar>>builder()
        .addAll(super.getKryoRegistrars())
        .addAll(VerificationRegistrars.kryoRegistrars)
        .build();
  }

  @Override
  protected Set<Class<? extends MorphiaRegistrar>> getMorphiaRegistrars() {
    return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
        .addAll(super.getMorphiaRegistrars())
        .add(VerificationMorphiaRegistrar.class)
        .build();
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
