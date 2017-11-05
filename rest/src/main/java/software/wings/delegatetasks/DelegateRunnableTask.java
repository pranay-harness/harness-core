package software.wings.delegatetasks;

import software.wings.waitnotify.NotifyResponseData;

/**
 * Created by peeyushaggarwal on 12/7/16.
 */
public interface DelegateRunnableTask extends Runnable {
  NotifyResponseData run(Object[] parameters);

  //  boolean canConnect(Object[] parameters);
}
