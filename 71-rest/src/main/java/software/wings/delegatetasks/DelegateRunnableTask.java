package software.wings.delegatetasks;

import io.harness.task.protocol.ResponseData;

/**
 * Created by peeyushaggarwal on 12/7/16.
 */
public interface DelegateRunnableTask extends Runnable {
  ResponseData run(Object[] parameters);

  //  boolean canConnect(Object[] parameters);
}
