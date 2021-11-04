package io.harness.event.handler.marketo;

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
public class MarketoConfig {
  @JsonProperty(defaultValue = "false") private boolean enabled;
  private String url;
  private String clientId;
  @ConfigSecret private String clientSecret;
}
