package io.harness.yaml.core;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.yaml.core.auxiliary.intfc.ExecutionWrapper;
import io.harness.yaml.core.intfc.WithIdentifier;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("step")
@JsonIgnoreProperties(ignoreUnknown = true)
public class StepElement implements ExecutionWrapper, WithIdentifier {
  String identifier;
  String name;
  String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  StepSpecType stepSpecType;

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setStepSpecType(StepSpecType stepSpecType) {
    this.stepSpecType = stepSpecType;
    if (this.stepSpecType != null) {
      this.stepSpecType.setIdentifier(identifier);
      this.stepSpecType.setName(name);
    }
  }

  @Builder
  public StepElement(String identifier, String name, String type, StepSpecType stepSpecType) {
    this.identifier = identifier;
    this.name = name;
    this.type = type;
    this.stepSpecType = stepSpecType;
  }
}
