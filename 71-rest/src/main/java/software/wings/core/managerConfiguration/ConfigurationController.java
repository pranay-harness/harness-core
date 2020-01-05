package software.wings.core.managerConfiguration;

import static io.harness.threading.Morpheus.sleep;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections.MapUtils.synchronizedMap;
import static software.wings.beans.ManagerConfiguration.Builder.aManagerConfiguration;
import static software.wings.beans.ManagerConfiguration.MATCH_ALL_VERSION;
import static software.wings.core.managerConfiguration.ConfigChangeEvent.PrimaryChanged;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.dropwizard.lifecycle.Managed;
import io.harness.queue.QueueController;
import io.harness.version.VersionInfoManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.StringUtils;
import software.wings.beans.ManagerConfiguration;
import software.wings.dl.WingsPersistence;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
@Slf4j
public class ConfigurationController implements Managed, QueueController {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private ExecutorService executorService;

  private static final long DEFAULT_POLL_INTERVAL_MILLIS = 5000;
  private final AtomicBoolean running = new AtomicBoolean(true);
  private final Map<ConfigChangeListener, List<ConfigChangeEvent>> configChangeListeners =
      synchronizedMap(new HashMap<>());
  private final AtomicBoolean primary = new AtomicBoolean(true);
  private final AtomicReference<String> primaryVersion = new AtomicReference<>(MATCH_ALL_VERSION);
  private long pollIntervalInMillis;

  public ConfigurationController() {
    this.pollIntervalInMillis = DEFAULT_POLL_INTERVAL_MILLIS;
  }

  public ConfigurationController(long pollIntervalInMillis) {
    this.pollIntervalInMillis = pollIntervalInMillis;
  }

  public void register(ConfigChangeListener listener, List<ConfigChangeEvent> configChangeEvents) {
    configChangeListeners.put(listener, configChangeEvents);
  }

  @Override
  public void start() {
    executorService.submit(this ::run);
  }

  @Override
  public void stop() {
    running.set(false);
  }

  @Override
  public boolean isPrimary() {
    return primary.get();
  }

  @Override
  public boolean isNotPrimary() {
    return !primary.get();
  }

  public String getPrimaryVersion() {
    return primaryVersion.get();
  }

  private void run() {
    while (running.get()) {
      ManagerConfiguration managerConfiguration = wingsPersistence.createQuery(ManagerConfiguration.class).get();
      if (managerConfiguration == null) {
        managerConfiguration = aManagerConfiguration().withPrimaryVersion(MATCH_ALL_VERSION).build();
        wingsPersistence.save(managerConfiguration);
      }

      if (!StringUtils.equals(primaryVersion.get(), managerConfiguration.getPrimaryVersion())) {
        primaryVersion.set(managerConfiguration.getPrimaryVersion());
      }

      boolean isPrimary = StringUtils.equals(MATCH_ALL_VERSION, managerConfiguration.getPrimaryVersion())
          || StringUtils.equals(
                 versionInfoManager.getVersionInfo().getVersion(), managerConfiguration.getPrimaryVersion());

      if (primary.getAndSet(isPrimary) != isPrimary) {
        logger.info("{} primary mode", isPrimary ? "Entering" : "Leaving");
        synchronized (configChangeListeners) {
          configChangeListeners.forEach((k, v) -> executorService.submit(() -> {
            if (configChangeListeners.get(k).contains(PrimaryChanged)) {
              k.onConfigChange(singletonList(PrimaryChanged));
            }
          }));
        }
      }
      waitForSometime();
    }
  }

  private void waitForSometime() {
    sleep(Duration.ofMillis(pollIntervalInMillis));
  }
}
