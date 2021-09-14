/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class StringLiteralsCheck extends AbstractCheck {
  private static final String MSG_KEY = "code.readability.string.literal.unallowed";

  @Override
  public int[] getDefaultTokens() {
    return new int[] {
        TokenTypes.STRING_LITERAL,
    };
  }

  @Override
  public int[] getRequiredTokens() {
    return new int[] {
        TokenTypes.STRING_LITERAL,
    };
  }

  @Override
  public int[] getAcceptableTokens() {
    return getDefaultTokens();
  }

  @Override
  public void visitToken(DetailAST literal) {
    DetailAST expression = literal.getParent();
    if (expression == null || expression.getType() != TokenTypes.EXPR) {
      return;
    }

    int index = 0;

    while (expression.getPreviousSibling() != null) {
      expression = expression.getPreviousSibling();
      if (expression == null) {
        return;
      }
      expression = expression.getPreviousSibling();
      if (expression == null) {
        return;
      }
      index++;
    }

    DetailAST elist = expression.getParent();
    if (elist == null || elist.getType() != TokenTypes.ELIST) {
      return;
    }

    DetailAST dot = elist.getPreviousSibling();
    if (dot == null || dot.getType() != TokenTypes.DOT) {
      return;
    }

    DetailAST method = dot.getFirstChild();
    if (method == null || method.getType() != TokenTypes.METHOD_CALL) {
      return;
    }

    DetailAST name = method.getNextSibling();
    if (name == null || name.getType() != TokenTypes.IDENT) {
      return;
    }

    if (index == 0 && "filter".equals(name.getText())) {
      log(literal, MSG_KEY);
    }
  }
}
