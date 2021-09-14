/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.testframework.framework;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import io.harness.filesystem.FileIo;
import io.harness.govern.IgnoreThrowable;
import io.harness.mock.server.MockServer;
import io.harness.resource.Project;
import io.harness.threading.Poller;

import com.google.inject.Singleton;
import io.restassured.http.ContentType;
import java.io.File;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class MockServerExecutor {
  private final MockServer mockServer = new MockServer();
  private static boolean failedAlready;
  public void ensureMockServer(Class<?> clazz) {
    if (!isHealthy()) {
      executeLocalMockServer(clazz);
    }
  }

  public void shutdownMockServer() {
    if (isHealthy()) {
      shutdown();
    }
  }

  @SneakyThrows
  private void executeLocalMockServer(Class<?> clazz) {
    if (failedAlready) {
      return;
    }

    String directoryPath = Project.rootDirectory(clazz);
    final File lockfile = new File(directoryPath, "mock");

    if (FileIo.acquireLock(lockfile, ofMinutes(2))) {
      try {
        if (isHealthy()) {
          return;
        }
        mockServer.start();
        Poller.pollFor(ofMinutes(1), ofSeconds(2), this::isHealthy);
      } catch (RuntimeException e) {
        failedAlready = true;
        throw e;
      } finally {
        FileIo.releaseLock(lockfile);
      }
    }
  }

  private boolean isHealthy() {
    try {
      Long status = Setup.mock().contentType(ContentType.JSON).get("/status").then().extract().as(Long.class);
      return status == 200;
    } catch (Exception e) {
      IgnoreThrowable.ignoredOnPurpose(e);
      return false;
    }
  }

  private void shutdown() {
    mockServer.stop();
  }
}
