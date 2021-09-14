/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.expression.app;

import io.harness.expression.configuration.util.ExpressionServiceConfigurationUtils;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

@Slf4j
public class ExpressionServiceApplication {
  public static void main(String[] args) {
    try {
      InputStream config = new FileInputStream(new File(FilenameUtils.getName(args[0])));
      ExpressionServiceConfiguration configuration =
          ExpressionServiceConfigurationUtils.getApplicationConfiguration(config);
      Injector injector = Guice.createInjector(
          new ExpressionGRPCServerModule(configuration.getConnectors(), configuration.getSecret()));
      ServiceManager serviceManager = injector.getInstance(ServiceManager.class).startAsync();
      serviceManager.awaitHealthy(); // wait for services to become healty

      Runtime.getRuntime().addShutdownHook(new Thread(() -> serviceManager.stopAsync().awaitStopped()));
      serviceManager.awaitStopped(); // need to stall main thread since grpc uses daemon-threads.
    } catch (IOException e) {
      log.error("Unable to read configuration file", e);
    }
  }
}
