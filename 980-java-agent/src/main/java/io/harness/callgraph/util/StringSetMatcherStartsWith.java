package io.harness.callgraph.util;

import java.util.Set;
import net.bytebuddy.build.HashCodeAndEqualsPlugin;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * An element matcher which checks if a string is prefix in a set of strings.
 */
@HashCodeAndEqualsPlugin.Enhance
public class StringSetMatcherStartsWith extends ElementMatcher.Junction.AbstractBase<String> {
  /**
   * The values to check against.
   */
  private final Set<String> values;

  /**
   * Creates a new string set matcher.
   *
   * @param values The values to check against.
   */
  public StringSetMatcherStartsWith(Set<String> values) {
    this.values = values;
  }

  @Override
  public boolean matches(String target) {
    for (String value : values) {
      if (target.startsWith(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder().append("in(");
    boolean first = true;
    for (String value : values) {
      if (first) {
        first = false;
      } else {
        stringBuilder.append(", ");
      }
      stringBuilder.append(value);
    }
    return stringBuilder.append(")").toString();
  }
}
