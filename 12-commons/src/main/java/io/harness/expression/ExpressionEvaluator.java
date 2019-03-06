package io.harness.expression;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;

import io.harness.data.algorithm.IdentifierName;
import io.harness.exception.CriticalExpressionEvaluationException;
import io.harness.exception.WingsException;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.logging.impl.NoOpLog;
import org.apache.commons.text.StrSubstitutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class ExpressionEvaluator.
 */
public class ExpressionEvaluator {
  public static final Pattern wingsVariablePattern = Pattern.compile("\\$\\{[^{}]*}");
  public static final Pattern variableNamePattern = Pattern.compile("^[-_a-zA-Z][-_\\w]*$");

  private static int EXPANSION_LIMIT = 100 * 1024;
  private static int EXPANSION_MULTIPLIER_LIMIT = 10;
  private static int DEPTH_LIMIT = 10;

  private Map<String, Object> expressionFunctorMap = new HashMap<>();

  private JexlEngine engine = new JexlBuilder().logger(new NoOpLog()).create();

  public void addFunctor(String name, ExpressionFunctor functor) {
    expressionFunctorMap.put(name, functor);
  }

  public Object evaluate(String expression, String name, Object value) {
    Map<String, Object> context = new SingletonMap(name, value);
    return evaluate(expression, context, null);
  }

  public Object evaluate(String expression, Map<String, Object> context) {
    return evaluate(expression, context, null);
  }

  public Object evaluate(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    expression = normalizeExpression(expression, context, defaultObjectPrefix);

    JexlContext jc = prepareContext(context, defaultObjectPrefix);
    return evaluate(expression, jc);
  }

  public Object evaluate(String expression, JexlContext context) {
    if (expression == null) {
      return null;
    }

    JexlExpression jexlExpression = engine.createExpression(expression);
    return jexlExpression.evaluate(context);
  }

  public String normalizeExpression(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    if (expression == null) {
      return null;
    }

    JexlContext jc = prepareContext(context, null);

    final NormalizeVariableResolver variableResolver =
        NormalizeVariableResolver.builder().objectPrefixes(generatePrefixList(defaultObjectPrefix)).context(jc).build();

    StrSubstitutor substitutor = new StrSubstitutor();
    substitutor.setVariableResolver(variableResolver);

    StringBuffer sb = new StringBuffer(expression);
    return substitutor.replace(sb);
  }

  public String substitute(String expression, Map<String, Object> context) {
    return substitute(expression, context, null, null);
  }

  public String substitute(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    return substitute(expression, context, null, defaultObjectPrefix);
  }

  public String substitute(String expression, Map<String, Object> context, VariableResolverTracker tracker) {
    return substitute(expression, context, tracker, null);
  }

  public String substitute(
      String expression, Map<String, Object> context, VariableResolverTracker tracker, String defaultObjectPrefix) {
    if (expression == null) {
      return null;
    }

    JexlContext jc = prepareContext(context, defaultObjectPrefix);

    String prefix = IdentifierName.random();
    String suffix = IdentifierName.random();

    Pattern pattern = Pattern.compile(prefix + "[0-9]+" + suffix);

    final EvaluateVariableResolver variableResolver = EvaluateVariableResolver.builder()
                                                          .expressionEvaluator(this)
                                                          .context(jc)
                                                          .variableResolverTracker(tracker)
                                                          .prefix(prefix)
                                                          .suffix(suffix)
                                                          .build();

    StrSubstitutor substitutor = new StrSubstitutor();
    substitutor.setEnableSubstitutionInVariables(true);
    substitutor.setVariableResolver(variableResolver);

    String result = expression;
    int limit = Math.max(EXPANSION_LIMIT, EXPANSION_MULTIPLIER_LIMIT * expression.length());
    for (int i = 0; i < DEPTH_LIMIT; i++) {
      String original = result;
      result = substitutor.replace(new StringBuffer(original));

      for (;;) {
        final Matcher matcher = pattern.matcher(result);
        if (!matcher.find()) {
          break;
        }

        StringBuffer sb = new StringBuffer();
        do {
          String name = matcher.group(0);
          String value = String.valueOf(jc.get(name));
          matcher.appendReplacement(sb, value.replace("\\", "\\\\").replace("$", "\\$"));
        } while (matcher.find());
        matcher.appendTail(sb);
        result = sb.toString();
      }
      if (result.equals(original)) {
        return result;
      }
      if (result.length() > limit) {
        throw new CriticalExpressionEvaluationException("Exponentially growing interpretation", expression);
      }
    }

    throw new CriticalExpressionEvaluationException(
        "Infinite loop or too deep indirection in property interpretation", expression);
  }

  public static void isValidVariableName(String name) {
    // Verify variable name should not contain any special character
    if (isEmpty(name)) {
      return;
    }
    Matcher matcher = ExpressionEvaluator.variableNamePattern.matcher(name);
    if (!matcher.matches()) {
      throw new WingsException(INVALID_ARGUMENT, USER)
          .addParam("args", "Special characters are not allowed in variable name");
    }
  }

  /**
   * Validates and gets name from expression
   * @param expression
   * @return
   */
  public static String getName(String expression) {
    if (isEmpty(expression)) {
      return expression;
    }
    Matcher matcher = ExpressionEvaluator.wingsVariablePattern.matcher(expression);
    if (matcher.matches()) {
      expression = matcher.group(0).substring(2, matcher.group(0).length() - 1);
    }
    return expression;
  }

  public static boolean matchesVariablePattern(String expression) {
    if (isEmpty(expression)) {
      return false;
    }
    return ExpressionEvaluator.wingsVariablePattern.matcher(expression).matches();
  }

  public static boolean containsVariablePattern(String expression) {
    if (isEmpty(expression)) {
      return false;
    }
    return ExpressionEvaluator.wingsVariablePattern.matcher(expression).find();
  }

  protected JexlContext prepareContext(Map<String, Object> context, String defaultObjectPrefix) {
    final HashMap<String, Object> map = new HashMap<>(context);
    map.putAll(expressionFunctorMap);
    return LateBindingContext.builder()
        .prefixes(generatePrefixList(defaultObjectPrefix))
        .expressionEvaluator(this)
        .map(map)
        .build();
  }

  private List<String> generatePrefixList(String defaultObjectPrefix) {
    List<String> prefixes = new ArrayList<>();
    if (defaultObjectPrefix != null) {
      prefixes.add(defaultObjectPrefix);
    }
    prefixes.add("context");
    return prefixes;
  }
}
