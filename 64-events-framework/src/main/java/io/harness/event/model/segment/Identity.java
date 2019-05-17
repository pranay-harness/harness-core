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
public class Identity {
  private String userId;
  private String anonymousId;
  private Traits traits;
}
