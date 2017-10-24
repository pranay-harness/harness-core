package software.wings.delegate.service;

import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;

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

  void resume() {
    if (state.compareAndSet(State.PAUSE, State.RUNNING)) {
      logger.info("[Old] Setting state to running from pause");
      delegateService.resume();
      logger.info("[Old] Delegate resumed");
    }
    if (state.compareAndSet(State.PAUSED, State.RUNNING)) {
      logger.info("[Old] Setting state to running from paused");
      delegateService.resume();
      logger.info("[Old] Delegate running");
    }
  }

  void stop() {
    state.set(State.STOP);
    logger.info("[Old] Setting state to stopped");
    delegateService.stop();
    logger.info("[Old] Delegate stopped");
  }

  public enum State { RUNNING, PAUSE, PAUSED, STOP }
}
