package software.wings.sm;

import java.util.Map;

/**
 * Interface for all RepeatElements.
 *
 * @author Rishi
 */
public interface ContextElement {
  /**
   * The constant APP
   */
  String APP = "app";
  /**
   * The constant SERVICE
   */
  String SERVICE = "service";

  /**
   * The constant APP.
   */
  String SERVICE_TEMPLATE = "serviceTemplate";

  /**
   * The constant ENV.
   */
  String ENV = "env";
  /**
   * The constant HOST.
   */
  String HOST = "host";
  /**
   * The constant INSTANCE.
   */
  String INSTANCE = "instance";

  /**
   * The constant TIMESTAMP_ID.
   */
  String TIMESTAMP_ID = "timestampId";

  /**
   * Gets element type.
   *
   * @return the element type
   */
  ContextElementType getElementType();

  /**
   * Gets uuid.
   *
   * @return uuid uuid
   */
  String getUuid();

  /**
   * Gets name.
   *
   * @return the name
   */
  String getName();

  /**
   * Param map.
   *
   * @return the map
   */
  Map<String, Object> paramMap();
}
