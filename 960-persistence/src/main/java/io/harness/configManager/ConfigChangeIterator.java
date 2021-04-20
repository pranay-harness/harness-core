package io.harness.configManager;

import static io.harness.configManager.PrimaryVersion.Builder.aConfiguration;
import static io.harness.configManager.PrimaryVersion.MATCH_ALL_VERSION;

import io.harness.persistence.HPersistence;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConfigChangeIterator {
  @Inject private ConfigurationController configurationController;
  @Inject private HPersistence hPersistence;

  public void registerIterators() {
    PrimaryVersion primaryVersion = hPersistence.createQuery(PrimaryVersion.class).get();
    if (primaryVersion == null) {
      primaryVersion = aConfiguration().withPrimaryVersion(MATCH_ALL_VERSION).build();
      hPersistence.save(primaryVersion);
    }
    ScheduledThreadPoolExecutor configChangeExecutor =
        new ScheduledThreadPoolExecutor(2, new ThreadFactoryBuilder().setNameFormat("config-change-iterator").build());
    configChangeExecutor.scheduleWithFixedDelay(() -> configurationController.run(), 0, 5, TimeUnit.SECONDS);
  }
}
