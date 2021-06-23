package io.harness.pms.sdk.execution.events.node.start;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.sdk.execution.events.PmsSdkEventFrameworkConstants.PT_NODE_START_CONSUMER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
@Singleton
public class NodeStartEventRedisConsumer extends PmsAbstractRedisConsumer<NodeStartEventMessageListener> {
  @Inject
  public NodeStartEventRedisConsumer(
      @Named(PT_NODE_START_CONSUMER) Consumer redisConsumer, NodeStartEventMessageListener messageListener) {
    super(redisConsumer, messageListener);
  }
}
