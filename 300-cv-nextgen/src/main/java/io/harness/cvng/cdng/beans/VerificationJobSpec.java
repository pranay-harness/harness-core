package io.harness.cvng.cdng.beans;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Data
@FieldNameConstants(innerTypeName = "VerificationJobSpecKeys")
@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@OwnedBy(HarnessTeam.CV)
@SuperBuilder
@NoArgsConstructor
public abstract class VerificationJobSpec {
  public abstract String getType();
  ParameterField<String> envRef;
  ParameterField<String> serviceRef;
  ParameterField<String> deploymentTag;
  ParameterField<String> duration;
  public Map<String, String> getRuntimeValues() {
    HashMap<String, String> runtimeParams = new HashMap<>();
    if (duration.getValue() != null) {
      runtimeParams.put(VerificationJobSpecKeys.duration, duration.getValue());
    }
    addToRuntimeParams(runtimeParams);
    return runtimeParams;
  }

  protected abstract void addToRuntimeParams(HashMap<String, String> runtimeParams);
}
