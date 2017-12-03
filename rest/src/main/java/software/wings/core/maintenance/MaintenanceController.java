package software.wings.core.maintenance;

import static java.util.Collections.synchronizedSet;

import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.common.Constants;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by brett on 9/15/17
 */
@Singleton
public class MaintenanceController implements Managed {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private static boolean forceMaintenanceOff = false;
  private static final AtomicBoolean maintenance = new AtomicBoolean(true);

  public static void forceMaintenanceOff() {
    forceMaintenanceOff = true;
  }

  public static boolean isMaintenance() {
    return !forceMaintenanceOff && maintenance.get();
  }

  @Inject private ExecutorService executorService;

  private final AtomicBoolean running = new AtomicBoolean(true);
  private final Set<MaintenanceListener> maintenanceListeners = synchronizedSet(new HashSet<>());

  public void register(MaintenanceListener listener) {
    maintenanceListeners.add(listener);
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#start()
   */
  @Override
  public void start() throws Exception {
    executorService.submit(() -> {
      while (running.get()) {
        try {
          boolean isMaintenance = new File(Constants.MAINTENANCE).exists();
          boolean previous = maintenance.getAndSet(isMaintenance);
          if (isMaintenance != previous) {
            logger.info("{} maintenance mode", isMaintenance ? "Entering" : "Leaving");
            synchronized (maintenanceListeners) {
              maintenanceListeners.forEach(listener
                  -> executorService.submit(
                      isMaintenance ? listener::onEnterMaintenance : listener::onLeaveMaintenance));
            }
          }
          Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#stop()
   */
  @Override
  public void stop() throws Exception {
    running.set(false);
  }
}
