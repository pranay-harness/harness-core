package io.harness.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.algorithm.IdentifierName;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.CriticalExpressionEvaluationException;
import io.harness.exception.FunctorException;
import io.harness.exception.InvalidRequestException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.impl.NoOpLog;
import org.apache.commons.text.StrSubstitutor;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Slf4j
public class EngineExpressionEvaluator {
  public static final String EXPR_START = "<+";
  public static final String EXPR_END = ">";
  public static final String EXPR_START_ESC = "<\\+";
  public static final String EXPR_END_ESC = ">";

  private static final Pattern variablePattern = Pattern.compile(EXPR_START_ESC + "[^{}<>]*" + EXPR_END_ESC);
  private static final Pattern secretVariablePattern =
      Pattern.compile(EXPR_START_ESC + "secret(Manager|Delegate)\\.[^{}<>]*" + EXPR_END_ESC);
  private static final Pattern validVariableFieldNamePattern = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
  private static final Pattern aliasNamePattern = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");

  private final JexlEngine engine;
  @Getter private final VariableResolverTracker variableResolverTracker;
  private final Map<String, Object> contextMap;
  @Getter private final Map<String, String> staticAliases;
  private boolean initialized;

  public EngineExpressionEvaluator(VariableResolverTracker variableResolverTracker) {
    this.engine = new JexlBuilder().logger(new NoOpLog()).create();
    this.variableResolverTracker =
        variableResolverTracker == null ? new VariableResolverTracker() : variableResolverTracker;
    this.contextMap = new LateBindingMap();
    this.staticAliases = new HashMap<>();
  }

  /**
   * Add objects/functors to context map and aliases. Context map and aliases can only be updated in this method.
   */
  protected void initialize() {
    // This method will be overridden by sub-classes.
  }

  protected final boolean isInitialized() {
    synchronized (this) {
      return initialized;
    }
  }

  /**
   * Add objects/functors to contextMap. Should be called within the initialize method only.
   *
   * @param name   the name of the functor
   * @param object the object to put against the name
   */
  protected final void addToContext(@NotNull String name, @NotNull Object object) {
    if (isInitialized()) {
      return;
    }
    contextMap.put(name, object);
  }

  /**
   * Add a static alias. Any expression that starts with `aliasName` will be replaced by `replacement`. Should be
   * called within the initialize method only.
   *
   * @param aliasName   the name of the alias
   * @param replacement the string to replace the alias name with
   */
  protected final void addStaticAlias(@NotNull String aliasName, @NotEmpty String replacement) {
    if (isInitialized()) {
      return;
    }
    if (!validAliasName(aliasName)) {
      throw new InvalidRequestException("Invalid alias: " + aliasName);
    }
    if (aliasName.equals(replacement)) {
      throw new InvalidRequestException("Alias and replacement cannot be the same: " + aliasName);
    }
    staticAliases.put(aliasName, replacement);
  }

  /**
   * Return the prefixes to search for when evaluating expressions. The return value should not be null/empty. When
   * overriding this method, consider calling super.fetchPrefixes() and including the prefixes of the superclass in the
   * final list.
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
  @NotEmpty
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
    return ExpressionEvaluatorUtils.updateExpressions(o, new ResolveFunctorImpl(this));
  }

  public String renderExpression(String expression) {
    if (!hasVariables(expression)) {
      return expression;
    }

    return ExpressionEvaluatorUtils.substitute(this, expression, prepareContext());
  }

  public Object evaluateExpression(String expression) {
    // TODO(gpahal): Look at adding support for recursion and nested expressions in this method.

    // NOTE: Don't check for hasExpressions here. There might be normal expressions like '"true" != "false"'
    if (EmptyPredicate.isEmpty(expression)) {
      return null;
    }

    EngineJexlContext ctx = prepareContext();
    StrSubstitutor strSubstitutor = new StrSubstitutor(EngineVariableResolver.builder()
                                                           .expressionEvaluator(this)
                                                           .ctx(ctx)
                                                           .prefix(IdentifierName.random())
                                                           .suffix(IdentifierName.random())
                                                           .build(),
        EXPR_START, EXPR_END, StrSubstitutor.DEFAULT_ESCAPE);
    return evaluateInternal(strSubstitutor.replace(expression), ctx);
  }

  /**
   * Evaluate a variables with the given context after applying static alias substitutions and prefixes. This variant is
   * non-recursive.
   */
  public Pair<Object, Boolean> evaluateVariable(String expression, EngineJexlContext ctx) {
    if (expression == null) {
      return null;
    }

    // Apply all the prefixes and return first one that evaluates successfully.
    List<String> finalExpressions = preProcessExpression(expression);
    for (String finalExpression : finalExpressions) {
      Object object = null;
      try {
        object = evaluateInternal(finalExpression, ctx);
      } catch (JexlException ex) {
        if (ex.getCause() instanceof FunctorException) {
          throw(FunctorException) ex.getCause();
        }
        log.debug(format("Failed to evaluate final expression: %s", finalExpression), ex);
      }

      if (object != null) {
        return Pair.of(object, Boolean.TRUE);
      }
    }

    // No final expression could be resolved.
    return Pair.of(createExpression(expression), Boolean.FALSE);
  }

