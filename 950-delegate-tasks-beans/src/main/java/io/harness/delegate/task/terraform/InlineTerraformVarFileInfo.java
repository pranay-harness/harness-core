package io.harness.delegate.task.terraform;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
public class InlineTerraformVarFileInfo implements TerraformVarFileInfo, NestedAnnotationResolver {
  @Expression(ALLOW_SECRETS) String varFileContent;
}
