package io.harness.mongo;

import io.harness.data.structure.NullSafeImmutableMap;
import io.harness.logging.AutoLogContext;
import io.harness.queue.Queuable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MessageLogContext extends AutoLogContext {
  public MessageLogContext(Queuable message, OverrideBehavior behavior) {
    super(NullSafeImmutableMap.<String, String>builder()
              .put("class", message.getClass().getName())
              .putIfNotNull("messageId", message.getId())
              .build(),
        behavior);
  }
}