  /**
   * Return the expression after applying static alias substitutions and prefixes.
   *
   * @param expression the original expression
   * @return the final expression
   */
  private List<String> preProcessExpression(String expression) {
    String normalizedExpression = applyStaticAliases(expression);
    return fetchPrefixes()
        .stream()
        .map(prefix -> EmptyPredicate.isEmpty(prefix) ? normalizedExpression : prefix + "." + normalizedExpression)
        .collect(Collectors.toList());
  }

  /**
   * Return the expression after applying static alias substitutions.
   *
   * @param expression the original expression
   * @return the final expression
   */
  private String applyStaticAliases(String expression) {
    if (EmptyPredicate.isEmpty(expression)) {
      return expression;
    }

    for (int i = 0; i < ExpressionEvaluatorUtils.DEPTH_LIMIT; i++) {
      if (staticAliases.containsKey(expression)) {
        expression = staticAliases.get(expression);
        continue;
      }

      List<String> parts = Arrays.asList(expression.split("\\.", 2));
      if (parts.size() < 2) {
        return expression;
      }

      String firstPart = parts.get(0);
      if (staticAliases.containsKey(firstPart)) {
        parts.set(0, staticAliases.get(firstPart));
        expression = String.join(".", parts);
      } else {
        return expression;
      }
    }

    throw new CriticalExpressionEvaluationException(
        "Infinite loop or too deep indirection in static alias interpretation", expression);
  }

  /**
   * Evaluate an expression with the given context. This variant is non-recursive.
   */
  protected Object evaluateInternal(String expression, EngineJexlContext ctx) {
    if (expression == null) {
      return null;
    }

    JexlExpression jexlExpression = engine.createExpression(expression);
    return jexlExpression.evaluate(ctx);
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
    return new EngineJexlContext(this, clonedContext);
  }

  public static boolean hasVariables(String str) {
    if (EmptyPredicate.isEmpty(str)) {
      return false;
    }

    return variablePattern.matcher(str).find();
  }

  public static List<String> findVariables(String str) {
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

  public static boolean hasSecretVariables(String str) {
    if (EmptyPredicate.isEmpty(str)) {
      return false;
    }

    return secretVariablePattern.matcher(str).find();
  }

  public static List<String> findSecretVariables(String str) {
    if (EmptyPredicate.isEmpty(str)) {
      return Collections.emptyList();
    }

    List<String> matches = new ArrayList<>();
    Matcher matcher = secretVariablePattern.matcher(str);
    while (matcher.find()) {
      matches.add(matcher.group(0));
    }
    return matches;
  }

  public static boolean validVariableFieldName(String name) {
    if (EmptyPredicate.isEmpty(name)) {
      return false;
    }

    return validVariableFieldNamePattern.matcher(name).matches();
  }

  public static boolean validAliasName(String name) {
    if (EmptyPredicate.isEmpty(name)) {
      return false;
    }

    return aliasNamePattern.matcher(name).matches();
  }

  public static String createExpression(String expr) {
    return expr == null ? null : EXPR_START + expr + EXPR_END;
  }

  public static class ResolveFunctorImpl implements ExpressionResolveFunctor {
    private final EngineExpressionEvaluator expressionEvaluator;

    public ResolveFunctorImpl(EngineExpressionEvaluator expressionEvaluator) {
      this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public String renderExpression(String expression) {
      return expressionEvaluator.renderExpression(expression);
    }

    @Override
    public Object evaluateExpression(String expression) {
      return expressionEvaluator.renderExpression(expression);
    }

    @Override
    public boolean hasVariables(String expression) {
      return EngineExpressionEvaluator.hasVariables(expression);
    }
  }
}
