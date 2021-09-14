/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.checks.mixin;

import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_CLASS;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_INTERFACE;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_NATIVE;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_PRIVATE;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_PROTECTED;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_PUBLIC;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_STATIC;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_SYNCHRONIZED;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_TRANSIENT;
import static com.puppycrawl.tools.checkstyle.api.TokenTypes.LITERAL_VOLATILE;

import com.puppycrawl.tools.checkstyle.api.DetailAST;

public class ModifierMixin {
  public static boolean isModifier(DetailAST ast) {
    switch (ast.getType()) {
      case LITERAL_PRIVATE:
      case LITERAL_PUBLIC:
      case LITERAL_PROTECTED:
      case LITERAL_STATIC:
      case LITERAL_TRANSIENT:
      case LITERAL_NATIVE:
      case LITERAL_SYNCHRONIZED:
      case LITERAL_VOLATILE:
      case LITERAL_CLASS:
      case LITERAL_INTERFACE:
        return true;
      default:
        return false;
    }
  }
}
