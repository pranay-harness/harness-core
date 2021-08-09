package io.harness.http;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HttpHeaderConfig implements NestedAnnotationResolver {
  @Expression(ALLOW_SECRETS) String key;
  @Expression(ALLOW_SECRETS) String value;
}
