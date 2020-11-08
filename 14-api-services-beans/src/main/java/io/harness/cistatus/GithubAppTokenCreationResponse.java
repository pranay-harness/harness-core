package io.harness.cistatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubAppTokenCreationResponse {
  private String token;
  private String expires_at;
}
