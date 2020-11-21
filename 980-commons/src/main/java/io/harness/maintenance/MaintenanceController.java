package io.harness.maintenance;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.threading.Morpheus.sleep;

import io.harness.annotations.dev.OwnedBy;
import io.harness.observer.Subject;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import java.io.File;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Singleton
@Slf4j
public class MaintenanceController implements Managed {
  private static final String MAINTENANCE_FILENAME = "maintenance";
  private static final String SHUTDOWN_FILENAME = "shutdown";

  private static Boolean forceMaintenance;
  private static final AtomicBoolean maintenance = new AtomicBoolean(true);
  private static final AtomicBoolean shutdown = new AtomicBoolean(false);

  public static void forceMaintenance(boolean force) {
    synchronized (log) {
      if (forceMaintenance == null || forceMaintenance != force) {
        log.info("Setting forced maintenance {}", force);
        forceMaintenance = force;
      }
    }
  }

  public static void resetForceMaintenance() {
    synchronized (log) {
      log.info("Un-setting forced maintenance");
      forceMaintenance = null;
    }
  }

  public static boolean getMaintenanceFlag() {
    return forceMaintenance != null ? forceMaintenance : maintenance.get();
  }

  @Inject private ExecutorService executorService;

  private final AtomicBoolean running = new AtomicBoolean(true);
  private final Subject<MaintenanceListener> maintenanceListenerSubject = new Subject<>();

  public void register(MaintenanceListener listener) {
    maintenanceListenerSubject.register(listener);
  }

  @Override
  public void start() {
    executorService.submit(() -> {
      while (running.get()) {
        boolean isShutdown = new File(SHUTDOWN_FILENAME).exists();
        if (shutdown.getAndSet(isShutdown) != isShutdown) {
          maintenanceListenerSubject.fireInform(MaintenanceListener::onShutdown);
        }
        boolean isMaintenance =
            forceMaintenance != null ? forceMaintenance : new File(MAINTENANCE_FILENAME).exists() || isShutdown;
        if (maintenance.getAndSet(isMaintenance) != isMaintenance) {
          log.info("{} maintenance mode", isMaintenance ? "Entering" : "Leaving");
          maintenanceListenerSubject.fireInform(
              isMaintenance ? MaintenanceListener::onEnterMaintenance : MaintenanceListener::onLeaveMaintenance);
        }
        sleep(Duration.ofSeconds(1));
      }
    });
  }

  /* (non-Javadoc)
   * @see io.dropwizard.lifecycle.Managed#stop()
   */
  @Override
  public void stop() {
    running.set(false);
  }
}
