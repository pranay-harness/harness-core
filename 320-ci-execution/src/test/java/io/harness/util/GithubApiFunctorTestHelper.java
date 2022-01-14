package io.harness.util;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import lombok.Builder;
import lombok.Value;

@OwnedBy(HarnessTeam.CI)
public class GithubApiFunctorTestHelper {
  public static final String EXPRESSION = "<+gitApp.token()>";
  public static final String RESOLVED_EXPRESSION_REGEX = "\\$\\{GIT_APP_TOKEN_[0123456789ABCDEF]+}";

  public static TestObject getTestObject() {
    return GithubApiFunctorTestHelper.TestObject.builder()
        .expression(ParameterField.createExpressionField(true, EXPRESSION, null, true))
        .expressionAsString(EXPRESSION)
        .build();
  }

  @Value
  @Builder
  public static class TestObject {
    ParameterField<String> expression;
    String expressionAsString;
  }
}
