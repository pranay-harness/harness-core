package software.wings.sm;

import java.util.Map;

/**
 * Interface for all RepeatElements.
 *
 * @author Rishi
 */

public interface ContextElement {
  public ContextElementType getElementType();

  public String getName();

  public Map<String, Object> paramMap();
}
