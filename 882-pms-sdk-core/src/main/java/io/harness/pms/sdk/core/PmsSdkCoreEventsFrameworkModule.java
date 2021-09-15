/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.sdk.core;

import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_PARTIAL_PLAN_RESPONSE;
import static io.harness.eventsframework.EventsFrameworkConstants.PIPELINE_SDK_RESPONSE_EVENT_TOPIC;
import static io.harness.pms.sdk.core.PmsSdkCoreEventsFrameworkConstants.PARTIAL_PLAN_RESPONSE_EVENT_PRODUCER;
import static io.harness.pms.sdk.core.PmsSdkCoreEventsFrameworkConstants.SDK_RESPONSE_EVENT_PRODUCER;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class PmsSdkCoreEventsFrameworkModule extends AbstractModule {
  private static PmsSdkCoreEventsFrameworkModule instance;

  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;
  private final String serviceName;

  public static PmsSdkCoreEventsFrameworkModule getInstance(EventsFrameworkConfiguration config, String serviceName) {
    if (instance == null) {
      instance = new PmsSdkCoreEventsFrameworkModule(config, serviceName);
    }
    return instance;
  }

  private PmsSdkCoreEventsFrameworkModule(EventsFrameworkConfiguration config, String serviceName) {
    this.eventsFrameworkConfiguration = config;
    this.serviceName = serviceName;
  }

  @Override
  protected void configure() {
    RedisConfig redisConfig = this.eventsFrameworkConfiguration.getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      bind(Producer.class)
          .annotatedWith(Names.named(SDK_RESPONSE_EVENT_PRODUCER))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(PARTIAL_PLAN_RESPONSE_EVENT_PRODUCER))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
    } else {
      bind(Producer.class)
          .annotatedWith(Names.named(SDK_RESPONSE_EVENT_PRODUCER))
          .toInstance(RedisProducer.of(PIPELINE_SDK_RESPONSE_EVENT_TOPIC, redisConfig,
              EventsFrameworkConstants.PIPELINE_SDK_RESPONSE_EVENT_MAX_TOPIC_SIZE, serviceName));
      bind(Producer.class)
          .annotatedWith(Names.named(PARTIAL_PLAN_RESPONSE_EVENT_PRODUCER))
          .toInstance(RedisProducer.of(PIPELINE_PARTIAL_PLAN_RESPONSE, redisConfig,
              EventsFrameworkConstants.PIPELINE_SDK_RESPONSE_EVENT_MAX_TOPIC_SIZE, serviceName));
    }
  }
}
