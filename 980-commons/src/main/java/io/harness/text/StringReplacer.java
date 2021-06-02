package io.harness.text;

import io.harness.text.resolver.ExpressionResolver;

public class StringReplacer {
  private final ExpressionResolver expressionResolver;
  private final char[] expressionPrefix;
  private final char[] expressionSuffix;

  public StringReplacer(ExpressionResolver expressionResolver, String expressionPrefix, String expressionSuffix) {
    this.expressionResolver = expressionResolver;
    this.expressionPrefix = expressionPrefix.toCharArray();
    this.expressionSuffix = expressionSuffix.toCharArray();
  }

  public String replace(String source) {
    if (source == null) {
      return null;
    }

    StringBuffer buf = new StringBuffer(source);
    return substitute(buf) ? buf.toString() : source;
  }

  private boolean substitute(StringBuffer buf) {
    boolean altered = false;
    int bufEnd = buf.length();
    int pos = 0;
    while (pos < bufEnd) {
      boolean hasPrefix = isMatch(expressionPrefix, buf, pos, bufEnd);
      if (!hasPrefix) {
        pos++;
        continue;
      }

      // Found expression prefix
      int expressionStartPos = pos;
      pos += expressionPrefix.length;
      boolean foundSuffix;
      int nestedExpressionCount = 0;
      while (pos < bufEnd) {
        if (isMatch(expressionPrefix, buf, pos, bufEnd)) {
          // Found a nested expression prefix
          nestedExpressionCount++;
          pos += expressionPrefix.length;
          continue;
        }

        foundSuffix = isMatch(expressionSuffix, buf, pos, bufEnd);
        if (!foundSuffix) {
          pos++;
          continue;
        }

        // Found expression suffix
        pos += expressionSuffix.length;
        if (nestedExpressionCount > 0) {
          // Found a nested expression suffix
          nestedExpressionCount--;
          continue;
        }

        // Get whole expression
        int expressionEndPos = pos;
        String expressionWithDelimiters = buf.substring(expressionStartPos, expressionEndPos);
        String expression = expressionWithDelimiters.substring(
            expressionPrefix.length, expressionWithDelimiters.length() - expressionSuffix.length);

        // Resolve the expression
        String expressionValue = expressionResolver.resolve(expression);
        if (expressionValue == null) {
          throw new RuntimeException(String.format("Cannot resolve expression: %s", expression));
        }

        buf.replace(expressionStartPos, expressionEndPos, expressionValue);
        pos += expressionValue.length() - expressionWithDelimiters.length();
        bufEnd = buf.length();
        altered = altered || !expressionWithDelimiters.equals(expressionValue);
        break;
      }
    }
    return altered;
  }

  private static boolean isMatch(char[] str, StringBuffer buf, int bufStart, int bufEnd) {
    if (bufStart + str.length > bufEnd) {
      return false;
    }
    for (int i = 0, j = bufStart; i < str.length; i++, j++) {
      if (str[i] != buf.charAt(j)) {
        return false;
      }
    }
    return true;
  }
}
