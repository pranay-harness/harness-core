/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.checks;

import io.harness.checks.mixin.AnnotationMixin;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class TestAnnotationsCheck extends AbstractCheck {
  private static final String MISSING_CATEGORY_MSG_KEY = "code.best.practice.test.missing_category";

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
  public void visitToken(DetailAST annotation) {
    final String name = AnnotationMixin.name(annotation);
    if (!name.equals("Test")) {
      return;
    }

    DetailAST nextAnnotation = annotation.getNextSibling();
    while (nextAnnotation != null) {
      if (nextAnnotation.getType() != TokenTypes.ANNOTATION) {
        log(annotation, MISSING_CATEGORY_MSG_KEY, name);
        return;
      }

      final String nextAnnotationName = AnnotationMixin.name(nextAnnotation);
      if (nextAnnotationName.equals("Category")) {
        return;
      }
      nextAnnotation = nextAnnotation.getNextSibling();
    }
    log(annotation, MISSING_CATEGORY_MSG_KEY, name);
  }
}
