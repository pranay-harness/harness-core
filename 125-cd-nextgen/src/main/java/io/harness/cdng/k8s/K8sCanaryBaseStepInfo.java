package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.beans.SwaggerConstants.BOOLEAN_CLASSPATH;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("K8sCanaryBaseStepInfo")
@FieldNameConstants(innerTypeName = "K8sCanaryBaseStepInfoKeys")
public class K8sCanaryBaseStepInfo {
  @NotNull InstanceSelectionWrapper instanceSelection;
  @ApiModelProperty(dataType = BOOLEAN_CLASSPATH) @YamlSchemaTypes({string}) ParameterField<Boolean> skipDryRun;
  @YamlSchemaTypes({runtime})
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<TaskSelectorYaml>> delegateSelectors;
}
