/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.grpc.pingpong;

import io.harness.grpc.utils.HTimestamps;
import io.harness.pingpong.DelegateServicePingPongGrpc;
import io.harness.pingpong.PingDelegateService;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.ProcessIdUtil;

@Slf4j
public class DelegateServicePingPongClient extends AbstractScheduledService {
  private static final String PROCESS_ID = ProcessIdUtil.getProcessId();
  private DelegateServicePingPongGrpc.DelegateServicePingPongBlockingStub pingPongServiceBlockingStub;

  private String version;

  @Inject
  public DelegateServicePingPongClient(
      DelegateServicePingPongGrpc.DelegateServicePingPongBlockingStub pingPongServiceBlockingStub, String version) {
    this.pingPongServiceBlockingStub = pingPongServiceBlockingStub;
    this.version = version;
  }

  @Override
  protected void runOneIteration() throws Exception {
    try {
      Instant timestamp = Instant.now();

      PingDelegateService ping = PingDelegateService.newBuilder()
                                     .setDelegateId("UNREGISTERED")
                                     .setPingTimestamp(HTimestamps.fromInstant(timestamp))
                                     .setProcessId(PROCESS_ID)
                                     .setVersion(version)
                                     .build();
      pingPongServiceBlockingStub.withDeadlineAfter(5, TimeUnit.SECONDS).tryPing(ping);
      log.info("Ping at {} successful", timestamp);
    } catch (Exception e) {
      log.error("Ping failed", e);
    }
  }

  @Override
  protected Scheduler scheduler() {
    return Scheduler.newFixedDelaySchedule(5, 5, TimeUnit.MINUTES);
  }
}
