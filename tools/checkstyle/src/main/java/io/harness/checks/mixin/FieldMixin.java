/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.checks.mixin;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class FieldMixin {
  public static DetailAST field(DetailAST ast, String name) {
    if (ast.getType() != TokenTypes.DOT) {
      return null;
    }

    final DetailAST statement = ast.getFirstChild();
    final DetailAST field = statement.getNextSibling();
    if (field.getType() != TokenTypes.IDENT || !field.getText().equals(name)) {
      return null;
    }

    return statement;
  }
}
