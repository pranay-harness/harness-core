package io.harness.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class ForEachCheck extends AbstractCheck {
  private static final String MSG_KEY = "performance.for_each.stream";

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
    if (!identifier.getText().equals("forEach")) {
      return;
    }

    DetailAST forEachDot = identifier.getParent();
    if (forEachDot.getType() != TokenTypes.DOT) {
      return;
    }

    DetailAST call = forEachDot.getFirstChild();
    if (call.getType() != TokenTypes.METHOD_CALL) {
      return;
    }

    DetailAST streamDot = call.getFirstChild();
    if (streamDot.getType() != TokenTypes.DOT) {
      return;
    }

    DetailAST stream = streamDot.getFirstChild().getNextSibling();
    if (stream.getType() != TokenTypes.IDENT || !stream.getText().equals("stream")) {
      return;
    }

    log(identifier, MSG_KEY, "SuboptimalUseOfForEach");
  }
}