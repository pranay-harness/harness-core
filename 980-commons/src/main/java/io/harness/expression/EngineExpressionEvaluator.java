package io.harness.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.algorithm.IdentifierName;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.CriticalExpressionEvaluationException;
import io.harness.exception.FunctorException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnresolvedExpressionsException;
import io.harness.expression.functors.DateTimeFunctor;
import io.harness.text.StringReplacer;
import io.harness.text.resolver.ExpressionResolver;

import com.google.common.collect.ImmutableList;
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
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlException;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.impl.NoOpLog;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Slf4j
public class EngineExpressionEvaluator {
  public static final String EXPR_START = "<+";
  public static final String EXPR_END = ">";
  public static final String EXPR_START_ESC = "<\\+";
  public static final String EXPR_END_ESC = ">";
  public static final String HARNESS_INTERNAL_VARIABLE_PREFIX = "__HVAR_";

  private static final Pattern VARIABLE_PATTERN = Pattern.compile(EXPR_START_ESC + "[^{}<>]*" + EXPR_END_ESC);
  private static final Pattern SECRET_VARIABLE_PATTERN =
      Pattern.compile(EXPR_START_ESC + "secret(Manager|Delegate)\\.[^{}<>]*" + EXPR_END_ESC);
  private static final Pattern VALID_VARIABLE_FIELD_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
  private static final Pattern ALIAS_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");

  private static final int MAX_DEPTH = 15;

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
    addToContext("regex", new RegexFunctor());
    addToContext("json", new JsonFunctor());
    addToContext("xml", new XmlFunctor());
    addToContext("datetime", new DateTimeFunctor());
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
    return ImmutableList.of("datetime", "");
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

  public PartialEvaluateResult partialResolve(Object o) {
    Map<String, Object> partialCtx = new HashMap<>();
    Object res = ExpressionEvaluatorUtils.updateExpressions(o, new PartialResolveFunctorImpl(this, partialCtx));
    return PartialEvaluateResult.createCompleteResult(res, partialCtx);
  }

  public String renderExpression(String expression) {
    return renderExpression(expression, null);
  }

  public String renderExpression(String expression, Map<String, Object> ctx) {
    if (!hasVariables(expression)) {
      return expression;
    }
    return renderExpressionInternal(expression, prepareContext(ctx), MAX_DEPTH);
  }

  public String renderExpressionInternal(@NotNull String expression, @NotNull EngineJexlContext ctx, int depth) {
    checkDepth(depth, expression);
    return runStringReplacer(expression, new RenderExpressionResolver(this, ctx, depth));
  }

  public Object evaluateExpression(String expression) {
    return evaluateExpression(expression, null);
  }

  public Object evaluateExpression(String expression, Map<String, Object> ctx) {
    // NOTE: Don't check for hasExpressions here. There might be normal expressions like '"true" != "false"'
    if (expression == null || EmptyPredicate.isEmpty(expression.trim())) {
      return null;
    }
    return evaluateExpressionInternal(expression, prepareContext(ctx), MAX_DEPTH);
  }

  private Object evaluateExpressionInternal(@NotNull String expression, @NotNull EngineJexlContext ctx, int depth) {
    checkDepth(depth, expression);
    String finalExpression = runStringReplacer(expression, new EvaluateExpressionResolver(this, ctx, depth));
    if (hasVariables(finalExpression)) {
      if (isNotSingleExpression(finalExpression)) {
        finalExpression = createExpression(finalExpression);
      }
      throw new UnresolvedExpressionsException(finalExpression, findVariables(finalExpression));
    }
    return evaluateInternal(finalExpression, ctx);
  }

  public PartialEvaluateResult partialRenderExpression(String expression) {
    return partialRenderExpression(expression, null);
  }

  public PartialEvaluateResult partialRenderExpression(String expression, Map<String, Object> ctx) {
    if (!hasVariables(expression)) {
      return PartialEvaluateResult.createCompleteResult(expression);
    }
    return partialRenderExpressionInternal(expression, prepareContext(ctx), new HashMap<>(), MAX_DEPTH);
  }

