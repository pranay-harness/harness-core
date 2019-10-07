package io.harness.grpc.pingpong;

import static io.harness.grpc.auth.DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import io.grpc.stub.StreamObserver;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.event.Ping;
import io.harness.event.PingPongServiceGrpc.PingPongServiceImplBase;
import io.harness.event.Pong;
import io.harness.grpc.utils.HTimestamps;
import io.harness.logging.AutoLogContext;
import lombok.extern.slf4j.Slf4j;
import software.wings.service.impl.AccountLogContext;

@Slf4j
public class PingPongService extends PingPongServiceImplBase {
  @Override
  public void tryPing(Ping ping, StreamObserver<Pong> responseObserver) {
    try (AutoLogContext ignore1 = new AccountLogContext(ACCOUNT_ID_CTX_KEY.get(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(ping.getDelegateId(), OVERRIDE_ERROR)) {
      logger.info("Ping at {} received", HTimestamps.toInstant(ping.getPingTimestamp()));
      responseObserver.onNext(Pong.newBuilder().build());
      responseObserver.onCompleted();
    }
  }
}
