package io.harness.hazelcast;

import com.google.inject.Injector;
import com.google.inject.Provides;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import io.harness.govern.DependencyModule;
import io.harness.govern.DependencyProviderModule;
import io.harness.govern.ServersModule;

import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class HazelcastModule extends DependencyProviderModule implements ServersModule {
  public static final String INSTANCE_NAME = "wings-hazelcast";
  private static volatile HazelcastModule instance;
  private HazelcastInstance hazelcastInstance;

  public static HazelcastModule getInstance() {
    if (instance == null) {
      instance = new HazelcastModule();
    }
    return instance;
  }

  @Provides
  public HazelcastInstance getHazelcastInstance() {
    if (hazelcastInstance == null || !hazelcastInstance.getLifecycleService().isRunning()) {
      Config config = new XmlConfigBuilder().build();
      config.setInstanceName(INSTANCE_NAME);
      hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(config);
    }
    return hazelcastInstance;
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return Collections.emptySet();
  }

  @Override
  public List<Closeable> servers(Injector injector) {
    return Collections.singletonList(() -> {
      if (hazelcastInstance != null) {
        hazelcastInstance.shutdown();
      }
    });
  }
}
