package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode
@TypeAlias("k8sBGSwapServicesStepParameters")
public class K8sBGSwapServicesStepParameters implements K8sStepParameters {
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> skipDryRun;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public K8sBGSwapServicesStepParameters(
      ParameterField<String> timeout, ParameterField<Boolean> skipDryRun, RollbackInfo rollbackInfo) {
    this.timeout = timeout;
    this.skipDryRun = skipDryRun;
    this.rollbackInfo = rollbackInfo;
  }
}
