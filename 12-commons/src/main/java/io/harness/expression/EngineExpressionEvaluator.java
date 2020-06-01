package io.harness.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.CriticalExpressionEvaluationException;
import io.harness.exception.FunctorException;
import io.harness.utils.ParameterField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.logging.impl.NoOpLog;
import org.apache.commons.text.StrLookup;
import org.apache.commons.text.StrSubstitutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Slf4j
public class EngineExpressionEvaluator implements ExpressionEvaluatorItfc, ExpressionEvaluatorUtils.ResolveFunctor {
  // TODO(gpahal): Both of these patterns need to be changed later
  private static final Pattern variablePattern = Pattern.compile("\\$\\{[^{}]*}");
  private static final Pattern validVariableNamePattern = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");

  private final JexlEngine engine;
  private final VariableResolverTracker variableResolverTracker;
  private final Map<String, Object> contextMap;
  private boolean initialized;

  public EngineExpressionEvaluator(VariableResolverTracker variableResolverTracker) {
    this.engine = new JexlBuilder().logger(new NoOpLog()).create();
    this.variableResolverTracker =
        variableResolverTracker == null ? new VariableResolverTracker() : variableResolverTracker;
    this.contextMap = new LateBindingMap();
  }

  protected void initialize() {
    addToContext("regex", new RegexFunctor());
    addToContext("json", new JsonFunctor());
    addToContext("xml", new XmlFunctor());
  }

  /**
   * Add objects/functors to contextMap. Should be called within the constructor or initialize only.
   * @param name   the name of the functor
   * @param object the object to put against the name
   */
  protected void addToContext(@NotNull String name, @NotNull Object object) {
    contextMap.put(name, object);
  }

  /**
   * Return the prefixes to search for when evaluating expressions.
   *
   * Example:
   *   If the expression is `a.b.c` and fetchPrefixes returns ["child", "qualified", ""]
   *   We search in the following order:
   *   - child.a.b.c
   *   - qualified.a.b.c
   *   - a.b.c
   *
   * NOTE: Expression without any prefix will only be searched if the prefixes list contains "" or null.
   *
   * Example:
   * `qualified.b.c` and fetchPrefixes returns ["child", "qualified", ""] We search in the following order:
   *   - child.qualified.b.c
   *   - qualified.qualified.b.c
   *   - qualified.b.c [This searches for b.c inside contextMap entry with name 'qualified']
   *
   * @return the prefixes to search with
   */
  @NotNull
  protected List<String> fetchPrefixes() {
    return Collections.singletonList("");
  }

  /**
   * This method renders expressions recursively for all String fields inside the given object. If a field is annotated
   * with @NotExpression, it is skipped.
   *
   * @param o the object to resolve
   * @return the resolved object (this can be the same object or a new one)
   */
  public Object resolve(Object o) {
    return ExpressionEvaluatorUtils.updateExpressions(o, this);
  }

  public String renderExpression(String expression) {
    if (!hasExpressions(expression)) {
      return expression;
    }

    EngineJexlContext ctx = prepareContext();
    return ExpressionEvaluatorUtils.substitute(this, expression, ctx, variableResolverTracker);
  }

  public Object evaluateExpression(String expression) {
    // TODO(gpahal): Add support for variable tracking in this method.
    // NOTE: Don't check for hasExpressions here. There might be normal expressions like '"true" != "false"'
    if (EmptyPredicate.isEmpty(expression)) {
      return null;
    }

    EngineJexlContext ctx = prepareContext();
    return evaluateExpressionInternal(expression, ctx, 0);
  }

  public Optional<Object> evaluateExpressionOptional(String expression) {
    Object value = evaluateExpression(expression);
    if (value instanceof String && hasExpressions((String) value)) {
      return Optional.empty();
    }
    return Optional.of(value);
  }

  private Object evaluateExpressionInternal(String expression, EngineJexlContext ctx, int depth) {
    String normalizedExpression = stripDelimiters(expression);
    if (EmptyPredicate.isEmpty(normalizedExpression)) {
      return normalizedExpression;
    }
    if (depth >= ExpressionEvaluatorUtils.DEPTH_LIMIT) {
      throw new CriticalExpressionEvaluationException("Exponentially growing interpretation", expression);
    }

    Object value;
    try {
      value = evaluate(normalizedExpression, ctx);
    } catch (JexlException ex) {
      if (ex.getCause() instanceof FunctorException) {
        throw(FunctorException) ex.getCause();
      }

      logger.debug(format("Failed to evaluate expression: %s", expression), ex);
      return expression;
    }

    if (value instanceof String && hasExpressions((String) value)) {
      return evaluateExpressionInternal((String) value, ctx, depth + 1);
    }
    return value;
  }

  /**
   * Evaluate an expression with the given context. This variant is non-recursive.
   */
  @Override
  public Object evaluate(String expression, JexlContext ctx) {
    if (expression == null) {
      return null;
    }

    JexlExpression jexlExpression = engine.createExpression(expression);
    Object value = jexlExpression.evaluate(ctx);
    if (value instanceof ParameterField) {
      ParameterField<?> parameterField = (ParameterField<?>) value;
      return parameterField.isExpression() ? parameterField.getExpressionValue() : parameterField.getValue();
    }
    return value;
  }

  private EngineJexlContext prepareContext() {
    synchronized (this) {
      if (!initialized) {
        initialize();
        initialized = true;
      }
    }

    Map<String, Object> clonedContext = new LateBindingMap();
    clonedContext.putAll(contextMap);
    return new EngineJexlContext(this, clonedContext, fetchPrefixes());
  }

  private static String stripDelimiters(String str) {
    if (EmptyPredicate.isEmpty(str)) {
      return str;
    }

    StrSubstitutor strSubstitutor = new StrSubstitutor(new IdentityStrLookup());
    return strSubstitutor.replace(str);
  }

  public static boolean hasExpressions(String str) {
    if (EmptyPredicate.isEmpty(str)) {
      return false;
    }

    return variablePattern.matcher(str).find();
  }

  public static List<String> findExpressions(String str) {
    if (EmptyPredicate.isEmpty(str)) {
      return Collections.emptyList();
    }

    List<String> matches = new ArrayList<>();
    Matcher matcher = variablePattern.matcher(str);
    while (matcher.find()) {
      matches.add(matcher.group(0));
    }
    return matches;
  }

  public static boolean validVariableName(String name) {
    if (EmptyPredicate.isEmpty(name)) {
      return false;
    }

    return validVariableNamePattern.matcher(name).matches();
  }

  private static class IdentityStrLookup extends StrLookup<String> {
    @Override
    public String lookup(String key) {
      return key;
    }
  }
}
