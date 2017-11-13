package software.wings.watcher.service;

import software.wings.utils.message.Message;

/**
 * Created by brett on 10/26/17
 */
public interface WatcherService {
  void run(boolean upgrade);

  void stop();

  Message waitForIncomingMessage(String messageName, long timeout);
}
