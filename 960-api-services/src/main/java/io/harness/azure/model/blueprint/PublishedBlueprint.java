package io.harness.azure.model.blueprint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublishedBlueprint {
  private String id;
  private String name;
  private String description;
  private String displayName;
  private Properties properties;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Properties {
    private String blueprintName;
    private String changeNotes;
    private Map<String, ParameterDefinition> parameters;
    private Map<String, ResourceGroupDefinition> resourceGroups;
    private BlueprintStatus status;
    private String targetScope;
    private String type;
  }
}
