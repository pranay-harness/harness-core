package io.harness.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class WingsExceptionCheck extends AbstractCheck {
  private static final String MSG_KEY = "exception.use.specific.class";

  @Override
  public int[] getDefaultTokens() {
    return new int[] {
        TokenTypes.IDENT,
    };
  }

  @Override
  public int[] getRequiredTokens() {
    return new int[] {
        TokenTypes.IDENT,
    };
  }

  @Override
  public int[] getAcceptableTokens() {
    return getDefaultTokens();
  }

  @Override
  public void visitToken(DetailAST identifier) {
    if (!identifier.getText().equals("INVALID_REQUEST")) {
      return;
    }

    DetailAST expression = identifier.getParent();
    if (expression.getType() == TokenTypes.DOT) {
      expression = expression.getParent();
    }
    if (expression.getType() != TokenTypes.EXPR) {
      return;
    }

    final DetailAST elist = expression.getParent();
    if (elist.getType() != TokenTypes.ELIST) {
      return;
    }

    final DetailAST exception = elist.getPreviousSibling().getPreviousSibling();
    if (exception == null || exception.getType() != TokenTypes.IDENT || !exception.getText().equals("WingsException")) {
      return;
    }

    log(identifier, MSG_KEY, "InvalidRequestException");
  }
}