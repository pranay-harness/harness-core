package software.wings.watcher.service;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by brett on 10/27/17
 */
public interface UpgradeService {
  void doUpgrade(String version, String newVersion) throws IOException, TimeoutException, InterruptedException;
}
