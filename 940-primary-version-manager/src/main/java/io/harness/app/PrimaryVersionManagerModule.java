package io.harness.app;

import io.harness.configManager.ConfigurationController;
import io.harness.controller.PrimaryVersionChangeScheduler;
import io.harness.controller.PrimaryVersionController;
import io.harness.govern.ProviderModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.queue.QueueController;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.PrimaryVersionManagerRegistrars;
import io.harness.version.VersionInfoManager;

import com.google.common.collect.ImmutableSet;
import com.google.inject.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.IOUtils;

public class PrimaryVersionManagerModule extends AbstractModule {
  private static volatile PrimaryVersionManagerModule instance;

  private PrimaryVersionManagerModule() {}

  public static PrimaryVersionManagerModule getInstance() {
    if (instance == null) {
      instance = new PrimaryVersionManagerModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    try {
      VersionInfoManager versionInfoManager = new VersionInfoManager(
          IOUtils.toString(getClass().getClassLoader().getResourceAsStream("main/resources-filtered/versionInfo.yaml"),
              StandardCharsets.UTF_8));
      bind(VersionInfoManager.class).toInstance(versionInfoManager);
      bind(QueueController.class).to(PrimaryVersionController.class);
    } catch (IOException e) {
      throw new IllegalStateException("Could not load versionInfo.yaml", e);
    }
  }
}
