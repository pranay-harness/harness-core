package io.harness.expression;

import static java.lang.String.format;

import io.harness.data.encoding.EncodingUtils;
import io.harness.exception.InvalidRequestException;

public class ImageSecretFunctor implements ExpressionFunctor {
  public static final String FUNCTOR_NAME = "imageSecret";
  private static final String REGISTRY_CREDENTIAL_TEMPLATE = "{\"%s\":{\"username\":\"%s\",\"password\":\"%s\"}}";

  public String create(String registryUrl, String userName, String password) {
    if (ExpressionEvaluator.matchesVariablePattern(registryUrl) || ExpressionEvaluator.matchesVariablePattern(userName)
        || ExpressionEvaluator.matchesVariablePattern(password)) {
      throw new InvalidRequestException("Arguments cannot be expression");
    }
    return EncodingUtils.encodeBase64(
        format(REGISTRY_CREDENTIAL_TEMPLATE, registryUrl, userName, password.replaceAll("\"", "\\\\\"")));
  }
}
