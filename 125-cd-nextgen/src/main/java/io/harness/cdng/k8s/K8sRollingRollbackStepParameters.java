package io.harness.cdng.k8s;

import io.harness.beans.ParameterField;
import io.harness.common.SwaggerConstants;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("k8sRollingRollbackStepParameters")
public class K8sRollingRollbackStepParameters implements StepParameters {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) private ParameterField<String> timeout;
  @JsonIgnore Map<String, StepDependencySpec> stepDependencySpecs;
}
