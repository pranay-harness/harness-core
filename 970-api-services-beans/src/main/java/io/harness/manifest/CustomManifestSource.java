package io.harness.manifest;

import static io.harness.expression.Expression.ALLOW_SECRETS;
import static io.harness.expression.Expression.DISALLOW_SECRETS;

import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;

import java.util.List;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomManifestSource implements NestedAnnotationResolver {
  @Expression(ALLOW_SECRETS) @Nullable String script;
  @Expression(DISALLOW_SECRETS) List<String> filePaths;
}
