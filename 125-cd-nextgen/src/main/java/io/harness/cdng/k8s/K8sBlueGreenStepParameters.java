package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.common.SwaggerConstants;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("K8sBlueGreenStepParameters")
public class K8sBlueGreenStepParameters extends K8sBlueGreenBaseStepInfo implements K8sStepParameters {
  String name;
  String identifier;

  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toDocumentJson(K8sBlueGreenStepParameters.infoBuilder()
                                                       .skipDryRun(skipDryRun)
                                                       .timeout(timeout)
                                                       .name(name)
                                                       .identifier(identifier)
                                                       .skipCondition(skipCondition)
                                                       .description(description)
                                                       .build());
  }

  String description;
  ParameterField<String> skipCondition;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;

  @Builder(builderMethodName = "infoBuilder")
  public K8sBlueGreenStepParameters(String name, String identifier, String description,
      ParameterField<String> skipCondition, ParameterField<String> timeout, ParameterField<Boolean> skipDryRun) {
    super(skipDryRun);
    this.timeout = timeout;
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.skipCondition = skipCondition;
  }
}
