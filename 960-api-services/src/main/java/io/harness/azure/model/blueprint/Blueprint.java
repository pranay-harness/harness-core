package io.harness.azure.model.blueprint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Blueprint {
  private String id;
  private String name;
  private String description;
  private String displayName;
  private String type;
  private Properties properties;

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Properties {
    private Map<String, ParameterDefinition> parameters;
    private Map<String, ResourceGroupDefinition> resourceGroups;
    private BlueprintStatus status;
    private String targetScope;
    private Object versions;
    private Object layout;
  }
}
