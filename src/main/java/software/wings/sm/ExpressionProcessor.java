/**
 *
 */
package software.wings.sm;

/**
 * @author Rishi
 */
public interface ExpressionProcessor {
  public String getPrefixObjectName();

  public String normalizeExpression(String expression);
}
