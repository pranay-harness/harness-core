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
public class Properties {
  private String original_timestamp;
  private String tech_name;
  private String tech_category;
}
