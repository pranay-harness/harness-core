package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutput;
import io.harness.context.ContextElementType;

import java.util.Map;

/**
 * Interface for all RepeatElements.
 */
@OwnedBy(CDC)
public interface ContextElement extends SweepingOutput {
  String WORKFLOW = "workflow";
  String DEPLOYMENT_URL = "deploymentUrl";
  String APP = "app";
  String ACCOUNT = "account";
  String SERVICE = "service";
  String SERVICE_TEMPLATE = "serviceTemplate";
  String ENV = "env";
  String HOST = "host";
  String INSTANCE = "instance";
  String PCF_INSTANCE = "pcfinstance";
  String ARTIFACT = "artifact";
  String SERVICE_VARIABLE = "serviceVariable";
  String ENVIRONMENT_VARIABLE = "environmentVariable";
  String SAFE_DISPLAY_SERVICE_VARIABLE = "safeDisplayServiceVariable";
  String TIMESTAMP_ID = "timestampId";
  String PIPELINE = "pipeline";
  String INFRA = "infra";
  String KUBERNETES = "kubernetes";
  String NAMESPACE = "namespace";
  String KUBECONFIG = "kubeconfig";
  String SHELL = "shell";

  ContextElementType getElementType();

  String getUuid();

  String getName();

  Map<String, Object> paramMap(ExecutionContext context);

  ContextElement cloneMin();
}
