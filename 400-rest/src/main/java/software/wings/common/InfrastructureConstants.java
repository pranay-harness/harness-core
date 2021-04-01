package software.wings.common;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public final class InfrastructureConstants {
  public static final String DEFAULT_AWS_HOST_NAME_CONVENTION = "${host.ec2Instance.privateDnsName.split('\\.')[0]}";
  public static final String DEFAULT_AZURE_VM_HOST_NAME_CONVENTION =
      "${host.azureVMInstance.privateDnsName.split('\\.')[0]}";
  public static final String DEFAULT_WEB_APP_HOST_NAME_CONVENTION = "${host.webAppInstance.hostName}";
  public static final String PHASE_INFRA_MAPPING_KEY_NAME = "phaseInfraMappingKey_";
  public static final String INFRA_KUBERNETES_INFRAID_EXPRESSION = "${infra.kubernetes.infraId}";
  public static final String CONFIG_FILE_EXPRESSIONS = "${configFile.";

  public static final String RC_INFRA_STEP_NAME = "Acquire Resource Lock";

  public static final String INFRA_ID_EXPRESSION = "${infra.infraId}";

  public static final Integer WEEK_TIMEOUT = 7 * 24 * 60 * 60 * 1000;

  public static final String STATE_TIMEOUT_KEY_NAME = "timeoutMillis";

  public static final String QUEUING_RC_NAME = "Queuing";
}
