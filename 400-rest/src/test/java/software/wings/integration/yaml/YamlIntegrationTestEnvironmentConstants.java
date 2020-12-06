package software.wings.integration.yaml;

public interface YamlIntegrationTestEnvironmentConstants {
  String ENVIRONMENT_1 = "harnessApiVersion: '1.0'\n"
      + "type: ENVIRONMENT\n"
      + "description: Description\n"
      + "environmentType: PROD\n";
  String ENVIRONMENT_1_SERVICE_INFRA = "harnessApiVersion: '1.0'\n"
      + "type: PHYSICAL_DATA_CENTER_SSH\n"
      + "computeProviderName: ComputeProviderPhysical\n"
      + "computeProviderType: PHYSICAL_DATA_CENTER\n"
      + "connection: User-Password\n"
      + "deploymentType: SSH\n"
      + "hostNames:\n"
      + "- hostName\n"
      + "infraMappingType: PHYSICAL_DATA_CENTER_SSH\n"
      + "serviceName: Service\n";
}
