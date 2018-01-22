package io.harness.checks.mixin;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

public class OperationMixin {
  public interface AstPredicate { boolean check(DetailAST ast); }

  public static DetailAST transitive(DetailAST ast, int operation, AstPredicate object, AstPredicate something) {
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

  public static DetailAST equalNull(DetailAST ast) {
    return transitive(ast, TokenTypes.EQUAL, child -> true, constant -> constant.getType() == TokenTypes.LITERAL_NULL);
  }

  public static DetailAST identifierEqualNull(DetailAST ast) {
    return transitive(ast, TokenTypes.EQUAL,
        identifier
        -> identifier.getType() == TokenTypes.IDENT,
        constant -> constant.getType() == TokenTypes.LITERAL_NULL);
  }

  public static DetailAST notEqualNull(DetailAST ast) {
    return transitive(
        ast, TokenTypes.NOT_EQUAL, child -> true, constant -> constant.getType() == TokenTypes.LITERAL_NULL);
  }

  public static DetailAST identifierNotEqualNull(DetailAST ast) {
    return transitive(ast, TokenTypes.NOT_EQUAL,
        identifier
        -> identifier.getType() == TokenTypes.IDENT,
        constant -> constant.getType() == TokenTypes.LITERAL_NULL);
  }

  public static DetailAST equalZero(DetailAST ast) {
    return transitive(ast, TokenTypes.EQUAL,
        child -> true, constant -> constant.getType() == TokenTypes.NUM_INT && constant.getText().equals("0"));
  }

  public static DetailAST notEqualZero(DetailAST ast) {
    return transitive(ast, TokenTypes.NOT_EQUAL,
        child -> true, constant -> constant.getType() == TokenTypes.NUM_INT && constant.getText().equals("0"));
  }
}
