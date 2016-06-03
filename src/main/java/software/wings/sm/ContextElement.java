package software.wings.sm;

import java.util.Map;

/**
 * Interface for all RepeatElements.
 *
 * @author Rishi
 */

public interface ContextElement {
  static final String APP_OBJECT_NAME = "app";
  static final String ENV_OBJECT_NAME = "env";
  static final String HOST_OBJECT_NAME = "host";

  public ContextElementType getElementType();

  public String getName();

  public Map<String, Object> paramMap();
}
