package io.harness.pms.sdk.execution.events.progress;

import static io.harness.pms.sdk.execution.events.PmsSdkEventFrameworkConstants.PT_PROGRESS_CONSUMER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;
import io.harness.queue.QueueController;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class ProgressEventRedisConsumer extends PmsAbstractRedisConsumer<ProgressEventMessageListener> {
  @Inject
  public ProgressEventRedisConsumer(@Named(PT_PROGRESS_CONSUMER) Consumer redisConsumer,
      ProgressEventMessageListener messageListener, @Named("sdkEventsCache") Cache<String, Integer> eventsCache,
      QueueController queueController) {
    super(redisConsumer, messageListener, eventsCache, queueController);
  }
}
