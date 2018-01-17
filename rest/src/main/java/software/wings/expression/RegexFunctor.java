package software.wings.expression;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexFunctor {
  public String extract(String pattern, String source) {
    final Pattern compiled = Pattern.compile(pattern);
    final Matcher matcher = compiled.matcher(source);
    if (!matcher.find()) {
      return "";
    }
    return matcher.group();
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
