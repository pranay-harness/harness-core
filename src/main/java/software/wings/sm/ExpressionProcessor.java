/**
 *
 */
package software.wings.sm;

/**
 * @author Rishi
 */
public interface ExpressionProcessor {
  public static final String EXPRESSION_NAME_DELIMITER = ",";

  String getPrefixObjectName();

  String normalizeExpression(String expression);
}
