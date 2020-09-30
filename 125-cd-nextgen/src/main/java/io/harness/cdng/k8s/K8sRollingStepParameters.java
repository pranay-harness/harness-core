package io.harness.cdng.k8s;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.beans.ParameterField;
import io.harness.common.SwaggerConstants;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.state.io.StepParameters;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sRollingStepParameters implements StepParameters {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> skipDryRun;
  @JsonIgnore Map<String, StepDependencySpec> stepDependencySpecs;
}
