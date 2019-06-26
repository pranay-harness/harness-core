package io.harness.marketplace.gcp.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.marketplace.gcp.events.intfc.Event;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Captures GCP marketplace pub/sub events' common properties
 */
@Getter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class BaseEvent implements Event {
  private EventType eventType;
  private String eventId;

  // jackson needs it
  public BaseEvent() {}

  public BaseEvent(EventType eventType, String eventId) {
    this.eventType = eventType;
    this.eventId = eventId;
  }
}
