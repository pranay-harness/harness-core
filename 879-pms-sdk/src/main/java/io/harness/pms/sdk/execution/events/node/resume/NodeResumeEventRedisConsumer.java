package io.harness.pms.sdk.execution.events.node.resume;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.sdk.execution.events.PmsSdkEventFrameworkConstants.PT_NODE_RESUME_CONSUMER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import javax.cache.Cache;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
@Singleton
public class NodeResumeEventRedisConsumer extends PmsAbstractRedisConsumer<NodeResumeEventMessageListener> {
  @Inject
  public NodeResumeEventRedisConsumer(@Named(PT_NODE_RESUME_CONSUMER) Consumer redisConsumer,
      NodeResumeEventMessageListener messageListener, @Named("sdkEventsCache") Cache<String, Integer> eventsCache) {
    super(redisConsumer, messageListener, eventsCache);
  }
}
