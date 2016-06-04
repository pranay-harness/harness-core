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
  public static final String EXPRESSION_NAME_DELIMITER = ",";

  String getPrefixObjectName();

  /**
   * Normalize expression.
   *
   * @param expression the expression
   * @return the string
   */
  String normalizeExpression(String expression);
}
