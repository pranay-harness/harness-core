package io.harness.checks;

import com.google.common.collect.ImmutableMap;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import io.harness.checks.mixin.AnnotationMixin;
import io.harness.checks.mixin.ModifierMixin;

import java.util.Map;

public class AnnotationOrderCheck extends AbstractCheck {
  private static final String ORDER_MSG_KEY = "code.readability.annotation.order";
  private static final String MODIFIER_MSG_KEY = "code.readability.annotation.modifier";

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

  Map<String, Integer> annotationOrder = ImmutableMap.<String, Integer>builder()
                                             .put("Value", 1)
                                             .put("Data", 1)
                                             .put("Builder", 11)
                                             .put("NoArgsConstructor", 12)
                                             .put("AllArgsConstructor", 13)
                                             .put("ToString", 21)
                                             .put("EqualsAndHashCode", 22)
                                             .build();

  @Override
  public int[] getAcceptableTokens() {
    return getDefaultTokens();
  }

  @Override
  public void visitToken(DetailAST annotation) {
    final String name = AnnotationMixin.name(annotation);
    final Integer order = annotationOrder.get(name);

    DetailAST prevAnnotation = annotation.getPreviousSibling();
    if (order != null) {
      while (prevAnnotation != null && prevAnnotation.getType() == TokenTypes.ANNOTATION) {
        final String prevName = AnnotationMixin.name(prevAnnotation);
        prevAnnotation = prevAnnotation.getPreviousSibling();

        final Integer prevOrder = annotationOrder.get(prevName);
        if (prevOrder == null) {
          continue;
        }

        if (prevOrder > order) {
          log(annotation, ORDER_MSG_KEY, name, prevName);
        }
      }
    }

    if (prevAnnotation != null && ModifierMixin.isModifier(prevAnnotation)) {
      log(annotation, MODIFIER_MSG_KEY, name);
    }
  }
}