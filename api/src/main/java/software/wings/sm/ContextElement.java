package software.wings.sm;

import java.util.Map;

/**
 * Interface for all RepeatElements.
 *
 * @author Rishi
 */
public interface ContextElement {
  /**
   * The constant WORKFLOW
   */
  String WORKFLOW = "workflow";

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
   * The constant ARTIFACT.
   */
  String ARTIFACT = "artifact";

  /**
   * The constant SERVICE_VARIABLE.
   */
  String SERVICE_VARIABLE = "serviceVariable";
  /**
   * The constant SAFE_DISPLAY_SERVICE_VARIABLE.
   */
  String SAFE_DISPLAY_SERVICE_VARIABLE = "safeDisplayServiceVariable";
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
  Map<String, Object> paramMap(ExecutionContext context);

  ContextElement cloneMin();
}
