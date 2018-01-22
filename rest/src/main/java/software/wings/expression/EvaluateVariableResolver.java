package software.wings.expression;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import lombok.Builder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.text.StrBuilder;
import org.apache.commons.text.StrLookup;

import java.util.HashSet;
import java.util.Set;

@Builder
public class EvaluateVariableResolver extends StrLookup {
  private JexlContext context;
  private ExpressionEvaluator expressionEvaluator;
  private String objectPrefix;
  private int varIndex;
  private Set<String> previous;
  private Set<String> visited;
  private String prefix;
  private String suffix;

  public void startSession() {
    if (visited == null) {
      visited = new HashSet<>();
    }
    if (previous == null) {
      previous = new HashSet<>();
    } else {
      previous.addAll(visited);
    }
    visited.clear();
  }

  @Override
  public String lookup(String variable) {
    if (previous.contains(variable)) {
      StrBuilder buf = new StrBuilder(128);
      buf.append("Infinite loop in property interpolation of ");
      buf.append(variable);
      throw new IllegalStateException(buf.toString());
    }

    Object value = null;
    try {
      if (isNotEmpty(objectPrefix)) {
        variable = NormalizeVariableResolver.expand(variable, context, objectPrefix);
      }
      value = expressionEvaluator.evaluate(variable, context, objectPrefix);
      visited.add(variable);
    } catch (JexlException exception) {
      value = "${" + variable + "}";
    }

    String name = prefix + ++varIndex + suffix;
    context.set(name, value);
    return name;
  }
}
