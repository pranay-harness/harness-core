/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.commandlibrary.server;

import io.harness.commandlibrary.server.app.CommandLibraryServerConfig;
import io.harness.commandlibrary.server.app.CommandLibraryServerModule;
import io.harness.commandlibrary.server.beans.TagConfig;
import io.harness.factory.ClosingFactoryModule;
import io.harness.mongo.MongoConfig;
import io.harness.testlib.module.TestMongoModule;

import software.wings.app.CommandLibrarySharedModule;
import software.wings.rules.WingsRule;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.dropwizard.Configuration;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandLibraryServerTestRule extends WingsRule {
  @Override
  protected Configuration getConfiguration(List<Annotation> annotations, String dbName) {
    final CommandLibraryServerConfig configuration = new CommandLibraryServerConfig();
    configuration.setMongoConnectionFactory(
        MongoConfig.builder().uri(System.getProperty("mongoUri", "mongodb://localhost:27017/" + dbName)).build());
    configuration.setTagConfig(TagConfig.builder()
                                   .allowedTags(ImmutableSet.of("Azure", "Gcp", "Kubernetes", "Aws"))
                                   .importantTags(ImmutableSet.of("Azure", "Gcp", "Kubernetes", "Aws"))
                                   .build());
    return configuration;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(mongoTypeModule(annotations));
    modules.add(TestMongoModule.getInstance());
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
    log.info("Stopping servers...");
    closingFactory.stopServers();
  }
}
