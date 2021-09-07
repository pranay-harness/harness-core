package io.harness.cdng.k8s;

import static io.harness.beans.SwaggerConstants.BOOLEAN_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("K8sApplyBaseStepInfo")
@FieldNameConstants(innerTypeName = "K8sApplyBaseStepInfoKeys")
public class K8sApplyBaseStepInfo {
  @ApiModelProperty(dataType = BOOLEAN_CLASSPATH) @YamlSchemaTypes({string}) ParameterField<Boolean> skipDryRun;
  @ApiModelProperty(dataType = BOOLEAN_CLASSPATH)
  @YamlSchemaTypes({string})
  ParameterField<Boolean> skipSteadyStateCheck;
  @YamlSchemaTypes(runtime)
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<String>> filePaths;
  @YamlSchemaTypes({runtime})
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<TaskSelectorYaml>> delegateSelectors;
}
