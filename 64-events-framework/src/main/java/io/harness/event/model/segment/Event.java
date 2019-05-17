package io.harness.event.model.segment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * @author rktummala
 */
@Value
@Builder
@AllArgsConstructor
public class Event {
  private String email;
  //  private Map<String, String> properties;
  private Properties properties;
}
