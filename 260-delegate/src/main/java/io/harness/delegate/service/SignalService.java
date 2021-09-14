/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.service;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class SignalService {
  private AtomicReference<State> state = new AtomicReference<>(State.RUNNING);

  @Inject private DelegateAgentService delegateService;

  void pause() {
    if (state.compareAndSet(State.RUNNING, State.PAUSE)) {
      log.info("[Old] Setting state to pause from running");
      delegateService.pause();
      log.info("[Old] Delegate paused");
    }
  }

  void stop() {
    state.set(State.STOP);
    log.info("[Old] Setting state to stopped");
    delegateService.stop();
    log.info("[Old] Delegate stopped");
  }

  public enum State { RUNNING, PAUSE, STOP }
}
