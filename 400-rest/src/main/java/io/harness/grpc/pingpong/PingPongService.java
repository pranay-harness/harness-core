package io.harness.grpc.pingpong;

import io.grpc.stub.StreamObserver;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.event.Ping;
import io.harness.event.PingPongServiceGrpc.PingPongServiceImplBase;
import io.harness.event.Pong;
import io.harness.grpc.utils.HTimestamps;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import lombok.extern.slf4j.Slf4j;

import static io.harness.grpc.auth.DelegateAuthServerInterceptor.ACCOUNT_ID_CTX_KEY;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

@Slf4j
public class PingPongService extends PingPongServiceImplBase {
  @Override
  public void tryPing(Ping ping, StreamObserver<Pong> responseObserver) {
    try (AutoLogContext ignore1 = new AccountLogContext(ACCOUNT_ID_CTX_KEY.get(), OVERRIDE_ERROR);
         AutoLogContext ignore2 = new DelegateLogContext(ping.getDelegateId(), OVERRIDE_ERROR)) {
      log.info("Ping at {} received from delegate with processId: {}, version: {}",
          HTimestamps.toInstant(ping.getPingTimestamp()), ping.getProcessId(), ping.getVersion());
      responseObserver.onNext(Pong.newBuilder().build());
      responseObserver.onCompleted();
    }
  }
}
