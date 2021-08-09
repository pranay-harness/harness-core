package software.wings.expression;

import static java.lang.String.format;

// This functor is only to assure compatibility between all SecretManagerFunctors
public interface SecretManagerFunctorInterface {
  String FUNCTOR_NAME = "secretManager";

  static String obtainExpression(String secretName, int expressionFunctorToken) {
    return format("${%s.obtain(\"%s\", %d)}", FUNCTOR_NAME, secretName, expressionFunctorToken);
  }

  Object obtain(String secretName, int token);

  static String obtainConfigFileExpression(
      String method, String path, String encryptedFileId, int expressionFunctorToken) {
    return format("${%s.%s(\"%s\", \"%s\", %d)}", FUNCTOR_NAME, method, path, encryptedFileId, expressionFunctorToken);
  }

  Object obtainConfigFileAsString(String path, String encryptedFileId, int token);
  Object obtainConfigFileAsBase64(String path, String encryptedFileId, int token);
}
