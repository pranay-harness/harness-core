package io.harness.secret;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class SecretsConfiguration {
  @JsonProperty(value = "secretResolutionEnabled", defaultValue = "false") private boolean secretResolutionEnabled;

  @JsonProperty(value = "gcpSecretManagerProject") private String gcpSecretManagerProject;
}
