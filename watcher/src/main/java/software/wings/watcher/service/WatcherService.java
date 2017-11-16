package software.wings.watcher.service;

import software.wings.utils.message.Message;

/**
 * Created by brett on 10/26/17
 */
public interface WatcherService {
  void run(boolean upgrade, boolean transition);

  void stop();

  void resume();

  Message waitForIncomingMessage(String messageName, long timeout);
}
