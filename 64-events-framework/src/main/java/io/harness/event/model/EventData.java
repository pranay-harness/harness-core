package io.harness.event.model;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author rktummala
 */
@Data
@Builder
public class EventData {
  @Default private Map<String, String> properties = new HashMap<>();
  private double value;

  /**
   * Any model that you want to put in the queue should implement EventInfo
   * and on the handler side, you can cast it to your model
   */
  private EventInfo eventInfo;
}
