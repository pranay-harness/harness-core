package software.wings.core.outbox;

import io.harness.outbox.OutboxEvent;
import io.harness.outbox.api.OutboxEventHandler;

import com.google.inject.Inject;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WingsOutboxEventHandler implements OutboxEventHandler {
  private final Map<String, OutboxEventHandler> outboxEventHandlerMap;

  @Inject
  public WingsOutboxEventHandler(Map<String, OutboxEventHandler> outboxEventHandlerMap) {
    this.outboxEventHandlerMap = outboxEventHandlerMap;
  }

  @Override
  public boolean handle(OutboxEvent outboxEvent) {
    try {
      OutboxEventHandler handler = outboxEventHandlerMap.get(outboxEvent.getResource().getType());
      if (null != handler) {
        return handler.handle(outboxEvent);
      }
      return false;
    } catch (Exception exception) {
      log.error(
          String.format("Unexpected error occurred during handling event of type %s", outboxEvent.getEventType()));
      return false;
    }
  }
}
