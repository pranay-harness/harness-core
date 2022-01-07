/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
