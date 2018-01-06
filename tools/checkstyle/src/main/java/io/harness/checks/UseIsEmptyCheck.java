package io.harness.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class UseIsEmptyCheck extends AbstractCheck {
  private static final String MSG_KEY = "code.readability.use.is_empty";

  @Override
  public int[] getDefaultTokens() {
    return new int[] {
        TokenTypes.EXPR,
    };
  }

  @Override
  public int[] getRequiredTokens() {
    return getDefaultTokens();
  }

  @Override
  public int[] getAcceptableTokens() {
    return getDefaultTokens();
  }

  FullIdent equalNull(DetailAST ast) {
    if (ast.getType() != TokenTypes.EQUAL) {
      return null;
    }

    DetailAST ident = ast.getFirstChild();
    DetailAST nil = ident.getNextSibling();

    if (ident.getType() == TokenTypes.LITERAL_NULL) {
      DetailAST temp = ident;
      ident = nil;
      nil = temp;
    }

    if (ident.getType() != TokenTypes.IDENT || nil.getType() != TokenTypes.LITERAL_NULL) {
      return null;
    }

    return FullIdent.createFullIdent(ident);
  }

  FullIdent isEmptyCall(DetailAST ast) {
    if (ast.getType() != TokenTypes.METHOD_CALL) {
      return null;
    }
    DetailAST dot = ast.getFirstChild();
    if (dot.getType() != TokenTypes.DOT) {
      return null;
    }

    DetailAST ident = dot.getFirstChild();
    DetailAST method = ident.getNextSibling();

    if (method.getType() != TokenTypes.IDENT || !method.getText().equals("isEmpty")) {
      return null;
    }

    return FullIdent.createFullIdent(ident);
  }

  void checkForNullOrIsEmpty(DetailAST ast) {
    if (ast.getType() != TokenTypes.LOR) {
      return;
    }
    final DetailAST left = ast.getFirstChild();
    final FullIdent leftIdent = equalNull(left);
    if (leftIdent == null) {
      return;
    }
    final DetailAST right = left.getNextSibling();
    final FullIdent rightIdent = isEmptyCall(right);
    if (rightIdent == null) {
      return;
    }

    if (leftIdent.getText().equals(rightIdent.getText())) {
      log(ast, MSG_KEY);
    }
  }

  @Override
  public void visitToken(DetailAST ast) {
    if (ast.getParent().getType() != TokenTypes.LITERAL_IF) {
      return;
    }
    if (ast.getChildCount() == 0) {
      return;
    }

    checkForNullOrIsEmpty(ast.getFirstChild());
  }
}