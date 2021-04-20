package io.harness.delegate.task.helm;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;
import io.harness.helm.HelmSubCommandType;

import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HelmCommandFlag implements NestedAnnotationResolver {
  @NotNull @Expression(ALLOW_SECRETS) private Map<HelmSubCommandType, String> valueMap;
}
