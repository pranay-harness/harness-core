package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;

import io.harness.steps.shellScript.k8s.K8sStepParameters;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("k8sCanaryStepParameters")
public class K8sCanaryStepParameters extends K8sCanaryBaseStepInfo implements K8sStepParameters {
  String name;
  String identifier;
  String description;
  ParameterField<String> skipCondition;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public K8sCanaryStepParameters(String name, String identifier, String description,
      ParameterField<String> skipCondition, InstanceSelectionWrapper instanceSelection, ParameterField<String> timeout,
      ParameterField<Boolean> skipDryRun, RollbackInfo rollbackInfo) {
    super(instanceSelection, skipDryRun);
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.skipCondition = skipCondition;
  }

  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toDocumentJson(K8sCanaryStepParameters.infoBuilder()
                                                       .instanceSelection(instanceSelection)
                                                       .skipDryRun(skipDryRun)
                                                       .timeout(timeout)
                                                       .name(name)
                                                       .identifier(identifier)
                                                       .skipCondition(skipCondition)
                                                       .description(description)
                                                       .build());
  }
}
