package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("K8sScaleBaseStepInfo")
public class K8sScaleBaseStepInfo {
  @NotNull InstanceSelectionWrapper instanceSelection;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> workload;
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> skipSteadyStateCheck;
}
