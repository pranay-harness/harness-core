package io.harness.commandlibrary.server;

import com.google.inject.Injector;
import com.google.inject.Module;

import io.dropwizard.Configuration;
import io.harness.commandlibrary.server.app.CommandLibraryServerConfig;
import io.harness.commandlibrary.server.app.CommandLibraryServerModule;
import io.harness.factory.ClosingFactoryModule;
import io.harness.mongo.MongoConfig;
import io.harness.testlib.module.TestMongoModule;
import software.wings.app.CommandLibrarySharedModule;
import software.wings.rules.WingsRule;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class CommandLibraryServerTestRule extends WingsRule {
  @Override
  protected Configuration getConfiguration(List<Annotation> annotations, String dbName) {
    final CommandLibraryServerConfig configuration = new CommandLibraryServerConfig();
    configuration.setMongoConnectionFactory(
        MongoConfig.builder().uri(System.getProperty("mongoUri", "mongodb://localhost:27017/" + dbName)).build());

    return configuration;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(mongoTypeModule(annotations));

    modules.addAll(new TestMongoModule().cumulativeDependencies());
    modules.add(new CommandLibraryServerModule((CommandLibraryServerConfig) configuration));
    modules.add(new CommandLibrarySharedModule(false));
    return modules;
  }

  @Override
  protected void registerObservers() {
    //    do nothing
  }

  @Override
  protected void registerScheduledJobs(Injector injector) {
    //    do nothing
  }

  @Override
  protected void addQueueModules(List<Module> modules) {
    //    do nothing here
  }

  @Override
  protected void after(List<Annotation> annotations) {
    log().info("Stopping servers...");
    closingFactory.stopServers();
  }
}
