package io.harness.azure.model.blueprint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlueprintStatus {
  private String lastModified;
  private String timeCreated;
}
