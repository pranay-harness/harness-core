package io.harness.cdng.k8s;

import io.harness.common.SwaggerConstants;
import io.harness.k8s.K8sCommandUnitConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nonnull;
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
  String name;
  String identifier;
  String description;
  ParameterField<String> skipCondition;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) private ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public K8sRollingRollbackStepParameters(String name, String identifier, String description,
      ParameterField<String> skipCondition, ParameterField<String> timeout, ParameterField<Boolean> skipDryRun,
      RollbackInfo rollbackInfo) {
    super(skipDryRun);
    this.timeout = timeout;
    this.skipCondition = skipCondition;
    this.rollbackInfo = rollbackInfo;
    this.name = name;
    this.identifier = identifier;
    this.name = name;
    this.description = description;
  }

  @Nonnull
  @Override
  @JsonIgnore
  public List<String> getCommandUnits() {
    return Arrays.asList(
        K8sCommandUnitConstants.Init, K8sCommandUnitConstants.Rollback, K8sCommandUnitConstants.WaitForSteadyState);
  }

  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toDocumentJson(K8sRollingRollbackStepParameters.infoBuilder()
                                                       .timeout(timeout)
                                                       .name(name)
                                                       .identifier(identifier)
                                                       .skipCondition(skipCondition)
                                                       .description(description)
                                                       .build());
  }
}
