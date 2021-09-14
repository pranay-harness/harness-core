/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.logging;

import com.google.common.util.concurrent.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * For logging state transitions of a guava {@link Service}
 *
 * Add in service constructor as addListener(new LoggingListener(this), MoreExecutors.directExecutor());
 */
@Slf4j
public class LoggingListener extends Service.Listener {
  private final Service service;
  public LoggingListener(Service service) {
    this.service = service;
  }

  @Override
  public void starting() {
    super.starting();
    log.info("Service {} starting", service);
  }

  @Override
  public void running() {
    super.running();
    log.info("Service {} running", service);
  }

  @Override
  public void stopping(Service.State from) {
    super.stopping(from);
    log.info("Service {} stopping from {}", service, from);
  }

  @Override
  public void terminated(Service.State from) {
    super.terminated(from);
    log.info("Service {} terminated from {}", service, from);
  }

  @Override
  public void failed(Service.State from, Throwable failure) {
    super.failed(from, failure);
    log.error("Service {} failed from {} with error", service, from, failure);
  }
}
