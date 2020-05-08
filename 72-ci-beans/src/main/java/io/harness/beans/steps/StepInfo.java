package io.harness.beans.steps;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.beans.template.BaseStepTemplate;
import io.harness.state.StateType;
import io.harness.state.io.StateParameters;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = CustomStepInfo.class, name = "CUSTOM")
  , @JsonSubTypes.Type(value = BuildEnvSetupStepInfo.class, name = "SETUP_ENV"),
      @JsonSubTypes.Type(value = BuildStepInfo.class, name = "BUILD"),
      @JsonSubTypes.Type(value = ArtifactUploadStepInfo.class, name = "UPLOAD_ARTIFACT"),
      @JsonSubTypes.Type(value = TestStepInfo.class, name = "TEST")
})
public interface StepInfo extends StateParameters, BaseStepTemplate {
  StepType getType();

  StateType getStateType();

  String getStepName();
}
