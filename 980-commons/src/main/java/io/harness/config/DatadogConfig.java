package io.harness.config;

import io.harness.secret.ConfigSecret;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;
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
  @ConfigSecret private String apiKey;
}
