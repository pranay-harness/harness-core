package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;

import lombok.Builder;
import lombok.ToString;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
@ToString(exclude = {"fileContent"})
public class FileData implements NestedAnnotationResolver {
  String filePath;
  byte[] fileBytes;
  String fileName;
  @Expression(ALLOW_SECRETS) String fileContent;
}
