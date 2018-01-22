package io.harness.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import io.harness.checks.mixin.FieldMixin;
import io.harness.checks.mixin.GeneralMixin;
import io.harness.checks.mixin.MethodMixin;
import io.harness.checks.mixin.OperationMixin;

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

  void checkForNullOrIsEmpty(DetailAST ast) {
    if (ast.getType() != TokenTypes.LOR) {
      return;
    }
    final DetailAST left = ast.getFirstChild();
    final DetailAST leftStatement = OperationMixin.equalNull(left);
    if (leftStatement == null) {
      return;
    }
    final DetailAST right = left.getNextSibling();
    DetailAST rightStatement = MethodMixin.call(right, "isEmpty");
    if (rightStatement == null) {
      DetailAST zero = OperationMixin.equalZero(right);
      if (zero == null) {
        return;
      }

      rightStatement = FieldMixin.field(zero, "length");
      if (rightStatement == null) {
        return;
      }
    }

    if (!GeneralMixin.same(leftStatement, rightStatement)) {
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
    final DetailAST leftStatement = OperationMixin.notEqualNull(left);
    if (leftStatement == null) {
      return;
    }
    final DetailAST right = left.getNextSibling();
    DetailAST rightStatement = right.getType() != TokenTypes.LNOT ? MethodMixin.call(right, "isNotEmpty")
                                                                  : MethodMixin.call(right.getFirstChild(), "isEmpty");
    if (rightStatement == null) {
      DetailAST notZero = OperationMixin.notEqualZero(right);
      if (notZero == null) {
        return;
      }

      rightStatement = FieldMixin.field(notZero, "length");
      if (rightStatement == null) {
        return;
      }
    }

    if (!GeneralMixin.same(leftStatement, rightStatement)) {
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