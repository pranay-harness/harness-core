package software.wings.delegate.service;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public interface DelegateService {
  void run(boolean upgrade, boolean restart);

  void pause();

  void resume();

  void stop();

  long getRunningTaskCount();

  void setAcquireTasks(boolean acquireTasks);
}