  private PartialEvaluateResult partialRenderExpressionInternal(
      @NotNull String expression, @NotNull EngineJexlContext ctx, @NotNull Map<String, Object> partialCtx, int depth) {
    checkDepth(depth, expression);
    PartialEvaluateExpressionResolver resolver = new PartialEvaluateExpressionResolver(this, ctx, partialCtx, depth);
    String finalExpression = runStringReplacer(expression, resolver);
    ctx.addToContext(partialCtx);
    List<String> variables = findVariables(finalExpression);
    if (EmptyPredicate.isEmpty(variables)) {
      return PartialEvaluateResult.createCompleteResult(evaluateInternal(expression, ctx));
    }

    boolean hasNonInternalVariables =
        variables.stream().anyMatch(v -> !v.startsWith(EXPR_START + HARNESS_INTERNAL_VARIABLE_PREFIX));
    if (hasNonInternalVariables) {
      finalExpression =
          new StringReplacer(new HarnessInternalRenderExpressionResolver(partialCtx), EXPR_START, EXPR_END)
              .replace(finalExpression);
      return PartialEvaluateResult.createPartialResult(finalExpression, partialCtx);
    } else {
      return PartialEvaluateResult.createCompleteResult(renderExpressionInternal(expression, ctx, depth));
    }
  }

  public PartialEvaluateResult partialEvaluateExpression(String expression) {
    return partialEvaluateExpression(expression, null);
  }

  public PartialEvaluateResult partialEvaluateExpression(String expression, Map<String, Object> ctx) {
    // NOTE: Don't check for hasExpressions here. There might be normal expressions like '"true" != "false"'
    if (EmptyPredicate.isEmpty(expression)) {
      return null;
    }
    return partialEvaluateExpressionInternal(expression, prepareContext(ctx), new HashMap<>(), MAX_DEPTH);
  }

  private PartialEvaluateResult partialEvaluateExpressionInternal(
      @NotNull String expression, @NotNull EngineJexlContext ctx, @NotNull Map<String, Object> partialCtx, int depth) {
    checkDepth(depth, expression);
    PartialEvaluateExpressionResolver resolver = new PartialEvaluateExpressionResolver(this, ctx, partialCtx, depth);
    String finalExpression = runStringReplacer(expression, resolver);
    ctx.addToContext(partialCtx);
    List<String> variables = findVariables(finalExpression);
    if (EmptyPredicate.isEmpty(variables)) {
      return PartialEvaluateResult.createCompleteResult(evaluateInternal(expression, ctx));
    }

    boolean hasNonInternalVariables =
        variables.stream().anyMatch(v -> !v.startsWith(EXPR_START + HARNESS_INTERNAL_VARIABLE_PREFIX));
    if (hasNonInternalVariables) {
      if (isNotSingleExpression(finalExpression)) {
        finalExpression = createExpression(finalExpression);
      }
      return PartialEvaluateResult.createPartialResult(finalExpression, partialCtx);
    } else {
      return PartialEvaluateResult.createCompleteResult(evaluateExpressionInternal(expression, ctx, depth));
    }
  }

  public PartialEvaluateResult partialEvaluateExpressionBlock(@NotNull String expressionBlock,
      @NotNull EngineJexlContext ctx, @NotNull Map<String, Object> partialCtx, int depth) {
    if (EmptyPredicate.isEmpty(expressionBlock)) {
      return PartialEvaluateResult.createCompleteResult(expressionBlock);
    }

    // Check for cases like <+<+abc>.contains(<+def>)>.
    if (hasVariables(expressionBlock)) {
      return partialEvaluateExpressionInternal(expressionBlock, ctx, partialCtx, depth - 1);
    }

    // If object is another expression, evaluate it recursively.
    Object object = evaluatePrefixCombinations(expressionBlock, ctx, depth);
    if (object instanceof String && hasVariables((String) object)) {
      if (createExpression(expressionBlock).equals(object)) {
        // If returned expression is exactly the same, throw exception.
        throw new CriticalExpressionEvaluationException(
            "Infinite loop in variable interpretation", createExpression(expressionBlock));
      } else {
        PartialEvaluateResult result = partialEvaluateExpressionInternal((String) object, ctx, partialCtx, depth - 1);
        if (result.isPartial()) {
          return result;
        } else {
          object = result.getValue();
        }
      }
    }

    if (object == null) {
      // No final expression could be resolved.
      return PartialEvaluateResult.createPartialResult(createExpression(expressionBlock), null);
    }

    String name = HARNESS_INTERNAL_VARIABLE_PREFIX + RandomStringUtils.random(12, true, false);
    partialCtx.put(name, object);
    return PartialEvaluateResult.createPartialResult(createExpression(name), partialCtx);
  }

