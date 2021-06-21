package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("terraformApplyBaseStepInfo")
public class TerraformApplyBaseStepInfo {
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  private ParameterField<String> provisionerIdentifier;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  private ParameterField<List<String>> delegateSelectors;
}
