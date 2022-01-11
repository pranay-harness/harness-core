/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.service.scm.ScmDelegateClient;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Add scm on delegate over all the methods for interception.
 */
@Singleton
@Slf4j
@OwnedBy(DX)
public class ScmDelegateClientImpl implements ScmDelegateClient {
  // Caller code eg:
  //    processScmRequest( c->scmServiceClient.listFiles(connector,xyz,abc,SCMGrpc.newBlockingStub(c)));

  @SneakyThrows
  @Override
  public <R> R processScmRequest(Function<Channel, R> functor) {
    int retryCount = 0;
    ManagedChannel channel = null;
    try (ScmUnixManager scmUnixManager = getScmService()) {
      while (retryCount <= 20) {
        try {
          channel = scmUnixManager.getChannel();
          return functor.apply(channel);
        } catch (StatusRuntimeException e) {
          if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
            if (++retryCount > 20) {
              throw new InvalidRequestException("Cannot start Scm Unix Manager", e);
            }
            Thread.sleep(100);
          } else {
            shutdownChannel(channel);
            throw e;
          }
        }
      }
    } finally {
      if (retryCount <= 20) {
        log.info(format("Succeeded processing scm request with %s retries", retryCount));
      }
      shutdownChannel(channel);
    }
    throw new InvalidRequestException("Cannot start Scm Unix Manager");
  }

  private ScmUnixManager getScmService() {
    String OS = System.getProperty("os.name").toLowerCase();
    log.info("Name of OS is {}", OS);
    try {
      if (OS.contains("mac")) {
        return new ScmMacOSManager();
      } else if (OS.contains("nux") || OS.contains("nix")) {
        return new ScmLinuxManager();
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Manager could not be created", e);
    }
    throw new InvalidRequestException("SCM on" + OS + "is not supported yet");
  }

  private void shutdownChannel(ManagedChannel channel) {
    if (channel != null) {
      try {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        log.error("Interrupted Exception while shutting down channel", e);
      }
      if (channel.isShutdown()) {
        log.info("scm channel successfully shut down");
      } else {
        log.error("Channel couldn't be shutdown");
      }
    }
  }
}
