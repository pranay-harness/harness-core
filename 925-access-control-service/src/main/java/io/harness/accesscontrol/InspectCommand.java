/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol;

import io.harness.metrics.MetricRegistryModule;
import io.harness.mongo.IndexManager;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import io.dropwizard.Application;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.mongodb.morphia.AdvancedDatastore;

public class InspectCommand<T extends io.dropwizard.Configuration> extends ConfiguredCommand<T> {
  private final Class<T> configurationClass;

  public InspectCommand(Application<T> application) {
    super("inspect", "Parses and validates the configuration file");
    this.configurationClass = application.getConfigurationClass();
  }

  @Override
  protected Class<T> getConfigurationClass() {
    return this.configurationClass;
  }
  @Override
  protected void run(Bootstrap<T> bootstrap, Namespace namespace, T configuration) {
    AccessControlConfiguration appConfig = (AccessControlConfiguration) configuration;
    appConfig.setMongoConfig(
        appConfig.getMongoConfig().toBuilder().indexManagerMode(IndexManager.Mode.INSPECT).build());
    MetricRegistry metricRegistry = new MetricRegistry();
    Injector injector =
        Guice.createInjector(AccessControlModule.getInstance(appConfig), new MetricRegistryModule(metricRegistry));
    injector.getInstance(Key.get(AdvancedDatastore.class, Names.named("primaryDatastore")));
  }
}
