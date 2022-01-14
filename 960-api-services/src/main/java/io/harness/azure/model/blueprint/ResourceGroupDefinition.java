package io.harness.azure.model.blueprint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceGroupDefinition {
  private String[] dependsOn;
  private String location;
  private String name;
  private Object tags;
  private Metadata metadata;
}
