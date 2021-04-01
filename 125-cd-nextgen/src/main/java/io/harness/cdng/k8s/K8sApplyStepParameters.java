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
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("k8sApplyStepParameters")
public class K8sApplyStepParameters extends K8sApplyBaseStepInfo implements K8sStepParameters {
  String name;
  String identifier;
  String description;
  ParameterField<String> skipCondition;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public K8sApplyStepParameters(String name, String identifier, String description,
      ParameterField<String> skipCondition, ParameterField<String> timeout, ParameterField<Boolean> skipDryRun,
      ParameterField<Boolean> skipSteadyStateCheck, ParameterField<List<String>> filePaths, RollbackInfo rollbackInfo) {
    super(skipDryRun, skipSteadyStateCheck, filePaths);
    this.name = name;
    this.identifier = identifier;
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
    this.description = description;
    this.skipCondition = skipCondition;
  }

  @Nonnull
  @Override
  @JsonIgnore
  public List<String> getCommandUnits() {
    if (!ParameterField.isNull(skipSteadyStateCheck) && skipSteadyStateCheck.getValue()) {
      return Arrays.asList(K8sCommandUnitConstants.FetchFiles, K8sCommandUnitConstants.Init,
          K8sCommandUnitConstants.Prepare, K8sCommandUnitConstants.Apply, K8sCommandUnitConstants.WrapUp);
    } else {
      return Arrays.asList(K8sCommandUnitConstants.FetchFiles, K8sCommandUnitConstants.Init,
          K8sCommandUnitConstants.Prepare, K8sCommandUnitConstants.Apply, K8sCommandUnitConstants.WaitForSteadyState,
          K8sCommandUnitConstants.WrapUp);
    }
  }

  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toDocumentJson(K8sApplyStepParameters.infoBuilder()
                                                       .filePaths(this.getFilePaths())
                                                       .skipDryRun(this.getSkipDryRun())
                                                       .skipSteadyStateCheck(skipSteadyStateCheck)
                                                       .timeout(timeout)
                                                       .name(name)
                                                       .identifier(identifier)
                                                       .skipCondition(skipCondition)
                                                       .description(description)
                                                       .build());
  }
}
