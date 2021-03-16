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
@TypeAlias("K8sScaleStepParameter")
public class K8sScaleStepParameter extends K8sScaleBaseStepInfo implements K8sStepParameters {
  String name;
  String identifier;
  String description;
  ParameterField<String> skipCondition;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public K8sScaleStepParameter(String name, String identifier, String description, ParameterField<String> skipCondition,
      ParameterField<String> timeout, ParameterField<Boolean> skipDryRun, ParameterField<Boolean> skipSteadyStateCheck,
      InstanceSelectionWrapper instanceSelection, ParameterField<String> workload, RollbackInfo rollbackInfo) {
    super(instanceSelection, workload, skipDryRun, skipSteadyStateCheck);
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.skipCondition = skipCondition;
  }

  @Nonnull
  @Override
  @JsonIgnore
  public List<String> getCommandUnits() {
    if (!ParameterField.isNull(skipSteadyStateCheck) && skipSteadyStateCheck.getValue()) {
      return Arrays.asList(K8sCommandUnitConstants.Init, K8sCommandUnitConstants.Scale);
    } else {
      return Arrays.asList(
          K8sCommandUnitConstants.Init, K8sCommandUnitConstants.Scale, K8sCommandUnitConstants.WaitForSteadyState);
    }
  }

  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toDocumentJson(K8sScaleStepParameter.infoBuilder()
                                                       .instanceSelection(instanceSelection)
                                                       .workload(workload)
                                                       .skipSteadyStateCheck(skipSteadyStateCheck)
                                                       .timeout(timeout)
                                                       .rollbackInfo(rollbackInfo)
                                                       .name(name)
                                                       .identifier(identifier)
                                                       .skipCondition(skipCondition)
                                                       .description(description)
                                                       .build());
  }
}
