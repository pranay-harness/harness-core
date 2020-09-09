package io.harness.cdng.variables;

import io.harness.cdng.visitor.helpers.VariableVisitorHelper;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
@SimpleVisitorHelper(helperClass = VariableVisitorHelper.class)
public class Variable {
  @NotNull private String name;
  @NotNull private String value;
  @NotNull private String type;
}
