package io.harness.azure.model.blueprint;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ParameterDefinition {
  private List<String> allowedValues;
  private String defaultValue;
  private Metadata metadata;
  private String type;
}
