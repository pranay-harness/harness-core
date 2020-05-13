package io.harness.beans.stages;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes({ @JsonSubTypes.Type(value = IntegrationStage.class, name = "INTEGRATION") })
public interface StageInfo {
  enum StageType { INTEGRATION }
  StageType getType();
  String getIdentifier();
}
