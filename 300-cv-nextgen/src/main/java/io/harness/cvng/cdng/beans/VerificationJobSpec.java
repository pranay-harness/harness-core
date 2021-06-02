package io.harness.cvng.cdng.beans;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
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
  @ApiModelProperty(hidden = true) public abstract String getType();
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH,
      value = "You can put expression <+env.identifier> to resolve env of current stage.")
  ParameterField<String> envRef;
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH,
      value = "You can put expression <+service.identifier> to resolve env of current stage.")
  ParameterField<String> serviceRef;
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH,
      value = "You can put expression <+serviceConfig.artifacts.primary.tag> to resolve primary tag")
  ParameterField<String> deploymentTag;
  @NotNull
  @ApiModelProperty(
      dataType = SwaggerConstants.STRING_CLASSPATH, value = "Format example: 5m, 30m, please put multiple of 5")
  ParameterField<String> duration;

  @ApiModelProperty(hidden = true)
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
