package software.wings.delegate.service;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public interface DelegateService {
  void run(boolean watched);

  void pause();

  void stop();
}
