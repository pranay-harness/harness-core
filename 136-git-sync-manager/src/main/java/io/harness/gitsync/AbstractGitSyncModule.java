/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync;

import static io.harness.ng.DbAliases.NG_MANAGER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Consumer;
import io.harness.eventsframework.impl.noop.NoOpConsumer;
import io.harness.eventsframework.impl.redis.RedisConsumer;
import io.harness.redis.RedisConfig;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

@OwnedBy(HarnessTeam.DX)
public abstract class AbstractGitSyncModule extends AbstractModule {
  @Override
  protected void configure() {
    if (getRedisConfig().getRedisUrl().equals("dummyRedisUrl")) {
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.GIT_PUSH_EVENT_STREAM))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.GIT_BRANCH_HOOK_EVENT_STREAM))
          .toInstance(
              NoOpConsumer.of(EventsFrameworkConstants.DUMMY_TOPIC_NAME, EventsFrameworkConstants.DUMMY_GROUP_NAME));
    } else {
      // TODO check for group name
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.GIT_PUSH_EVENT_STREAM))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.GIT_PUSH_EVENT_STREAM, NG_MANAGER, getRedisConfig(),
              EventsFrameworkConstants.GIT_PUSH_EVENT_STREAM_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.GIT_PUSH_EVENT_STREAM_BATCH_SIZE));
      bind(Consumer.class)
          .annotatedWith(Names.named(EventsFrameworkConstants.GIT_BRANCH_HOOK_EVENT_STREAM))
          .toInstance(RedisConsumer.of(EventsFrameworkConstants.GIT_BRANCH_HOOK_EVENT_STREAM, NG_MANAGER,
              getRedisConfig(), EventsFrameworkConstants.GIT_BRANCH_HOOK_EVENT_STREAM_MAX_PROCESSING_TIME,
              EventsFrameworkConstants.GIT_BRANCH_HOOK_EVENT_STREAM_BATCH_SIZE));
    }
  }

  public abstract RedisConfig getRedisConfig();
}
