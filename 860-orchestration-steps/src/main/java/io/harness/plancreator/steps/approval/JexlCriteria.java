package io.harness.plancreator.steps.approval;

import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModel;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("Jexl")
public class JexlCriteria implements Criteria {
  @NotEmpty private String type;
  @NotNull private JexlCriteriaSpec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class JexlCriteriaSpec {
    @NotNull private ParameterField<String> expression;
  }
}
