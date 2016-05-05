package software.wings.sm;

import java.io.Serializable;

/**
 * Interface for all RepeatElements.
 * @author Rishi
 */
public interface Repeatable extends Serializable {
  public RepeatElementType getRepeatElementType();

  public String getName();
}
