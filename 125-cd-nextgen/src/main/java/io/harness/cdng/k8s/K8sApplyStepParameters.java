package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("k8sApplyStepParameters")
public class K8sApplyStepParameters extends K8sApplyBaseStepInfo implements K8sStepParameters {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public K8sApplyStepParameters(ParameterField<String> timeout, ParameterField<Boolean> skipDryRun,
      ParameterField<Boolean> skipSteadyStateCheck, ParameterField<List<String>> filePaths, RollbackInfo rollbackInfo) {
    super(skipDryRun, skipSteadyStateCheck, filePaths);
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
  }
}
