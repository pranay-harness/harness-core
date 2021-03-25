package io.harness.cdng.k8s;

import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.bool;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("K8sApplyBaseStepInfo")
public class K8sApplyBaseStepInfo {
  @NotNull @YamlSchemaTypes({string, bool}) ParameterField<Boolean> skipDryRun;
  @YamlSchemaTypes({string, bool}) ParameterField<Boolean> skipSteadyStateCheck;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) ParameterField<List<String>> filePaths;
}
