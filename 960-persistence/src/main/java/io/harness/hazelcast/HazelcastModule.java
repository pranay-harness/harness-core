/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.hazelcast;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;

import com.google.inject.Injector;
import com.google.inject.Provides;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import java.io.Closeable;
import java.util.Collections;
import java.util.List;

@OwnedBy(PL)
public class HazelcastModule extends ProviderModule implements ServersModule {
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
  public List<Closeable> servers(Injector injector) {
    return Collections.singletonList(() -> {
      if (hazelcastInstance != null) {
        hazelcastInstance.shutdown();
      }
    });
  }
}
