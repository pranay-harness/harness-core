package software.wings.sm;

import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * Interface for all RepeatElements.
 *
 * @author Rishi
 */
public interface ContextElement {
  /**
   * The constant APP_OBJECT_NAME.
   */
  static final String APP_OBJECT_NAME = "app";
  /**
   * The constant ENV_OBJECT_NAME.
   */
  static final String ENV_OBJECT_NAME = "env";
  /**
   * The constant HOST_OBJECT_NAME.
   */
  static final String HOST_OBJECT_NAME = "host";
  /**
   * The constant INSTANCE_OBJECT_NAME.
   */
  static final String INSTANCE_OBJECT_NAME = "instance";

  /**
   * Gets element type.
   *
   * @return the element type
   */
  public ContextElementType getElementType();

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName();

  /**
   * Param map.
   *
   * @return the map
   */
  public Map<String, Object> paramMap();
}
