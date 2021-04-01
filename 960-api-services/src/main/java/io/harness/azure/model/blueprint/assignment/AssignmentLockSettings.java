package io.harness.azure.model.blueprint.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentLockSettings {
  private String[] excludedActions;
  private String[] excludedPrincipals;
  private String mode;
}
