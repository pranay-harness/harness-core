package software.wings.common;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class InfrastructureConstants {
  public static final String DEFAULT_AWS_HOST_NAME_CONVENTION = "${host.ec2Instance.privateDnsName.split('\\.')[0]}";
  public static final String PHASE_INFRA_MAPPING_KEY = "phaseInfraMappingKey";
  public static final String INFRA_KUBERNETES_INFRAID_EXPRESSION = "${infra.kubernetes.infraId}";
}
