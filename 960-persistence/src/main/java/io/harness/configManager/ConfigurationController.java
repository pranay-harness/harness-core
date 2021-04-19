package io.harness.configManager;

import static io.harness.configManager.Configuration.Builder.aConfiguration;
import static io.harness.configManager.Configuration.MATCH_ALL_VERSION;

import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.version.VersionInfoManager;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.StringUtils;

@Singleton
@Slf4j
public class ConfigurationController implements QueueController {
  @Inject private HPersistence persistence;
  @Inject private VersionInfoManager versionInfoManager;

  private final AtomicBoolean primary = new AtomicBoolean(true);
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
    Configuration configuration = persistence.createQuery(Configuration.class).get();
    if (configuration == null) {
      configuration = aConfiguration().withPrimaryVersion(MATCH_ALL_VERSION).build();
      persistence.save(configuration);
    }

    if (!StringUtils.equals(primaryVersion.get(), configuration.getPrimaryVersion())) {
      primaryVersion.set(configuration.getPrimaryVersion());
    }

    boolean isPrimary = StringUtils.equals(MATCH_ALL_VERSION, configuration.getPrimaryVersion())
        || StringUtils.equals(versionInfoManager.getVersionInfo().getVersion(), configuration.getPrimaryVersion());
    primary.set(isPrimary);
  }
}
