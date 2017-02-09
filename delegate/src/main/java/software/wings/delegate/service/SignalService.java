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

  AtomicReference<State> state = new AtomicReference<>(State.RUNNING);

  @Inject private DelegateService delegateService;

  public void pause() {
    if (state.compareAndSet(State.RUNNING, State.PAUSE)) {
      logger.debug("Setting state to pause from running");
      delegateService.pause();
    }
  }

  public void resume() {
    if (state.compareAndSet(State.PAUSE, State.RUNNING)) {
      logger.debug("Setting state to running from pause");
      delegateService.resume();
    }
    if (state.compareAndSet(State.PAUSED, State.RUNNING)) {
      logger.debug("Setting state to running from paused");
      delegateService.resume();
    }
  }

  public void stop() {
    state.set(State.STOP);
    logger.info("Setting state to stopped");
    delegateService.stop();
  }

  public void paused() {
    if (state.compareAndSet(State.PAUSE, State.PAUSED)) {
      logger.debug("Setting state to paused from pause");
    }
  }

  public boolean shouldStop() {
    return state.get() == State.STOP;
  }

  public boolean shouldRun() {
    return state.get() == State.RUNNING;
  }

  public enum State { RUNNING, PAUSE, PAUSED, STOP }
}
