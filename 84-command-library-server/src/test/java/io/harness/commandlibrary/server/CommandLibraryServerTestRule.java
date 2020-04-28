package io.harness.commandlibrary.server;

import com.google.inject.Injector;
import com.google.inject.Module;

import com.mongodb.MongoClient;
import io.dropwizard.Configuration;
import io.harness.commandlibrary.server.app.CommandLibraryServerConfig;
import io.harness.commandlibrary.server.app.CommandLibraryServerModule;
import io.harness.module.TestMongoModule;
import io.harness.mongo.MongoConfig;
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
  protected List<Module> getRequiredModules(
      Configuration configuration, MongoClient locksMongoClient, String locksDatabase) {
    final ArrayList<Module> modules = new ArrayList<>(new TestMongoModule(datastore).cumulativeDependencies());
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
