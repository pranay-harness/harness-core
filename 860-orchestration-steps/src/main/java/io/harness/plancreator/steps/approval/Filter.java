package io.harness.plancreator.steps.approval;

import io.harness.pms.yaml.ParameterField;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

@Value
@Builder
public class Filter {
  public enum Operator { EQ, NOT_EQ, CONTAINS, IN, NOT_IN }
  @NotEmpty String fieldName;
  @NotNull List<ParameterField<String>> fieldValues;
  @NotNull Operator op;
}
