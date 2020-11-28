package io.harness.event.model;

import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

/**
 * @author rktummala
 */
@Data
@Builder
public class EventData {
  @Default private Map<String, String> properties;
  // TODO : Remove this value once prometheus is deprecated
  private double value;

  /**
   * Any model that you want to put in the queue should implement EventInfo
   * and on the handler side, you can cast it to your model
   */
  private EventInfo eventInfo;
}