  /**
   * Evaluate an expression block (anything inside <+...>) with the given context after applying static alias
   * substitutions and prefixes. This variant is non-recursive.
   */
  public Object evaluateExpressionBlock(@NotNull String expressionBlock, @NotNull EngineJexlContext ctx, int depth) {
    checkDepth(depth, expressionBlock);
    if (EmptyPredicate.isEmpty(expressionBlock)) {
      return expressionBlock;
    }

    // Check for cases like <+<+abc>.contains(<+def>)>.
    if (hasVariables(expressionBlock)) {
      return evaluateExpressionInternal(expressionBlock, ctx, depth - 1);
    }

    // If object is another expression, evaluate it recursively.
    Object object = evaluatePrefixCombinations(expressionBlock, ctx, depth);
    if (object instanceof String && hasVariables((String) object)) {
      if (createExpression(expressionBlock).equals(object)) {
        // If returned expression is exactly the same, throw exception.
        throw new CriticalExpressionEvaluationException(
            "Infinite loop in variable interpretation", createExpression(expressionBlock));
      } else {
        observed(expressionBlock, object);
        return evaluateExpressionInternal((String) object, ctx, depth - 1);
      }
    }

    observed(expressionBlock, object);
    return object;
  }

  private Object evaluatePrefixCombinations(
      @NotNull String expressionBlock, @NotNull EngineJexlContext ctx, int depth) {
    // Apply all the prefixes and return first one that evaluates successfully.
    List<String> finalExpressions = preProcessExpression(expressionBlock);
    Object object = null;
    for (String finalExpression : finalExpressions) {
      try {
        if (hasVariables(finalExpression)) {
          object = evaluateExpressionInternal(finalExpression, ctx, depth - 1);
        } else {
          object = evaluateInternal(finalExpression, ctx);
        }
      } catch (JexlException ex) {
        if (ex.getCause() instanceof FunctorException) {
          throw(FunctorException) ex.getCause();
        }
        log.debug(format("Failed to evaluate final expression: %s", finalExpression), ex);
      }

      if (object != null) {
        return object;
      }
    }
    return null;
  }

  private void observed(String variable, Object value) {
    if (variableResolverTracker != null) {
      variableResolverTracker.observed(variable, value);
    }
  }

