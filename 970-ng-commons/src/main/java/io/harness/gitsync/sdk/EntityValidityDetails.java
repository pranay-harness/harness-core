package io.harness.gitsync.sdk;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EntityValidityDetails {
  private boolean valid;
  private String invalidYaml;
}
