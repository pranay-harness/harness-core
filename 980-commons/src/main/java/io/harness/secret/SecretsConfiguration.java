package io.harness.secret;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class SecretsConfiguration {
  @JsonProperty(value = "secretResolutionEnabled", defaultValue = "false") private boolean secretResolutionEnabled;

  @JsonProperty(value = "gcpSecretManagerProject") private String gcpSecretManagerProject;
}
