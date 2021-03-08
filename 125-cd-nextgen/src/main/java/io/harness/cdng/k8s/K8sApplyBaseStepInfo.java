package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("K8sApplyBaseStepInfo")
public class K8sApplyBaseStepInfo {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> skipDryRun;
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> skipSteadyStateCheck;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) ParameterField<List<String>> filePaths;
}
