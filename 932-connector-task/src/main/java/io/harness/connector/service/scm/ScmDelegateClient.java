package io.harness.connector.service.scm;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.grpc.Channel;
import java.util.function.Function;

@OwnedBy(HarnessTeam.DX)
public interface ScmDelegateClient {
  <R> R processScmRequest(Function<Channel, R> functor);
}
