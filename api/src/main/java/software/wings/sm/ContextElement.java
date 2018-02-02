package software.wings.sm;

import java.util.Map;

/**
 * Interface for all RepeatElements.
 *
 * @author Rishi
 */
public interface ContextElement {
  String WORKFLOW = "workflow";
  String DEPLOYMENT_URL = "deploymentUrl";
  String APP = "app";
  String SERVICE = "service";
  String SERVICE_TEMPLATE = "serviceTemplate";
  String ENV = "env";
  String HOST = "host";
  String INSTANCE = "instance";
  String ARTIFACT = "artifact";
  String SERVICE_VARIABLE = "serviceVariable";
  String SAFE_DISPLAY_SERVICE_VARIABLE = "safeDisplayServiceVariable";
  String TIMESTAMP_ID = "timestampId";
  String PIPELINE = "pipeline";

  ContextElementType getElementType();

  String getUuid();

  String getName();

  Map<String, Object> paramMap(ExecutionContext context);

  ContextElement cloneMin();
}
