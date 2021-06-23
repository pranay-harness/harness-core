package io.harness.delegate.task.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
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
            throw e;
          }
        }
      }
    } finally {
      if (retryCount <= 20) {
        log.info(format("Succeeded processing scm request with %s retries", retryCount));
      }
      if (channel != null) {
        channel.shutdown();
      }
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
}