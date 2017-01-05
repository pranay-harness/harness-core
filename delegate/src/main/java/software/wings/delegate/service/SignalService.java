package software.wings.delegate.service;

import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by peeyushaggarwal on 1/4/17.
 */
@Singleton
public class SignalService {
  private static final Logger logger = LoggerFactory.getLogger(SignalService.class);

  AtomicReference<State> state = new AtomicReference<>(State.RUNNING);

  public void pause() {
    if (state.compareAndSet(State.RUNNING, State.PAUSE)) {
      logger.debug("Setting state to pause from running");
    }
  }

  public void resume() {
    if (state.compareAndSet(State.PAUSE, State.RUNNING)) {
      logger.debug("Setting state to running from pause");
    }
    if (state.compareAndSet(State.PAUSED, State.RUNNING)) {
      logger.debug("Setting state to running from paused");
    }
  }

  public void stop() {
    state.set(State.STOP);
    logger.info("Setting state to stopped");
  }

  public void paused() {
    if (state.compareAndSet(State.PAUSE, State.PAUSED)) {
      logger.debug("Setting state to paused from pause");
    }
  }

  public void waitForPause() {
    while (state.get() != State.PAUSED) {
      LockSupport.parkNanos(0);
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
