package io.harness.steps.shellScript;

import io.harness.common.SwaggerConstants;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.pms.sdk.core.steps.io.RollbackInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
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
  String type = StepSpecTypeConstants.SHELL_SCRIPT;
  String description;
  ParameterField<String> skipCondition;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> timeout;
  RollbackInfo rollbackInfo;
  Map<String, Object> outputVariables;
  Map<String, Object> environmentVariables;

  @Builder(builderMethodName = "infoBuilder")
  public ShellScriptStepParameters(ShellType shellType, ShellScriptSourceWrapper source,
      ExecutionTarget executionTarget, ParameterField<Boolean> onDelegate, String name, String identifier, String type,
      String description, ParameterField<String> skipCondition, ParameterField<String> timeout,
      RollbackInfo rollbackInfo, Map<String, Object> outputVariables, Map<String, Object> environmentVariables) {
    super(shellType, source, executionTarget, onDelegate);
    this.name = name;
    this.identifier = identifier;
    this.type = type;
    this.description = description;
    this.skipCondition = skipCondition;
    this.timeout = timeout;
    this.rollbackInfo = rollbackInfo;
    this.outputVariables = outputVariables;
    this.environmentVariables = environmentVariables;
    this.type = StepSpecTypeConstants.SHELL_SCRIPT;
  }

  @Override
  public String toViewJson() {
    return RecastOrchestrationUtils.toDocumentJson(ShellScriptStepParameters.infoBuilder()
                                                       .environmentVariables(environmentVariables)
                                                       .executionTarget(getExecutionTarget())
                                                       .onDelegate(getOnDelegate())
                                                       .outputVariables(outputVariables)
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
