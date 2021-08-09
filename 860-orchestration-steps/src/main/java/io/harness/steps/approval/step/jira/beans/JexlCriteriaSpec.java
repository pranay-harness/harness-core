package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.SkipAutoEvaluation;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Data
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonTypeName(CriteriaSpecTypeConstants.JEXL)
@TypeAlias("jexlCriteriaSpec")
public class JexlCriteriaSpec implements CriteriaSpec {
  @NotNull
  @SkipAutoEvaluation
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH)
  private ParameterField<String> expression;

  @Override
  public CriteriaSpecType getType() {
    return CriteriaSpecType.JEXL;
  }

  @Override
  public CriteriaSpecDTO toCriteriaSpecDTO(boolean skipEmpty) {
    return JexlCriteriaSpecDTO.fromJexlCriteriaSpec(this, skipEmpty);
  }
}
