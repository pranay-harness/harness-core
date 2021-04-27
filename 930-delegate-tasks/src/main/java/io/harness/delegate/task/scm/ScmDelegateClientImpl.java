package io.harness.delegate.task.scm;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import com.google.inject.Singleton;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import java.io.IOException;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
/**
 * Add scm on delegate over all the methods for interception.
 */
public class ScmDelegateClientImpl implements ScmDelegateClient {
  // Caller code eg:
  //    processScmRequest( c->scmServiceClient.listFiles(connector,xyz,abc,SCMGrpc.newBlockingStub(c)));

  @Override
  public <R> R processScmRequest(Function<Channel, R> functor) {
    try {
      ScmUnixManager scmUnixManager = null;
      try {
        scmUnixManager = new ScmUnixManager();
        final ManagedChannel channel = scmUnixManager.getChannel();
        final R result = functor.apply(channel);
        // (Todo) : Close SCM Manager in background
        scmUnixManager.close();
        return result;
      } catch (IOException ioException) {
        // handle it here
        return null;
      }
    } catch (Exception e) {
      throw new InvalidRequestException("");
    }
  }
}