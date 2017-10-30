package software.wings.watcher.service;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by brett on 10/26/17
 */
@Singleton
public class WatcherServiceImpl implements WatcherService {
  private final Logger logger = LoggerFactory.getLogger(WatcherServiceImpl.class);
  private final Object waiter = new Object();
  @Inject @Named("upgradeExecutor") private ScheduledExecutorService upgradeExecutor;
  @Inject @Named("watchExecutor") private ScheduledExecutorService watchExecutor;
  @Inject private ExecutorService executorService;
  @Inject private UpgradeService upgradeService;
  @Inject private Injector injector;

  @Override
  public void run(boolean upgrade) {
    try {
      if (upgrade) {
        logger.info("[New] Upgraded watcher process started. Sending confirmation.");
        System.out.println("watchstarted"); // Don't remove this. It is used as message in upgrade flow.
      } else {
        logger.info("Watcher process started");
      }

      startUpgradeCheck(getVersion());
      startWatcher();

      if (upgrade) {
        logger.info("[New] Watcher upgraded.");
      } else {
        logger.info("Watcher started.");
      }

      synchronized (waiter) {
        waiter.wait();
      }

    } catch (Exception e) {
      logger.error("Exception while starting/running watcher", e);
    }
  }

  @Override
  public void resume() {}

  @Override
  public void stop() {}

  private void startUpgradeCheck(String version) {
    upgradeExecutor.scheduleWithFixedDelay(() -> {
      logger.info("Checking for upgrade");
      try {
        // TODO - Check S3 for upgrade availability
        String newVersion = "1.0.1-DEV";
        if (false) {
          logger.info("[Old] Upgrading watcher.");
          upgradeService.doUpgrade(getVersion(), newVersion);
        } else {
          logger.info("Watcher up to date");
        }
      } catch (Exception e) {
        logger.error("[Old] Exception while checking for upgrade", e);
      }
    }, 1, 1, TimeUnit.MINUTES);
  }

  private void startWatcher() {
    watchExecutor.scheduleWithFixedDelay(
        ()
            -> {
                // TODO - watch delegate
            },
        1, 5, TimeUnit.SECONDS);
  }

  private String getVersion() {
    return System.getProperty("version", "1.0.0-DEV");
  }
}
