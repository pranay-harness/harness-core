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
public class Trace {
  private String userId;
  private String event;
  private Properties properties;
}
