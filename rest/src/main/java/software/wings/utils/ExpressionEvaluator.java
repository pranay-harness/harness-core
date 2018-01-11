/**
 *
 */

package software.wings.utils;

import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ro.fortsoft.pf4j.util.StringUtils;
import software.wings.exception.WingsException;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Class ExpressionEvaluator.
 *
 * @author Rishi
 */
public class ExpressionEvaluator {
  /**
   * The constant wingsVariablePattern.
   */
  public static final Pattern wingsVariablePattern = Pattern.compile("\\$\\{[^{}]*}");
  public static final Pattern variableNamePattern = Pattern.compile("^[_a-zA-Z][_\\w]*$");

  private static ExpressionEvaluator instance = new ExpressionEvaluator();
  private static final Logger logger = LoggerFactory.getLogger(ExpressionEvaluator.class);

  /**
   * Gets instance.
   *
   * @return the instance
   */
  public static ExpressionEvaluator getInstance() {
    return instance;
  }

  /**
   * Evaluate.
   *
   * @param expression the expression
   * @param name       the name
   * @param value      the value
   * @return the object
   */
  public Object evaluate(String expression, String name, Object value) {
    if (expression == null) {
      return expression;
    }

    logger.debug("evaluate request - expression: {}, name: {}, value: {}", expression, name, value);
    JexlEngine jexl = new JexlBuilder().create();
    JexlExpression expr = jexl.createExpression(expression);
    JexlContext jc = new MapContext();
    jc.set(name, value);

    Object retValue = expr.evaluate(jc);
    logger.debug("evaluate request - return value: {}", retValue);
    return retValue;
  }

  /**
   * Evaluate.
   *
   * @param expression the expression
   * @param context    the context
   * @return the object
   */
  public Object evaluate(String expression, Map<String, Object> context) {
    logger.debug("evaluate request - expression: {}, context: {}", expression, context);
    if (expression == null) {
      return expression;
    }

    JexlEngine jexl = new JexlBuilder().create();
    JexlExpression jexp = jexl.createExpression(expression);
    JexlContext jc = new MapContext();
    if (context != null) {
      for (String key : context.keySet()) {
        jc.set(key, context.get(key));
      }
    }
    Object retValue = jexp.evaluate(jc);
    logger.debug("evaluate request - return value: {}", retValue);
    return retValue;
  }

  /**
   * Evaluate.
   *
   * @param expression          the expression
   * @param context             the context
   * @param defaultObjectPrefix the default object prefix
   * @return the object
   */
  public Object evaluate(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    expression = normalizeExpression(expression, context, defaultObjectPrefix);
    return evaluate(expression, context);
  }

  private String normalizeExpression(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    if (expression == null) {
      return expression;
    }

    Matcher matcher = wingsVariablePattern.matcher(expression);

    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String variable = matcher.group(0);
      logger.debug("wingsVariable found: {}", variable);

      // remove $ and braces(${varName})
      variable = variable.substring(2, variable.length() - 1);

      String topObjectName = variable;
      if (topObjectName.indexOf('.') > 0) {
        topObjectName = topObjectName.substring(0, topObjectName.indexOf('.'));
      }

      if (!context.containsKey(topObjectName)) {
        variable = defaultObjectPrefix + "." + variable;
      }
      matcher.appendReplacement(sb, variable);
    }
    matcher.appendTail(sb);

    return sb.toString();
  }

  /**
   * Merge.
   *
   * @param expression the expression
   * @param context    the context
   * @return the string
   */
  public String merge(String expression, Map<String, Object> context) {
    return merge(expression, context, null);
  }

  /**
   * Merge.
   *
   * @param expression          the expression
   * @param context             the context
   * @param defaultObjectPrefix the default object prefix
   * @return the string
   */
  public String merge(String expression, Map<String, Object> context, String defaultObjectPrefix) {
    if (expression == null) {
      return null;
    }
    Matcher matcher = wingsVariablePattern.matcher(expression);

    StringBuffer sb = new StringBuffer();

    while (matcher.find()) {
      String variable = matcher.group(0);
      logger.debug("wingsVariable found: {}", variable);

      // remove $ and braces(${varName})
      variable = variable.substring(2, variable.length() - 1);

      if (defaultObjectPrefix != null) {
        String topObjectName = variable;
        if (topObjectName.indexOf('.') > 0) {
          topObjectName = topObjectName.substring(0, topObjectName.indexOf('.'));
        }
        if (!context.containsKey(topObjectName)) {
          variable = defaultObjectPrefix + "." + variable;
        }
      }

      String evaluatedValue;
      try {
        evaluatedValue = String.valueOf(evaluate(variable, context));
        if (evaluatedValue == null) {
          continue;
        }
        Matcher matcher2 = wingsVariablePattern.matcher(evaluatedValue);
        if (matcher2.find()) {
          evaluatedValue = merge(evaluatedValue, context, defaultObjectPrefix);
        }
      } catch (Exception e) {
        logger.warn("Ignoring exception -" + e.getMessage(), e);
        continue;
      }
      if (evaluatedValue.contains("$")) {
        String[] strings = evaluatedValue.split("\\$");
        StringBuffer sb2 = new StringBuffer();
        sb2.append(strings[0]);
        for (int i = 1; i < strings.length; i++) {
          sb2.append("\\$").append(strings[i]);
        }
        evaluatedValue = sb2.toString();
        logger.info(evaluatedValue);
      }
      matcher.appendReplacement(sb, evaluatedValue);
    }
    matcher.appendTail(sb);

    return sb.toString();
  }

  public static void isValidVariableName(String name) {
    // Verify Service variable name should not contain any special character
    if (StringUtils.isEmpty(name)) {
      return;
    }
    Matcher matcher = ExpressionEvaluator.variableNamePattern.matcher(name);
    if (!matcher.matches()) {
      throw new WingsException(INVALID_ARGUMENT)
          .addParam("args", "Special characters are not allowed in variable name");
    }
  }
}
