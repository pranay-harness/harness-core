/**
 *
 */

package software.wings.sm;

import static org.apache.commons.lang3.StringUtils.startsWith;

import org.apache.commons.lang3.StringUtils;
import software.wings.common.Constants;

import java.util.List;

/**
 * The Interface ExpressionProcessor.
 *
 * @author Rishi
 */
public interface ExpressionProcessor {
  /**
   * The constant EXPRESSION_NAME_DELIMITER.
   */
  String EXPRESSION_NAME_DELIMITER = ",";

  /**
   * The constant EXPRESSION_PREFIX.
   */
  String EXPRESSION_PREFIX = "${";

  /**
   * The constant EXPRESSION_SUFFIX.
   */
  String EXPRESSION_SUFFIX = "}";

  /**
   * Gets prefix object name.
   *
   * @return the prefix object name
   */
  String getPrefixObjectName();

  /**
   * Gets expression start pattern.
   *
   * @return the expression start pattern
   */
  List<String> getExpressionStartPatterns();

  /**
   * Gets expression equal pattern.
   *
   * @return the expression equal pattern
   */
  List<String> getExpressionEqualPatterns();

  /**
   * Gets context element type.
   *
   * @return the context element type
   */
  ContextElementType getContextElementType();

  /**
   * Matches boolean.
   *
   * @param expression the expression
   * @return the boolean
   */
  default boolean matches(String expression) {
    if (getExpressionStartPatterns()
            .stream()
            .filter(pattern -> startsWith(expression, pattern) || startsWith(expression, EXPRESSION_PREFIX + pattern))
            .findFirst()
            .isPresent()
        || getExpressionEqualPatterns()
               .stream()
               .filter(pattern
                   -> StringUtils.equals(expression, pattern)
                       || StringUtils.equals(expression, EXPRESSION_PREFIX + pattern + EXPRESSION_SUFFIX))
               .findFirst()
               .isPresent()) {
      return true;
    }
    return false;
  }

  /**
   * Normalize expression string.
   *
   * @param expression the expression
   * @return the string
   */
  default String normalizeExpression(String expression) {
    if (!matches(expression)) {
      return null;
    }
    expression = getPrefixObjectName() + "." + expression;
    if (!expression.endsWith(Constants.EXPRESSION_LIST_SUFFIX)) {
      expression = expression + Constants.EXPRESSION_LIST_SUFFIX;
    }
    return expression;
  }
}
