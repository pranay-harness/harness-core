package io.harness.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import io.harness.checks.mixin.AnnotationMixin;

public class RequiredBuilderMethodNameCheck extends AbstractCheck {
  private static final String MSG_KEY = "annotation.builder.missing.method.name";

  private static final String ANNOTATION_NAME = "Builder";

  private static final String BUILDER_METHOD_NAME = "builderMethodName";

  @Override
  public int[] getDefaultTokens() {
    return new int[] {
        TokenTypes.ANNOTATION,
    };
  }

  @Override
  public int[] getRequiredTokens() {
    return new int[] {
        TokenTypes.ANNOTATION,
    };
  }

  @Override
  public int[] getAcceptableTokens() {
    return getDefaultTokens();
  }

  @Override
  public void visitToken(DetailAST annotationNode) {
    final String annotationNameCheck = AnnotationMixin.name(annotationNode);

    if (annotationNameCheck.equals(ANNOTATION_NAME)) {
      if (!AnnotationMixin.parameters(annotationNode).contains(BUILDER_METHOD_NAME)) {
        log(annotationNode, MSG_KEY);
      }
    }
  }
}