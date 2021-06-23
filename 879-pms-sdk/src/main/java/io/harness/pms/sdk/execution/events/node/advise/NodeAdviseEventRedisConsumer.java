package io.harness.pms.sdk.execution.events.node.advise;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.pms.sdk.execution.events.PmsSdkEventFrameworkConstants.PT_NODE_ADVISE_CONSUMER;
import static io.harness.pms.sdk.execution.events.PmsSdkEventFrameworkConstants.PT_NODE_ADVISE_LISTENER;

import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.api.Consumer;
import io.harness.ng.core.event.MessageListener;
import io.harness.pms.events.base.PmsAbstractRedisConsumer;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
@Singleton
public class NodeAdviseEventRedisConsumer extends PmsAbstractRedisConsumer {
  @Inject
  public NodeAdviseEventRedisConsumer(@Named(PT_NODE_ADVISE_CONSUMER) Consumer redisConsumer,
      @Named(PT_NODE_ADVISE_LISTENER) MessageListener messageListener) {
    super(redisConsumer, messageListener);
  }
}
