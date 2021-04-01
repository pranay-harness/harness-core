package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode
@TypeAlias("k8sCanaryDeleteParameters")
public class K8sCanaryDeleteStepParameters implements K8sStepParameters {
  String name;
  String identifier;
  String description;
  ParameterField<String> skipCondition;
  ParameterField<Boolean> skipDryRun;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public K8sCanaryDeleteStepParameters(String name, String identifier, String description,
      ParameterField<String> skipCondition, ParameterField<String> timeout, ParameterField<Boolean> skipDryRun,
      RollbackInfo rollbackInfo) {
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.skipCondition = skipCondition;
    this.timeout = timeout;
    this.skipDryRun = skipDryRun;
    this.rollbackInfo = rollbackInfo;
  }

  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toDocumentJson(K8sCanaryDeleteStepParameters.infoBuilder()
                                                       .name(name)
                                                       .description(description)
                                                       .skipCondition(skipCondition)
                                                       .timeout(timeout)
                                                       .build());
  }

  @Override
  public List<String> getCommandUnits() {
    return Arrays.asList(K8sCommandUnitConstants.Init, K8sCommandUnitConstants.Delete);
  }
}
