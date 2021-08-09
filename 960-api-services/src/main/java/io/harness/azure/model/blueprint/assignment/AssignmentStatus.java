package io.harness.azure.model.blueprint.assignment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssignmentStatus {
  private String lastModified;
  private String[] managedResources;
  private String timeCreated;
}
