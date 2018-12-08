package io.harness.checks;

import com.google.common.collect.ImmutableMap;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import io.harness.checks.mixin.AnnotationMixin;

import java.util.Map;

public class AnnotationOrderCheck extends AbstractCheck {
  private static final String MSG_KEY = "code.readability.annotation.order";

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

  Map<String, Integer> annotationOrder =
      ImmutableMap.<String, Integer>builder().put("Value", 1).put("Data", 1).put("Builder", 10).build();

  @Override
  public int[] getAcceptableTokens() {
    return getDefaultTokens();
  }

  @Override
  public void visitToken(DetailAST annotation) {
    final String name = AnnotationMixin.name(annotation);
    final Integer order = annotationOrder.get(name);
    if (order == null) {
      return;
    }

    DetailAST prevAnnotation = annotation.getPreviousSibling();
    while (prevAnnotation != null && prevAnnotation.getType() == TokenTypes.ANNOTATION) {
      final String prevName = AnnotationMixin.name(prevAnnotation);
      prevAnnotation = prevAnnotation.getPreviousSibling();

      final Integer prevOrder = annotationOrder.get(prevName);
      if (prevOrder == null) {
        continue;
      }

      if (prevOrder > order) {
        log(annotation, MSG_KEY, name, prevName);
      }
    }
  }
}