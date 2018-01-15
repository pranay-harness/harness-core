package io.harness.checks;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.FullIdent;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

import java.util.HashMap;
import java.util.Map;

public class ExclusiveStaticImportCheck extends AbstractCheck {
  private static final String MSG_KEY = "code.dependencies.hell.exclusive.static.import";

  // TODO: take this as arguments
  private Map<String, String> staticImports = new HashMap();

  public void setStaticImports(String... imports) {
    for (String staticImport : imports) {
      String method = staticImport.substring(staticImport.lastIndexOf('.') + 1);
      String pkg = staticImport.substring(0, staticImport.length() - method.length() - 1);

      staticImports.put(method, pkg);
    }
  }

  @Override
  public int[] getDefaultTokens() {
    return new int[] {
        TokenTypes.STATIC_IMPORT,
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

  @Override
  public void visitToken(DetailAST ast) {
    final DetailAST staticLiteral = ast.getFirstChild();
    if (staticLiteral.getType() != TokenTypes.LITERAL_STATIC) {
      return;
    }

    final DetailAST dot = staticLiteral.getNextSibling();
    if (dot.getType() != TokenTypes.DOT) {
      return;
    }

    final DetailAST pkg = dot.getFirstChild();
    if (pkg.getType() != TokenTypes.DOT) {
      return;
    }

    final DetailAST method = pkg.getNextSibling();
    if (!staticImports.containsKey(method.getText())) {
      return;
    }

    String expectPackage = staticImports.get(method.getText());
    if (expectPackage.equals(FullIdent.createFullIdent(pkg).getText())) {
      return;
    }

    log(ast, MSG_KEY, method.getText(), expectPackage);
  }
}