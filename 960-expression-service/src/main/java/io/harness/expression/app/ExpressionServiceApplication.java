/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
