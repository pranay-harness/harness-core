package io.harness.checks.mixin;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.Set;
import java.util.TreeSet;

public class AnnotationMixin {
  public static String name(DetailAST annotationNode) {
    final DetailAST identifierNode = annotationNode.findFirstToken(TokenTypes.IDENT);
    final String result;

    if (identifierNode != null) {
      result = identifierNode.getText();
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

  public static Set<String> parameters(DetailAST annotationNode) {
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
