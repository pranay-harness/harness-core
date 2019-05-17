package io.harness.event.handler.segment;

import com.google.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author rktummala on 05/08/19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class SegmentConfig {
  @JsonProperty(defaultValue = "false") private boolean enabled;
  private String url;
  private String apiKey;
}
