package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("k8sRollingRollbackStepParameters")
public class K8sRollingRollbackStepParameters extends K8sRollingRollbackBaseStepInfo implements K8sStepParameters {
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) private ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public K8sRollingRollbackStepParameters(
      ParameterField<String> timeout, ParameterField<Boolean> skipDryRun, RollbackInfo rollbackInfo) {
    super(skipDryRun);
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
  }
}
