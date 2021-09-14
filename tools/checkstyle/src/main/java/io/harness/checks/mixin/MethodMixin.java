/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.checks.mixin;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class MethodMixin {
  public static DetailAST call(DetailAST ast, String name) {
    if (ast.getType() != TokenTypes.METHOD_CALL) {
      return null;
    }
    DetailAST dot = ast.getFirstChild();
    if (dot.getType() != TokenTypes.DOT) {
      return null;
    }

    DetailAST identifier = dot.getFirstChild();
    DetailAST method = identifier.getNextSibling();

    if (method.getType() != TokenTypes.IDENT || !method.getText().equals(name)) {
      return null;
    }

    return identifier;
  }
}