  /**
   * Return the expression after applying static alias substitutions and prefixes.
   *
   * @param expression the original expression
   * @return the final expression
   */
  private List<String> preProcessExpression(@NotNull String expression) {
    String normalizedExpression = applyStaticAliases(expression);
    if (hasVariables(normalizedExpression)) {
      return Collections.singletonList(normalizedExpression);
    }
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
  private String applyStaticAliases(@NotNull String expression) {
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
   * Evaluate an expression with the given context. This variant is non-recursive and doesn't support harness
   * expressions - variables delimited by <+...>.
   */
  protected Object evaluateInternal(@NotNull String expression, @NotNull EngineJexlContext ctx) {
    JexlExpression jexlExpression = engine.createExpression(expression);
    return jexlExpression.evaluate(ctx);
  }

  private EngineJexlContext prepareContext(Map<String, Object> ctx) {
    synchronized (this) {
      if (!initialized) {
        initialize();
        initialized = true;
      }
    }

    Map<String, Object> clonedContext = new LateBindingMap();
    clonedContext.putAll(contextMap);
    if (EmptyPredicate.isNotEmpty(ctx)) {
      clonedContext.putAll(ctx);
    }
    return new EngineJexlContext(this, clonedContext);
  }

  private static void checkDepth(int depth, String expression) {
    if (depth <= 0) {
      throw new CriticalExpressionEvaluationException(
          "Infinite loop or too deep indirection in expression interpretation", expression);
    }
  }

  private static String runStringReplacer(@NotNull String expression, @NotNull ExpressionResolver resolver) {
    StringReplacer replacer = new StringReplacer(resolver, EXPR_START, EXPR_END);
    return replacer.replace(expression);
  }

  public static boolean matchesVariablePattern(String str) {
    if (EmptyPredicate.isEmpty(str)) {
      return false;
    }
    return VARIABLE_PATTERN.matcher(str).matches();
  }

  public static boolean isNotSingleExpression(String str) {
    if (EmptyPredicate.isEmpty(str)) {
      return true;
    }
    String finalExpression = runStringReplacer(str, new EmptyExpressionResolver());
    return !EmptyPredicate.isEmpty(finalExpression);
  }

  public static boolean hasVariables(String str) {
    return hasPattern(str, VARIABLE_PATTERN);
  }

  public static List<String> findVariables(String str) {
    return findPatterns(str, VARIABLE_PATTERN);
  }

  public static boolean hasSecretVariables(String str) {
    return hasPattern(str, SECRET_VARIABLE_PATTERN);
  }

  public static List<String> findSecretVariables(String str) {
    return findPatterns(str, SECRET_VARIABLE_PATTERN);
  }

  public static boolean validVariableFieldName(String name) {
    if (EmptyPredicate.isEmpty(name)) {
      return false;
    }
    return VALID_VARIABLE_FIELD_NAME_PATTERN.matcher(name).matches();
  }

  public static boolean validAliasName(String name) {
    if (EmptyPredicate.isEmpty(name)) {
      return false;
    }
    return ALIAS_NAME_PATTERN.matcher(name).matches();
  }

  public static String createExpression(String expr) {
    return expr == null ? null : EXPR_START + expr + EXPR_END;
  }

  public static boolean hasPattern(String str, Pattern pattern) {
    if (EmptyPredicate.isEmpty(str)) {
      return false;
    }
    return pattern.matcher(str).find();
  }

  public static List<String> findPatterns(String str, Pattern pattern) {
    if (EmptyPredicate.isEmpty(str)) {
      return Collections.emptyList();
    }

    List<String> matches = new ArrayList<>();
    Matcher matcher = pattern.matcher(str);
    while (matcher.find()) {
      matches.add(matcher.group(0));
    }
    return matches;
  }

  private static class RenderExpressionResolver implements ExpressionResolver {
    private final EngineExpressionEvaluator engineExpressionEvaluator;
    private final EngineJexlContext ctx;
    private final int depth;

    RenderExpressionResolver(EngineExpressionEvaluator engineExpressionEvaluator, EngineJexlContext ctx, int depth) {
      this.engineExpressionEvaluator = engineExpressionEvaluator;
      this.ctx = ctx;
      this.depth = depth;
    }

    @Override
    public String resolve(String expression) {
      try {
        Object value = engineExpressionEvaluator.evaluateExpressionBlock(expression, ctx, depth);
        if (value == null) {
          return createExpression(expression);
        }

        // TODO(gpahal): On order to make render strict - we should throw an error if value is a string and contains
        // expressions
        return String.valueOf(value);
      } catch (UnresolvedExpressionsException ex) {
        return ex.fetchFinalExpression();
      }
    }
  }

  private static class EvaluateExpressionResolver implements ExpressionResolver {
    private final EngineExpressionEvaluator engineExpressionEvaluator;
    private final EngineJexlContext ctx;
    private final int depth;
    private final String prefix;
    private final String suffix;
    private int varIndex;

    EvaluateExpressionResolver(EngineExpressionEvaluator engineExpressionEvaluator, EngineJexlContext ctx, int depth) {
      this.engineExpressionEvaluator = engineExpressionEvaluator;
      this.ctx = ctx;
      this.depth = depth;
      this.prefix = IdentifierName.random();
      this.suffix = IdentifierName.random();
    }

    @Override
    public String resolve(String expression) {
      Object value = engineExpressionEvaluator.evaluateExpressionBlock(expression, ctx, depth - 1);
      if (value == null) {
        return createExpression(expression);
      }

      String name = prefix + ++varIndex + suffix;
      ctx.set(name, value);
      return name;
    }
  }

  private static class PartialEvaluateExpressionResolver implements ExpressionResolver {
    private final EngineExpressionEvaluator engineExpressionEvaluator;
    private final EngineJexlContext ctx;
    private final Map<String, Object> partialCtx;
    private final int depth;

    PartialEvaluateExpressionResolver(EngineExpressionEvaluator engineExpressionEvaluator, EngineJexlContext ctx,
        Map<String, Object> partialCtx, int depth) {
      this.engineExpressionEvaluator = engineExpressionEvaluator;
      this.ctx = ctx;
      this.partialCtx = partialCtx;
      this.depth = depth;
    }

    @Override
    public String resolve(String expression) {
      PartialEvaluateResult result =
          engineExpressionEvaluator.partialEvaluateExpressionBlock(expression, ctx, partialCtx, depth - 1);
      if (result.isPartial()) {
        return result.getExpressionValue();
      }

      String name = HARNESS_INTERNAL_VARIABLE_PREFIX + RandomStringUtils.random(12, true, false);
      partialCtx.put(name, result.getValue());
      return createExpression(name);
    }
  }

  private static class HarnessInternalRenderExpressionResolver implements ExpressionResolver {
    private final Map<String, Object> partialCtx;

    HarnessInternalRenderExpressionResolver(Map<String, Object> partialCtx) {
      this.partialCtx = partialCtx;
    }

    @Override
    public String resolve(String expression) {
      if (partialCtx.containsKey(expression)) {
        return String.valueOf(partialCtx.get(expression));
      } else {
        return createExpression(expression);
      }
    }
  }

  private static class EmptyExpressionResolver implements ExpressionResolver {
    @Override
    public String resolve(String expression) {
      return "";
    }
  }

  public static class ResolveFunctorImpl implements ExpressionResolveFunctor {
    private final EngineExpressionEvaluator expressionEvaluator;

    public ResolveFunctorImpl(EngineExpressionEvaluator expressionEvaluator) {
      this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public String processString(String expression) {
      return expressionEvaluator.renderExpression(expression);
    }
  }

  public static class PartialResolveFunctorImpl implements ExpressionResolveFunctor {
    private final EngineExpressionEvaluator expressionEvaluator;
    private final Map<String, Object> partialCtx;

    public PartialResolveFunctorImpl(EngineExpressionEvaluator expressionEvaluator, Map<String, Object> partialCtx) {
      this.expressionEvaluator = expressionEvaluator;
      this.partialCtx = partialCtx;
    }

    @Override
    public String processString(String expression) {
      PartialEvaluateResult result = expressionEvaluator.partialRenderExpression(expression);
      if (EmptyPredicate.isNotEmpty(result.getPartialCtx())) {
        partialCtx.putAll(result.getPartialCtx());
      }
      if (result.isPartial()) {
        return result.getExpressionValue();
      }
      return (String) result.getValue();
    }
  }

  @Value
  public static class PartialEvaluateResult {
    boolean partial;
    String expressionValue;
    Object value;
    Map<String, Object> partialCtx;

    public static PartialEvaluateResult createPartialResult(String expression, Map<String, Object> partialCtx) {
      return new PartialEvaluateResult(true, expression, null, partialCtx);
    }

    public static PartialEvaluateResult createCompleteResult(Object value) {
      return createCompleteResult(value, null);
    }

    public static PartialEvaluateResult createCompleteResult(Object value, Map<String, Object> partialCtx) {
      return new PartialEvaluateResult(false, null, value, partialCtx);
    }
  }
}
