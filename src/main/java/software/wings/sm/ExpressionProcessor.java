/**
 *
 */

package software.wings.sm;

// TODO: Auto-generated Javadoc

/**
 * The Interface ExpressionProcessor.
 *
 * @author Rishi
 */
public interface ExpressionProcessor {
  /**
   * The constant EXPRESSION_NAME_DELIMITER.
   */
  public static final String EXPRESSION_NAME_DELIMITER = ",";

  /**
   * Gets prefix object name.
   *
   * @return the prefix object name
   */
  String getPrefixObjectName();

  /**
   * Normalize expression.
   *
   * @param expression the expression
   * @return the string
   */
  String normalizeExpression(String expression);

  boolean matches(String expression);

  ContextElementType getContextElementType();
}
