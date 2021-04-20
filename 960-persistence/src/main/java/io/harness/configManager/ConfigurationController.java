package io.harness.configManager;

import static io.harness.configManager.PrimaryVersion.MATCH_ALL_VERSION;

import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.version.VersionInfoManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Singleton
@Slf4j
public class ConfigurationController implements QueueController {
  @Inject private HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private ExecutorService executorService;

  private final AtomicBoolean primary = new AtomicBoolean(true);
  private final AtomicBoolean running = new AtomicBoolean(true);
  private final AtomicReference<String> primaryVersion = new AtomicReference<>(MATCH_ALL_VERSION);

  public ConfigurationController() {}

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

  public void run() {
    PrimaryVersion primaryVersion = persistence.createQuery(PrimaryVersion.class).get();

    if (!StringUtils.equals(this.primaryVersion.get(), primaryVersion.getPrimaryVersion())) {
      log.info("Changing primary version from {} to {}", this.primaryVersion.get(), primaryVersion.getPrimaryVersion());
      this.primaryVersion.set(primaryVersion.getPrimaryVersion());
    }

    boolean isPrimary = StringUtils.equals(MATCH_ALL_VERSION, primaryVersion.getPrimaryVersion())
        || StringUtils.equals(versionInfoManager.getVersionInfo().getVersion(), primaryVersion.getPrimaryVersion());
    primary.set(isPrimary);
  }
}