package io.harness.yaml.core.variables;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.validator.NGRegexValidatorConstants;
import io.harness.validator.NGVariableName;
import io.harness.visitor.helpers.variables.OutputVariableVisitorHelper;
import io.harness.walktree.visitor.SimpleVisitorHelper;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SimpleVisitorHelper(helperClass = OutputVariableVisitorHelper.class)
@TypeAlias("io.harness.yaml.core.variables.OutputNGVariable")
@OwnedBy(CDC)
public class OutputNGVariable {
  @NGVariableName @Pattern(regexp = NGRegexValidatorConstants.IDENTIFIER_PATTERN) String name;
  String description;
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;
}
