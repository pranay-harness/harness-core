package software.wings.expression;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.Builder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.text.StrLookup;

@Builder
public class NormalizeVariableResolver extends StrLookup {
  private JexlContext context;
  private String objectPrefix;

  public static String expand(String variable, JexlContext context, String objectPrefix) {
    int index = variable.indexOf('.');
    String topObjectName = index < 0 ? variable : variable.substring(0, index);

    if (!context.has(topObjectName)) {
      return objectPrefix + "." + variable;
    }
    return variable;
  }

  @Override
  public String lookup(String variable) {
    if (isNotEmpty(objectPrefix)) {
      return expand(variable, context, objectPrefix);
    }
    return variable;
  }
}
