package io.harness.maintenance;

import static io.harness.threading.Morpheus.sleep;
import static java.util.Collections.synchronizedSet;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.hazelcast.core.HazelcastInstance;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class MaintenanceController implements Managed {
  private static final Logger logger = LoggerFactory.getLogger(MaintenanceController.class);

  private static final String MAINTENANCE = "maintenance";
  private static final String SHUTDOWN = "shutdown";

  private static Boolean forceMaintenance;
  private static final AtomicBoolean maintenance = new AtomicBoolean(true);
  private static final AtomicBoolean shutdown = new AtomicBoolean(false);

  public static void forceMaintenance(boolean force) {
    synchronized (logger) {
      if (forceMaintenance == null || forceMaintenance != force) {
        logger.info("Setting forced maintenance {}", force);
        forceMaintenance = force;
      }
    }
  }

  public static void resetForceMaintenance() {
    synchronized (logger) {
      logger.info("Un-setting forced maintenance");
      forceMaintenance = null;
    }
  }

  public static boolean isMaintenance() {
    return forceMaintenance != null ? forceMaintenance : maintenance.get();
  }

  @Inject private ExecutorService executorService;
  @Inject private HazelcastInstance hazelcastInstance;

  private final AtomicBoolean running = new AtomicBoolean(true);
  private final Set<MaintenanceListener> maintenanceListeners = synchronizedSet(new HashSet<>());

  public void register(MaintenanceListener listener) {
    maintenanceListeners.add(listener);
  }

  @Override
  public void start() {
    executorService.submit(() -> {
      while (running.get()) {
        boolean isShutdown = new File(SHUTDOWN).exists();
        if (shutdown.getAndSet(isShutdown) != isShutdown) {
          logger.info("Shutdown started. Leaving hazelcast cluster.");
          hazelcastInstance.shutdown();
        }
        boolean isMaintenance =
            forceMaintenance != null ? forceMaintenance : new File(MAINTENANCE).exists() || isShutdown;
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
  public void stop() {
    running.set(false);
  }
}
