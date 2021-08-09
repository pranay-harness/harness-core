package io.harness.checks.mixin;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

public class GeneralMixin {
  public static boolean same(DetailAST ast1, DetailAST ast2) {
    if (!ast1.getText().equals(ast2.getText())) {
      return false;
    }
    if (ast1.getType() != ast2.getType()) {
      return false;
    }
    if (ast1.getChildCount() != ast2.getChildCount()) {
      return false;
    }

    DetailAST child1 = ast1.getFirstChild();
    DetailAST child2 = ast2.getFirstChild();

    for (int i = 0; i < ast1.getChildCount(); i++) {
      if (!same(child1, child2)) {
        return false;
      }
      child1 = child1.getNextSibling();
      child2 = child2.getNextSibling();
    }

    return true;
  }
}
