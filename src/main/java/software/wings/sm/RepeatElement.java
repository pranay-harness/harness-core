/**
 *
 */
package software.wings.sm;

import java.io.Serializable;

/**
 * @author Rishi
 */
public interface RepeatElement extends Serializable {
  public RepeatElementType getRepeatElementType();

  public String getRepeatElementName();
}
