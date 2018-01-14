package io.harness.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class UseIsEmptyCheck extends AbstractCheck {
  private static final String UTIL_NULL_OR_EMPTY_MSG_KEY = "code.readability.util.null_or_empty";
  private static final String UTIL_NOT_NULL_AND_NOT_EMPTY_MSG_KEY = "code.readability.util.not_null_and_not_empty";

  @Override
  public int[] getDefaultTokens() {
    return new int[] {
        TokenTypes.LOR,
        TokenTypes.LAND,
        TokenTypes.EQUAL,
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

  DetailAST methodCall(DetailAST ast, String name) {
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

  interface AstPredicate {
    boolean check(DetailAST ast);
  }

  DetailAST transitiveOperation(DetailAST ast, int operation, AstPredicate object, AstPredicate something) {
    if (ast.getType() != operation) {
      return null;
    }

    DetailAST first = ast.getFirstChild();
    DetailAST second = first.getNextSibling();

    if (object.check(first) && something.check(second)) {
      return first;
    }

    if (object.check(second) && something.check(first)) {
      return second;
    }

    return null;
  }

  DetailAST identifierEqualNull(DetailAST ast) {
    return transitiveOperation(ast, TokenTypes.EQUAL,
        identifier
        -> identifier.getType() == TokenTypes.IDENT,
        constant -> constant.getType() == TokenTypes.LITERAL_NULL);
  }
  DetailAST identifierNotEqualNull(DetailAST ast) {
    return transitiveOperation(ast, TokenTypes.NOT_EQUAL,
        identifier
        -> identifier.getType() == TokenTypes.IDENT,
        constant -> constant.getType() == TokenTypes.LITERAL_NULL);
  }

  DetailAST methodEqualZero(DetailAST ast) {
    return transitiveOperation(ast, TokenTypes.EQUAL,
        method
        -> method.getType() == TokenTypes.METHOD_CALL,
        constant -> constant.getType() == TokenTypes.NUM_INT && constant.getText().equals("0"));
  }

  void checkForNullOrIsEmpty(DetailAST ast) {
    if (ast.getType() != TokenTypes.LOR) {
      return;
    }
    final DetailAST left = ast.getFirstChild();
    final DetailAST leftIdentifier = identifierEqualNull(left);
    if (leftIdentifier == null) {
      return;
    }
    final DetailAST right = left.getNextSibling();
    final DetailAST rightIdentifier = methodCall(right, "isEmpty");
    if (rightIdentifier == null) {
      return;
    }

    if (!FullIdent.createFullIdent(leftIdentifier)
             .getText()
             .equals(FullIdent.createFullIdent(rightIdentifier).getText())) {
      return;
    }

    DetailAST parent = ast.getParent();
    while (parent != null) {
      if (parent.getType() == TokenTypes.LITERAL_RETURN) {
        return;
      }
      parent = parent.getParent();
    }

    log(ast, UTIL_NULL_OR_EMPTY_MSG_KEY);
  }

  void checkForNotNullAndIsNotEmpty(DetailAST ast) {
    if (ast.getType() != TokenTypes.LAND) {
      return;
    }
    final DetailAST left = ast.getFirstChild();
    final DetailAST leftIdentifier = identifierNotEqualNull(left);
    if (leftIdentifier == null) {
      return;
    }
    final DetailAST right = left.getNextSibling();
    DetailAST rightIdentifier = right.getType() != TokenTypes.LNOT ? methodCall(right, "isNotEmpty")
                                                                   : methodCall(right.getFirstChild(), "isEmpty");
    if (rightIdentifier == null) {
      return;
    }

    if (!FullIdent.createFullIdent(leftIdentifier)
             .getText()
             .equals(FullIdent.createFullIdent(rightIdentifier).getText())) {
      return;
    }

    DetailAST parent = ast.getParent();
    while (parent != null) {
      if (parent.getType() == TokenTypes.LITERAL_RETURN) {
        return;
      }
      parent = parent.getParent();
    }

    log(ast, UTIL_NOT_NULL_AND_NOT_EMPTY_MSG_KEY);
  }

  @Override
  public void visitToken(DetailAST ast) {
    checkForNullOrIsEmpty(ast);
    checkForNotNullAndIsNotEmpty(ast);
  }
}