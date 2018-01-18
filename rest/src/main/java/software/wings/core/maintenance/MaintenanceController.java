package software.wings.core.maintenance;

import static io.harness.threading.Morpheus.sleep;
import static java.util.Collections.synchronizedSet;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.common.Constants;

import java.io.File;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by brett on 9/15/17
 */
@Singleton
public class MaintenanceController implements Managed {
  private static final Logger logger = LoggerFactory.getLogger(MaintenanceController.class);

  private static boolean forceMaintenanceOff;
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
        boolean isMaintenance = new File(Constants.MAINTENANCE).exists();
        if (maintenance.getAndSet(isMaintenance) != isMaintenance) {
          logger.info("{} maintenance mode", isMaintenance ? "Entering" : "Leaving");
          synchronized (maintenanceListeners) {
            maintenanceListeners.forEach(listener
                -> executorService.submit(isMaintenance ? listener::onEnterMaintenance : listener::onLeaveMaintenance));
          }
        }
        sleep(Duration.ofSeconds(1));
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
