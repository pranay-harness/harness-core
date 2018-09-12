package software.wings.expression;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Singleton;

import io.harness.data.algorithm.IdentifierName;
import io.harness.exception.WingsException;
import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.logging.impl.NoOpLog;
import org.apache.commons.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class ExpressionEvaluator.
 *
 * @author Rishi
 */
@Singleton
public class ExpressionEvaluator {
  private static final Logger logger = LoggerFactory.getLogger(ExpressionEvaluator.class);

  private JexlEngine engine = new JexlBuilder().logger(new NoOpLog()).create();
  private RegexFunctor regexFunctor = new RegexFunctor();
  private AwsFunctor awsFunctor = new AwsFunctor();

  private static int EXPANSION_LIMIT = 100 * 1024;
  private static int EXPANSION_MULTIPLIER_LIMIT = 10;
  private static int DEPTH_LIMIT = 10;

  /**
   * The constant wingsVariablePattern.
   */
  public static final Pattern wingsVariablePattern = Pattern.compile("\\$\\{[^{}]*}");
  public static final Pattern variableNamePattern = Pattern.compile("^[-_a-zA-Z][-_\\w]*$");

  public Object evaluate(String expression, String name, Object value) {
    Map<String, Object> context = new SingletonMap(name, value);
    return evaluate(expression, context, null);
  }

  public Object evaluate(String expression, Map<String, Object> context) {
    return evaluate(expression, context, null);
  }

  public Object evaluate(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    expression = normalizeExpression(expression, context, defaultObjectPrefix);

    JexlContext jc = prepareContext(context);
    return evaluate(expression, jc);
  }

  public Object evaluate(String expression, JexlContext context) {
    logger.debug("evaluate request - expression: {}, context: {}", expression, context);
    if (expression == null) {
      return null;
    }

    JexlExpression jexlExpression = engine.createExpression(expression);
    Object retValue = jexlExpression.evaluate(context);
    logger.debug("evaluate request - return value: {}", retValue);
    return retValue;
  }

  public String normalizeExpression(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    if (expression == null) {
      return null;
    }

    JexlContext jc = prepareContext(context);

    final NormalizeVariableResolver variableResolver =
        NormalizeVariableResolver.builder().objectPrefix(defaultObjectPrefix).context(jc).build();

    StrSubstitutor substitutor = new StrSubstitutor();
    substitutor.setVariableResolver(variableResolver);

    StringBuffer sb = new StringBuffer(expression);
    return substitutor.replace(sb);
  }

  public String substitute(String expression, Map<String, Object> context) {
    return substitute(expression, context, null);
  }

  public String substitute(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    if (expression == null) {
      return null;
    }

    JexlContext jc = prepareContext(context);

    String prefix = IdentifierName.random();
    String suffix = IdentifierName.random();

    Pattern pattern = Pattern.compile(prefix + "[0-9]+" + suffix);

    final EvaluateVariableResolver variableResolver = EvaluateVariableResolver.builder()
                                                          .expressionEvaluator(this)
                                                          .objectPrefix(defaultObjectPrefix)
                                                          .context(jc)
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
          matcher.appendReplacement(sb, value.replace("$", "\\$"));
        } while (matcher.find());
        matcher.appendTail(sb);
        result = sb.toString();
      }
      if (result.equals(original)) {
        return result;
      }
      if (result.length() > limit) {
        throw new WingsException(INVALID_ARGUMENT)
            .addParam("args", "Interpretation grows exponentially for: " + expression);
      }
    }

    throw new WingsException(INVALID_ARGUMENT)
        .addParam("args", "Infinite loop or too deep indirection in property interpretation for: " + expression);
  }

  public static void isValidVariableName(String name) {
    // Verify Service variable name should not contain any special character
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
    Matcher matcher = ExpressionEvaluator.wingsVariablePattern.matcher(expression);
    if (matcher.matches()) {
      expression = matcher.group(0).substring(2, matcher.group(0).length() - 1);
    }
    return expression;
  }

  private JexlContext prepareContext(Map<String, Object> context) {
    JexlContext jc = new MapContext();
    if (context != null) {
      for (Entry<String, Object> entry : context.entrySet()) {
        jc.set(entry.getKey(), entry.getValue());
      }
    }

    jc.set("regex", regexFunctor);
    jc.set("aws", awsFunctor);
    return jc;
  }
}
