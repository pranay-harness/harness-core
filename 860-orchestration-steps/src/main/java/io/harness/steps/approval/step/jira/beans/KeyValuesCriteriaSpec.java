package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
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
@JsonTypeName(CriteriaSpecTypeConstants.KEY_VALUES)
@TypeAlias("keyValuesCriteriaSpec")
public class KeyValuesCriteriaSpec implements CriteriaSpec {
  @YamlSchemaTypes({string})
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
  private ParameterField<Boolean> matchAnyCondition;

  @NotNull private List<Condition> conditions;

  @Override
  public CriteriaSpecType getType() {
    return CriteriaSpecType.KEY_VALUES;
  }

  @Override
  public CriteriaSpecDTO toCriteriaSpecDTO(boolean skipEmpty) {
    return KeyValuesCriteriaSpecDTO.fromKeyValueCriteria(this, skipEmpty);
  }
}
