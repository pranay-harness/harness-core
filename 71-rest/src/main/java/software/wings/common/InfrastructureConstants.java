package software.wings.common;

public interface InfrastructureConstants {
  String DEFAULT_AWS_HOST_NAME_CONVENTION = "${host.ec2Instance.privateDnsName.split('\\.')[0]}";

  String PHASE_INFRA_MAPPING_KEY = "phaseInfraMappingKey";
}
