package io.harness.plancreator.steps.approval;

import io.harness.pms.yaml.ParameterField;

import io.swagger.annotations.ApiModel;
import java.util.List;
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
@ApiModel("KeyValues")
public class KeyValueCriteria implements Criteria {
  @NotEmpty private String type;
  @NotNull private KeyValueCriteriaSpec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class KeyValueCriteriaSpec {
    @NotNull private ParameterField<Boolean> matchAnyFilter;
    @NotNull private List<Filter> filters;
  }
}
