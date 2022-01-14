package io.harness.azure.model.blueprint.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentLockMode {
  private String allResourcesDoNotDelete;
  private String allResourcesReadOnly;
  private String none;
}
