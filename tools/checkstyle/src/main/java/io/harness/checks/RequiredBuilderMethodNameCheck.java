package io.harness.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.Set;
import java.util.TreeSet;

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
    final String annotationNameCheck = getAnnotationName(annotationNode);

    if (annotationNameCheck.equals(ANNOTATION_NAME)) {
      if (!getAnnotationParameters(annotationNode).contains(BUILDER_METHOD_NAME)) {
        log(annotationNode, MSG_KEY);
      }
    }
  }

  /**
   * Returns full name of an annotation.
   * @param annotationNode The node to examine.
   * @return name of an annotation.
   */
  private static String getAnnotationName(DetailAST annotationNode) {
    final DetailAST identNode = annotationNode.findFirstToken(TokenTypes.IDENT);
    final String result;

    if (identNode != null) {
      result = identNode.getText();
    } else {
      final StringBuilder builder = new StringBuilder();
      DetailAST separationDotNode = annotationNode.findFirstToken(TokenTypes.DOT);
      while (separationDotNode.getType() == TokenTypes.DOT) {
        builder.insert(0, '.').insert(1, separationDotNode.getLastChild().getText());
        separationDotNode = separationDotNode.getFirstChild();
      }
      builder.insert(0, separationDotNode.getText());
      result = builder.toString();
    }
    return result;
  }

  /**
   * Returns the name of annotations properties.
   * @param annotationNode The node to examine.
   * @return name of annotation properties.
   */
  private static Set<String> getAnnotationParameters(DetailAST annotationNode) {
    final Set<String> annotationParameters = new TreeSet<>();
    DetailAST annotationChildNode = annotationNode.getFirstChild();

    while (annotationChildNode != null) {
      if (annotationChildNode.getType() == TokenTypes.ANNOTATION_MEMBER_VALUE_PAIR) {
        annotationParameters.add(annotationChildNode.getFirstChild().getText());
      }
      annotationChildNode = annotationChildNode.getNextSibling();
    }
    return annotationParameters;
  }
}