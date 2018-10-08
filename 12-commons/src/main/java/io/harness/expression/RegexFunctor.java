package io.harness.expression;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFunctor implements ExpressionFunctor {
  public String extract(String pattern, String source) {
    final Pattern compiled = Pattern.compile(pattern);
    final Matcher matcher = compiled.matcher(source);
    while (matcher.find()) {
      String match = matcher.group();
      if (!match.isEmpty()) {
        return match;
      }
    }
    return "";
  }

  public String replace(String pattern, String replacement, String source) {
    final Pattern compiled = Pattern.compile(pattern);
    final Matcher matcher = compiled.matcher(source);
    return matcher.replaceAll(replacement);
  }

  public boolean match(String pattern, String source) {
    final Pattern compiled = Pattern.compile(pattern);
    final Matcher matcher = compiled.matcher(source);
    return matcher.find();
  }
}
