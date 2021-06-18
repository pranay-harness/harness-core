package io.harness.pms.sdk.execution.events.progress;

import static io.harness.pms.sdk.execution.events.PmsUtilityConsumerConstants.PT_PROGRESS_CONSUMER;
import static io.harness.pms.sdk.execution.events.PmsUtilityConsumerConstants.PT_PROGRESS_LISTENER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class ProgressEventRedisConsumer extends PmsAbstractRedisConsumer {
  @Inject
  public ProgressEventRedisConsumer(@Named(PT_PROGRESS_CONSUMER) Consumer redisConsumer,
      @Named(PT_PROGRESS_LISTENER) MessageListener messageListener) {
    super(redisConsumer, messageListener);
  }
}
