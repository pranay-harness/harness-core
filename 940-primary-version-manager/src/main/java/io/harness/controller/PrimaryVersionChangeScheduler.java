package io.harness.controller;

import static io.harness.beans.PrimaryVersion.Builder.aConfiguration;
import static io.harness.configManager.PrimaryVersion.MATCH_ALL_VERSION;

import io.harness.beans.PrimaryVersion;
import io.harness.persistence.HPersistence;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Singleton
public class PrimaryVersionChangeScheduler {
  @Inject private PrimaryVersionController configurationController;
  @Inject private HPersistence hPersistence;

  public void registerExecutors() {
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
