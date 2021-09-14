/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness;

import static io.harness.AuthorizationServiceHeader.PIPELINE_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.eventsframework.EventsFrameworkConstants.ORCHESTRATION_LOG;
import static io.harness.eventsframework.EventsFrameworkConstants.PLAN_NOTIFY_EVENT_TOPIC;
import static io.harness.eventsframework.EventsFrameworkConstants.PMS_ORCHESTRATION_NOTIFY_EVENT;
import static io.harness.eventsframework.EventsFrameworkConstants.WEBHOOK_REQUEST_PAYLOAD_DETAILS;
import static io.harness.eventsframework.EventsFrameworkConstants.WEBHOOK_REQUEST_PAYLOAD_DETAILS_MAX_TOPIC_SIZE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.GitAwareRedisProducer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@OwnedBy(PIPELINE)
public class PipelineServiceEventsFrameworkModule extends AbstractModule {
  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;

  @Override
  protected void configure() {
    RedisConfig redisConfig = this.eventsFrameworkConfiguration.getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.SETUP_USAGE))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.PLAN_NOTIFY_EVENT_PRODUCER))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.PLAN_NOTIFY_EVENT_TOPIC));
      bind(Producer.class)
          .annotatedWith(Names.named(WEBHOOK_REQUEST_PAYLOAD_DETAILS))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(ORCHESTRATION_LOG))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.PLAN_NOTIFY_EVENT_PRODUCER))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));

      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.PMS_ORCHESTRATION_NOTIFY_EVENT))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
    } else {
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.SETUP_USAGE))
          .toInstance(GitAwareRedisProducer.of(EventsFrameworkConstants.SETUP_USAGE, redisConfig,
              EventsFrameworkConstants.SETUP_USAGE_MAX_TOPIC_SIZE, PIPELINE_SERVICE.getServiceId()));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.PLAN_NOTIFY_EVENT_PRODUCER))
          .toInstance(GitAwareRedisProducer.of(EventsFrameworkConstants.PLAN_NOTIFY_EVENT_TOPIC, redisConfig,
              EventsFrameworkConstants.PLAN_NOTIFY_EVENT_MAX_TOPIC_SIZE, PIPELINE_SERVICE.getServiceId()));
      bind(Producer.class)
          .annotatedWith(Names.named(WEBHOOK_REQUEST_PAYLOAD_DETAILS))
          .toInstance(RedisProducer.of(WEBHOOK_REQUEST_PAYLOAD_DETAILS, redisConfig,
              WEBHOOK_REQUEST_PAYLOAD_DETAILS_MAX_TOPIC_SIZE, PIPELINE_SERVICE.getServiceId()));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.ENTITY_CRUD, redisConfig,
              EventsFrameworkConstants.ENTITY_CRUD_MAX_TOPIC_SIZE, PIPELINE_SERVICE.getServiceId()));
      bind(Producer.class)
          .annotatedWith(Names.named(ORCHESTRATION_LOG))
          .toInstance(RedisProducer.of(ORCHESTRATION_LOG, redisConfig,
              EventsFrameworkConstants.ENTITY_CRUD_MAX_TOPIC_SIZE, PIPELINE_SERVICE.getServiceId()));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.PMS_ORCHESTRATION_NOTIFY_EVENT))
          .toInstance(GitAwareRedisProducer.of(EventsFrameworkConstants.PMS_ORCHESTRATION_NOTIFY_EVENT, redisConfig,
              EventsFrameworkConstants.PMS_ORCHESTRATION_NOTIFY_EVENT_MAX_TOPIC_SIZE, PIPELINE_SERVICE.getServiceId()));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.ENTITY_CRUD, PIPELINE_SERVICE.getServiceId(),
              redisConfig, EventsFrameworkConstants.ENTITY_CRUD_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.ENTITY_CRUD_READ_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.POLLING_EVENTS_STREAM))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.POLLING_EVENTS_STREAM, PIPELINE_SERVICE.getServiceId(),
              redisConfig, EventsFrameworkConstants.POLLING_EVENTS_STREAM_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.POLLING_EVENTS_STREAM_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.WEBHOOK_EVENTS_STREAM))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.WEBHOOK_EVENTS_STREAM, PIPELINE_SERVICE.getServiceId(),
              redisConfig, EventsFrameworkConstants.WEBHOOK_EVENTS_STREAM_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.WEBHOOK_EVENTS_STREAM_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(PLAN_NOTIFY_EVENT_TOPIC))
          .toInstance(RedisConsumer.of(PLAN_NOTIFY_EVENT_TOPIC, PIPELINE_SERVICE.getServiceId(), redisConfig,
              EventsFrameworkConstants.PLAN_NOTIFY_EVENT_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.PLAN_NOTIFY_EVENT_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(PMS_ORCHESTRATION_NOTIFY_EVENT))
          .toInstance(RedisConsumer.of(PMS_ORCHESTRATION_NOTIFY_EVENT, PIPELINE_SERVICE.getServiceId(), redisConfig,
              EventsFrameworkConstants.PLAN_NOTIFY_EVENT_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.PMS_ORCHESTRATION_NOTIFY_EVENT_BATCH_SIZE));
    }
  }
}
