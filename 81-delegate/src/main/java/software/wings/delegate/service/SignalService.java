package software.wings.delegate.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by peeyushaggarwal on 1/4/17.
 */
@Singleton
public class SignalService {
  private static final Logger logger = LoggerFactory.getLogger(SignalService.class);

  private AtomicReference<State> state = new AtomicReference<>(State.RUNNING);

  @Inject private DelegateService delegateService;

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
