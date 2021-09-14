/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.checks.mixin;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import java.util.HashMap;
import java.util.Map;

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

  public static Map<String, DetailAST> parameters(DetailAST annotationNode) {
    final Map<String, DetailAST> annotationParameters = new HashMap<>();
    DetailAST annotationChildNode = annotationNode.getFirstChild();

    while (annotationChildNode != null) {
      if (annotationChildNode.getType() == TokenTypes.ANNOTATION_MEMBER_VALUE_PAIR) {
        annotationParameters.put(annotationChildNode.getFirstChild().getText(), annotationChildNode);
      } else if (annotationChildNode.getType() == TokenTypes.EXPR) {
        annotationParameters.put("", annotationChildNode);
      }

      annotationChildNode = annotationChildNode.getNextSibling();
    }
    return annotationParameters;
  }

  public static String obtainDefaultValue(DetailAST expression) {
    final DetailAST literal = expression.getFirstChild();
    return literal.getText();
  }
}
