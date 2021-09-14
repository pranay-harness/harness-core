/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.eventsframework;

import static io.harness.AuthorizationServiceHeader.NG_MANAGER;
import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_BRANCH_HOOK_EVENT_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_BRANCH_HOOK_EVENT_STREAM_MAX_TOPIC_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_CONFIG_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_PR_EVENT_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_PR_EVENT_STREAM_MAX_TOPIC_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_PUSH_EVENT_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.GIT_PUSH_EVENT_STREAM_MAX_TOPIC_SIZE;
import static io.harness.eventsframework.EventsFrameworkConstants.INSTANCE_STATS;
import static io.harness.eventsframework.EventsFrameworkConstants.WEBHOOK_EVENTS_STREAM;
import static io.harness.eventsframework.EventsFrameworkConstants.WEBHOOK_EVENTS_STREAM_MAX_TOPIC_SIZE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.noop.NoOpProducer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.eventsframework.impl.redis.RedisProducer;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import lombok.AllArgsConstructor;

@OwnedBy(PL)
@AllArgsConstructor
public class EventsFrameworkModule extends AbstractModule {
  private final EventsFrameworkConfiguration eventsFrameworkConfiguration;

  @Override
  protected void configure() {
    RedisConfig redisConfig = this.eventsFrameworkConfiguration.getRedisConfig();
    if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.NG_ACCOUNT_SETUP))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.SETUP_USAGE))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.SETUP_USAGE))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_ACTIVITY))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_ACTIVITY))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.GIT_CONFIG_STREAM))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.USERMEMBERSHIP))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.USERMEMBERSHIP))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(WEBHOOK_EVENTS_STREAM))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.POLLING_EVENTS_STREAM))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(GIT_PUSH_EVENT_STREAM))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(GIT_PR_EVENT_STREAM))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(GIT_BRANCH_HOOK_EVENT_STREAM))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(GIT_CONFIG_STREAM))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.INSTANCE_STATS))
          .toInstance(NoOpProducer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(INSTANCE_STATS))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
    } else {
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.ENTITY_CRUD, redisConfig,
              EventsFrameworkConstants.ENTITY_CRUD_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_CRUD))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.ENTITY_CRUD, NG_MANAGER.getServiceId(), redisConfig,
              EventsFrameworkConstants.ENTITY_CRUD_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.ENTITY_CRUD_READ_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.NG_ACCOUNT_SETUP))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.ENTITY_CRUD, "NG_ACCOUNT_SETUP_GROUP", redisConfig,
              EventsFrameworkConstants.NG_ACCOUNT_SETUP_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.NG_ACCOUNT_SETUP_READ_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.USERMEMBERSHIP))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.USERMEMBERSHIP, NG_MANAGER.getServiceId(), redisConfig,
              EventsFrameworkConstants.USERMEMBERSHIP_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.USERMEMBERSHIP_READ_BATCH_SIZE));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.SETUP_USAGE))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.SETUP_USAGE, redisConfig,
              EventsFrameworkConstants.ENTITY_CRUD_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.SETUP_USAGE))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.SETUP_USAGE, NG_MANAGER.getServiceId(), redisConfig,
              EventsFrameworkConstants.SETUP_USAGE_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.SETUP_USAGE_READ_BATCH_SIZE));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_ACTIVITY))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.ENTITY_ACTIVITY, redisConfig,
              EventsFrameworkConstants.ENTITY_ACTIVITY_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.ENTITY_ACTIVITY))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.ENTITY_ACTIVITY, NG_MANAGER.getServiceId(), redisConfig,
              EventsFrameworkConstants.ENTITY_ACTIVITY_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.ENTITY_ACTIVITY_READ_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.HARNESS_TO_GIT_PUSH, NG_MANAGER.getServiceId(),
              redisConfig, EventsFrameworkConstants.HARNESS_TO_GIT_PUSH_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.HARNESS_TO_GIT_PUSH_READ_BATCH_SIZE));
      // todo(abhinav): move this to git sync manager if it is carved out.
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.GIT_CONFIG_STREAM))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.GIT_CONFIG_STREAM, redisConfig,
              EventsFrameworkConstants.GIT_CONFIG_STREAM_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.USERMEMBERSHIP))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.USERMEMBERSHIP, redisConfig,
              EventsFrameworkConstants.USER_MEMBERSHIP_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Producer.class)
          .annotatedWith(Names.named(WEBHOOK_EVENTS_STREAM))
          .toInstance(RedisProducer.of(
              WEBHOOK_EVENTS_STREAM, redisConfig, WEBHOOK_EVENTS_STREAM_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.POLLING_EVENTS_STREAM))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.POLLING_EVENTS_STREAM, redisConfig,
              EventsFrameworkConstants.POLLING_EVENTS_STREAM_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Producer.class)
          .annotatedWith(Names.named(GIT_PUSH_EVENT_STREAM))
          .toInstance(RedisProducer.of(
              GIT_PUSH_EVENT_STREAM, redisConfig, GIT_PUSH_EVENT_STREAM_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Producer.class)
          .annotatedWith(Names.named(GIT_PR_EVENT_STREAM))
          .toInstance(RedisProducer.of(
              GIT_PR_EVENT_STREAM, redisConfig, GIT_PR_EVENT_STREAM_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.SAML_AUTHORIZATION_ASSERTION))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.SAML_AUTHORIZATION_ASSERTION, NG_MANAGER.getServiceId(),
              redisConfig, EventsFrameworkConstants.DEFAULT_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.DEFAULT_READ_BATCH_SIZE));
      bind(Producer.class)
          .annotatedWith(Names.named(GIT_BRANCH_HOOK_EVENT_STREAM))
          .toInstance(RedisProducer.of(GIT_BRANCH_HOOK_EVENT_STREAM, redisConfig,
              GIT_BRANCH_HOOK_EVENT_STREAM_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Consumer.class)
          .annotatedWith(Names.named(GIT_CONFIG_STREAM))
          .toInstance(RedisConsumer.of(GIT_CONFIG_STREAM, NG_MANAGER.getServiceId(), redisConfig,
              EventsFrameworkConstants.GIT_CONFIG_STREAM_PROCESSING_TIME,
              EventsFrameworkConstants.GIT_CONFIG_STREAM_READ_BATCH_SIZE));
      bind(Producer.class)
          .annotatedWith(Names.named(INSTANCE_STATS))
          .toInstance(RedisProducer.of(
              INSTANCE_STATS, redisConfig, EventsFrameworkConstants.DEFAULT_TOPIC_SIZE, NG_MANAGER.getServiceId()));
      bind(Consumer.class)
          .annotatedWith(Names.named(INSTANCE_STATS))
          .toInstance(RedisConsumer.of(INSTANCE_STATS, NG_MANAGER.getServiceId(), redisConfig,
              EventsFrameworkConstants.DEFAULT_MAX_PROCESSING_TIME, EventsFrameworkConstants.DEFAULT_READ_BATCH_SIZE));
      bind(Producer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.CD_DEPLOYMENT_EVENT))
          .toInstance(RedisProducer.of(EventsFrameworkConstants.CD_DEPLOYMENT_EVENT, redisConfig,
              EventsFrameworkConstants.CD_DEPLOYMENT_EVENT_MAX_TOPIC_SIZE, NG_MANAGER.getServiceId()));
    }
  }
}
