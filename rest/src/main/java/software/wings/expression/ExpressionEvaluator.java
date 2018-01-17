package software.wings.expression;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;

import com.google.inject.Singleton;

import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.commons.logging.impl.NoOpLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.exception.WingsException;

import java.util.Map;
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

  /**
   * The constant wingsVariablePattern.
   */
  public static final Pattern wingsVariablePattern = Pattern.compile("\\$\\{[^{}]*}");
  public static final Pattern variableNamePattern = Pattern.compile("^[_a-zA-Z][_\\w]*$");

  public Object evaluate(String expression, String name, Object value) {
    Map<String, Object> context = new SingletonMap(name, value);
    return evaluate(expression, context, null);
  }

  public Object evaluate(String expression, Map<String, Object> context) {
    return evaluate(expression, context, null);
  }

  public Object evaluate(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    expression = normalizeExpression(expression, context, defaultObjectPrefix);

    JexlContext jc = new MapContext();
    if (context != null) {
      for (String key : context.keySet()) {
        jc.set(key, context.get(key));
      }
    }

    return evaluate(expression, jc, defaultObjectPrefix);
  }

  private Object evaluate(String expression, JexlContext context, String defaultObjectPrefix) {
    logger.debug("evaluate request - expression: {}, context: {}", expression, context);
    if (expression == null) {
      return expression;
    }

    context.set("re", regexFunctor);

    JexlExpression jexlExpression = engine.createExpression(expression);
    Object retValue = jexlExpression.evaluate(context);
    logger.debug("evaluate request - return value: {}", retValue);
    return retValue;
  }

  public String normalizeExpression(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    if (expression == null) {
      return expression;
    }

    final NormalizeVariableResolver variableResolver =
        NormalizeVariableResolver.builder().objectPrefix(defaultObjectPrefix).context(context).build();

    StrSubstitutor substitutor = new StrSubstitutor();
    substitutor.setVariableResolver(variableResolver);

    StringBuffer sb = new StringBuffer(expression);
    return substitutor.replace(sb);
  }

  public String substitute(String expression, Map<String, Object> context) {
    return substitute(expression, context, null);
  }

  public String substitute(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    final EvaluateVariableResolver variableResolver = EvaluateVariableResolver.builder()
                                                          .expressionEvaluator(this)
                                                          .objectPrefix(defaultObjectPrefix)
                                                          .context(context)
                                                          .build();

    StrSubstitutor substitutor = new StrSubstitutor();
    substitutor.setEnableSubstitutionInVariables(true);
    substitutor.setVariableResolver(variableResolver);

    StringBuffer sb = new StringBuffer(expression);
    return substitutor.replace(sb);
  }

  public static void isValidVariableName(String name) {
    // Verify Service variable name should not contain any special character
    if (isEmpty(name)) {
      return;
    }
    Matcher matcher = ExpressionEvaluator.variableNamePattern.matcher(name);
    if (!matcher.matches()) {
      throw new WingsException(INVALID_ARGUMENT)
          .addParam("args", "Special characters are not allowed in variable name");
    }
  }
}
