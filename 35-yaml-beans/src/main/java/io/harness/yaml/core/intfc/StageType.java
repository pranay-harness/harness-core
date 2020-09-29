package io.harness.yaml.core.intfc;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.pipeline.executions.NGStageType;

/** Base Interface for different stage types. **/
@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface StageType extends WithIdentifier {
  void setIdentifier(String identifier);
  void setName(String name);
  @JsonIgnore NGStageType getStageType();
}
