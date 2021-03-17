package io.harness.pms.preflight.handler;

import io.harness.pms.preflight.entity.PreFlightEntity;
import io.harness.repositories.preflight.PreFlightRepository;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public class AsyncPreFlightHandler implements Runnable {
  private final PreFlightEntity entity;
  private final PreFlightRepository preFlightRepository;

  @Override
  public void run() {
    log.info("Handling event with id " + entity.getUuid());
  }
}
