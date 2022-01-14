package io.harness.pcf.model;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@OwnedBy(CDP)
public class CfConfig {
  @NotEmpty private String endpointUrl;
  private char[] username;
  private char[] password;
}
