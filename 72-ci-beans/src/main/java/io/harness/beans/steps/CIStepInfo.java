package io.harness.beans.steps;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.beans.template.CIBaseStepTemplate;
import io.harness.state.StateType;
import io.harness.state.io.StateParameters;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = io.harness.beans.steps.CICustomStepInfo.class, name = "CUSTOM")
  , @JsonSubTypes.Type(value = io.harness.beans.steps.CIBuildEnvSetupStepInfo.class, name = "SETUP_ENV"),
      @JsonSubTypes.Type(value = io.harness.beans.steps.CIBuildStepInfo.class, name = "BUILD"),
      @JsonSubTypes.Type(value = io.harness.beans.steps.CIArtifactUploadStepInfo.class, name = "UPLOAD_ARTIFACT"),
      @JsonSubTypes.Type(value = io.harness.beans.steps.CITestStepInfo.class, name = "TEST")
})
public interface CIStepInfo extends StateParameters, CIBaseStepTemplate {
  StepType getType();

  StateType getStateType();

  String getStepName();
}
