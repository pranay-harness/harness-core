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
