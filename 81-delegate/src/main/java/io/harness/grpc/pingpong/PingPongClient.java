package io.harness.grpc.pingpong;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.service.DelegateServiceImpl;
import io.harness.event.Ping;
import io.harness.event.PingPongServiceGrpc.PingPongServiceBlockingStub;
import io.harness.grpc.utils.HTimestamps;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class PingPongClient extends AbstractScheduledService {
  private final PingPongServiceBlockingStub pingPongServiceBlockingStub;

  @Inject
  public PingPongClient(PingPongServiceBlockingStub pingPongServiceBlockingStub) {
    this.pingPongServiceBlockingStub = pingPongServiceBlockingStub;
  }

  @Override
  protected void runOneIteration() {
    try {
      Instant timestamp = Instant.now();
      Ping ping = Ping.newBuilder()
                      .setDelegateId(DelegateServiceImpl.getDelegateId())
                      .setPingTimestamp(HTimestamps.fromInstant(timestamp))
                      .build();
      pingPongServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).tryPing(ping);
      logger.info("Ping at {} successful", timestamp);
    } catch (Exception e) {
      logger.error("Ping failed", e);
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(5, 5, TimeUnit.MINUTES);
  }
}
