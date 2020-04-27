package io.harness.delegate.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

@Singleton
@Slf4j
public class SignalService {
  private AtomicReference<State> state = new AtomicReference<>(State.RUNNING);

  @Inject private DelegateAgentService delegateService;

  void pause() {
    if (state.compareAndSet(State.RUNNING, State.PAUSE)) {
      logger.info("[Old] Setting state to pause from running");
      delegateService.pause();
      logger.info("[Old] Delegate paused");
    }
  }

  void stop() {
    state.set(State.STOP);
    logger.info("[Old] Setting state to stopped");
    delegateService.stop();
    logger.info("[Old] Delegate stopped");
  }

  public enum State { RUNNING, PAUSE, STOP }
}
