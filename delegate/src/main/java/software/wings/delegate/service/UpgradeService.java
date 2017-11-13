package software.wings.delegate.service;

import software.wings.beans.DelegateScripts;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by peeyushaggarwal on 1/4/17.
 */
public interface UpgradeService {
  void doUpgrade(DelegateScripts delegateScripts) throws IOException, TimeoutException, InterruptedException;

  void doRestart() throws IOException, TimeoutException, InterruptedException;
}
