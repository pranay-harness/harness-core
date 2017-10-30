package software.wings.watcher.service;

import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;

/**
 * Created by brett on 10/26/17
 */
@Singleton
public class SignalService {
  private static final Logger logger = LoggerFactory.getLogger(SignalService.class);

  private AtomicReference<State> state = new AtomicReference<>(State.RUNNING);

  @Inject private WatcherService watcherService;

  void resume() {
    if (state.compareAndSet(State.PAUSE, State.RUNNING)) {
      logger.info("[Old] Setting state to running from pause");
      watcherService.resume();
      logger.info("[Old] Watcher resumed");
    }
    if (state.compareAndSet(State.PAUSED, State.RUNNING)) {
      logger.info("[Old] Setting state to running from paused");
      watcherService.resume();
      logger.info("[Old] Watcher running");
    }
  }

  void stop() {
    state.set(State.STOP);
    logger.info("[Old] Setting state to stopped");
    watcherService.stop();
    logger.info("[Old] Watcher stopped");
  }

  public enum State { RUNNING, PAUSE, PAUSED, STOP }
}
