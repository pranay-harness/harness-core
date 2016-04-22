package software.wings.sm;

import java.io.Serializable;

/**
 * Interface for all RepeatElements.
 * @author Rishi
 */
public interface RepeatElement extends Serializable {
  RepeatElementType getRepeatElementType();

  String getRepeatElementName();
}
