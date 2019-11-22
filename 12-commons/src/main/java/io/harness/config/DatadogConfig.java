package io.harness.config;

import com.google.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author rktummala
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Singleton
public class DatadogConfig {
  @JsonProperty(defaultValue = "false") private boolean enabled;
  private String apiKey;
}
