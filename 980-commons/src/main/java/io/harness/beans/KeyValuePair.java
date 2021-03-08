package io.harness.beans;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyValuePair implements ExpressionReflectionUtils.NestedAnnotationResolver {
  @Expression(ALLOW_SECRETS) private String key;
  @Expression(ALLOW_SECRETS) private String value;
}
