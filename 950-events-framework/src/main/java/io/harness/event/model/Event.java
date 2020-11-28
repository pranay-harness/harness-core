package io.harness.event.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala
 */
@Data
@Builder
public class Event {
  private EventData eventData;
  private EventType eventType;
}
