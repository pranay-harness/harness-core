package io.harness.steps.common.script;

import io.harness.common.SwaggerConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
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
@TypeAlias("shellScriptStepParameters")
public class ShellScriptStepParameters extends ShellScriptBaseStepInfo implements StepParameters {
  String name;
  String identifier;
  String description;
  ParameterField<String> skipCondition;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;

  @Builder(builderMethodName = "infoBuilder")
  public ShellScriptStepParameters(String name, String identifier, String description,
      ParameterField<String> skipCondition, ShellType shellType, ShellScriptSourceWrapper source,
      List<NGVariable> environmentVariables, List<NGVariable> outputVariables, ExecutionTarget executionTarget,
      ParameterField<String> timeout, ParameterField<Boolean> onDelegate, RollbackInfo rollbackInfo) {
    super(shellType, source, environmentVariables, outputVariables, executionTarget, onDelegate);
    this.name = name;
    this.identifier = identifier;
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
    this.description = description;
    this.skipCondition = skipCondition;
  }

  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toDocumentJson(ShellScriptStepParameters.infoBuilder()
                                                       .environmentVariables(getEnvironmentVariables())
                                                       .executionTarget(getExecutionTarget())
                                                       .onDelegate(getOnDelegate())
                                                       .outputVariables(getOutputVariables())
                                                       .environmentVariables(getEnvironmentVariables())
                                                       .shellType(getShell())
                                                       .source(getSource())
                                                       .timeout(getTimeout())
                                                       .name(getName())
                                                       .identifier(getIdentifier())
                                                       .description(getDescription())
                                                       .skipCondition(getSkipCondition())
                                                       .build());
  }
}
