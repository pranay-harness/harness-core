package io.harness.generator;

import static io.harness.generator.SettingGenerator.Settings.AZURE_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.DEV_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.WINRM_TEST_CONNECTOR;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.LaunchType;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.generator.constants.InfraDefinitionGeneratorConstants;
import io.harness.testframework.restutils.InfrastructureDefinitionRestUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.dl.WingsPersistence;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.AwsEcsInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AzureInstanceInfrastructure;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;

import java.util.List;

@Singleton
public class InfrastructureDefinitionGenerator {
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private ApplicationGenerator applicationGenerator;

  @Inject private WingsPersistence wingsPersistence;

  private final String gcpCluster = "us-central1-a/harness-test";

  public InfrastructureDefinition ensurePredefined(
      Randomizer.Seed seed, Owners owners, String predefined, String bearerToken) {
    switch (predefined) {
      case InfrastructureType.GCP_KUBERNETES_ENGINE:
        return ensureGcpK8s(seed, owners, "gcp-k8s", bearerToken);
      case InfrastructureType.AWS_AMI:
        return ensureAwsAmi(seed, owners, bearerToken);
      case InfrastructureType.AWS_INSTANCE:
        return ensureAwsSsh(seed, owners, bearerToken);
      case InfrastructureType.AWS_ECS:
        return ensureAwsEcs(seed, owners, bearerToken);
      case InfrastructureType.AZURE_SSH:
        return ensureAzureInstance(seed, owners, bearerToken);
      default:
        return null;
    }
  }

  private Environment ensureEnv(Randomizer.Seed seed, Owners owners) {
    Application application = owners.obtainApplication();
    if (application == null) {
      application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    }
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
    }
    return environment;
  }

  private InfrastructureDefinition ensureAwsEcs(Randomizer.Seed seed, Owners owners, String bearerToken) {
    Environment environment = ensureEnv(seed, owners);
    final SettingAttribute awsCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, Settings.AWS_TEST_CLOUD_PROVIDER);

    AwsEcsInfrastructure awsEcsInfrastructure = AwsEcsInfrastructure.builder()
                                                    .region("us-east-1")
                                                    .clusterName("qb-dev-cluster")
                                                    .cloudProviderId(awsCloudProvider.getUuid())
                                                    .launchType(LaunchType.EC2.toString())
                                                    .build();

    String name = Joiner.on(StringUtils.EMPTY).join("aws-ecs", System.currentTimeMillis());
    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(name)
                                                            .infrastructure(awsEcsInfrastructure)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .cloudProviderType(CloudProviderType.AWS)
                                                            .deploymentType(DeploymentType.ECS)
                                                            .build();
    return GeneratorUtils.suppressDuplicateException(
        ()
            -> InfrastructureDefinitionRestUtils.save(bearerToken, infrastructureDefinition),
        () -> exists(infrastructureDefinition));
  }

  private InfrastructureDefinition ensureAwsSsh(Seed seed, Owners owners, String bearerToken) {
    Environment environment = ensureEnv(seed, owners);
    final SettingAttribute awsCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, Settings.AWS_TEST_CLOUD_PROVIDER);

    final SettingAttribute devKeySettingAttribute = settingGenerator.ensurePredefined(seed, owners, DEV_TEST_CONNECTOR);

    AwsInstanceInfrastructure awsInstanceInfrastructure = AwsInstanceInfrastructure.builder()
                                                              .cloudProviderId(awsCloudProvider.getUuid())
                                                              .hostConnectionAttrs(devKeySettingAttribute.getUuid())
                                                              .region("us-east-1")
                                                              .awsInstanceFilter(AwsInstanceFilter.builder().build())
                                                              .build();

    String name = Joiner.on(StringUtils.EMPTY).join("aws-ssh", System.currentTimeMillis());

    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(name)
                                                            .deploymentType(DeploymentType.SSH)
                                                            .cloudProviderType(CloudProviderType.AWS)
                                                            .infrastructure(awsInstanceInfrastructure)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .build();
    return GeneratorUtils.suppressDuplicateException(
        ()
            -> InfrastructureDefinitionRestUtils.save(bearerToken, infrastructureDefinition),
        () -> exists(infrastructureDefinition));
  }

  private InfrastructureDefinition ensureGcpK8s(
      Randomizer.Seed seed, Owners owners, String namespace, String bearerToken) {
    Environment environment = ensureEnv(seed, owners);

    final SettingAttribute gcpK8sCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, Settings.GCP_PLAYGROUND);

    final String nameSpaceUnique = Joiner.on(StringUtils.EMPTY).join(namespace, System.currentTimeMillis());

    String name = Joiner.on(StringUtils.EMPTY).join("gcp-k8s", System.currentTimeMillis());

    GoogleKubernetesEngine gcpK8sInfra = GoogleKubernetesEngine.builder()
                                             .cloudProviderId(gcpK8sCloudProvider.getUuid())
                                             .namespace(nameSpaceUnique)
                                             .clusterName(gcpCluster)
                                             .releaseName("release-${infra.kubernetes.infraId}")
                                             .build();
    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(name)
                                                            .deploymentType(DeploymentType.KUBERNETES)
                                                            .cloudProviderType(CloudProviderType.GCP)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .infrastructure(gcpK8sInfra)
                                                            .build();
    return GeneratorUtils.suppressDuplicateException(
        ()
            -> InfrastructureDefinitionRestUtils.save(bearerToken, infrastructureDefinition),
        () -> exists(infrastructureDefinition));
  }

  private InfrastructureDefinition ensureAwsAmi(Randomizer.Seed seed, Owners owners, String bearerToken) {
    Environment environment = ensureEnv(seed, owners);
    final String region = "us-east-1";
    final String accountId = environment.getAccountId();
    final String appId = environment.getAppId();

    final SettingAttribute awsCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, Settings.AWS_TEST_CLOUD_PROVIDER);

    List<String> autoScalingGroups = InfrastructureDefinitionRestUtils.listAutoScalingGroups(
        bearerToken, accountId, appId, awsCloudProvider.getUuid(), region);

    Assertions.assertThat(autoScalingGroups).isNotEmpty();

    AwsAmiInfrastructure awsAmiInfrastructure = AwsAmiInfrastructure.builder()
                                                    .cloudProviderId(awsCloudProvider.getUuid())
                                                    .region(region)
                                                    .autoScalingGroupName(autoScalingGroups.get(0))
                                                    .build();

    String name = Joiner.on(StringUtils.EMPTY).join("aws-ami", System.currentTimeMillis());
    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(name)
                                                            .cloudProviderType(CloudProviderType.AWS)
                                                            .deploymentType(DeploymentType.AMI)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .infrastructure(awsAmiInfrastructure)
                                                            .build();
    return GeneratorUtils.suppressDuplicateException(
        ()
            -> InfrastructureDefinitionRestUtils.save(bearerToken, infrastructureDefinition),
        () -> exists(infrastructureDefinition));
  }

  private InfrastructureDefinition ensureAzureInstance(Randomizer.Seed seed, Owners owners, String bearerToken) {
    Environment environment = ensureEnv(seed, owners);
    final SettingAttribute azureCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AZURE_TEST_CLOUD_PROVIDER);
    final SettingAttribute winRmConnectionAttr = settingGenerator.ensurePredefined(seed, owners, WINRM_TEST_CONNECTOR);

    String name = Joiner.on(StringUtils.EMPTY).join("azure-winrm-", System.currentTimeMillis());

    AzureInstanceInfrastructure azureInstanceInfrastructure =
        AzureInstanceInfrastructure.builder()
            .cloudProviderId(azureCloudProvider.getUuid())
            .winRmConnectionAttributes(winRmConnectionAttr.getUuid())
            .subscriptionId(InfraDefinitionGeneratorConstants.AZURE_SUBSCRIPTION_ID)
            .resourceGroup(InfraDefinitionGeneratorConstants.AZURE_RESOURCE_GROUP)
            .build();

    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(name)
                                                            .cloudProviderType(CloudProviderType.AZURE)
                                                            .deploymentType(DeploymentType.WINRM)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .infrastructure(azureInstanceInfrastructure)
                                                            .build();

    return GeneratorUtils.suppressDuplicateException(
        ()
            -> InfrastructureDefinitionRestUtils.save(bearerToken, infrastructureDefinition),
        () -> exists(infrastructureDefinition));
  }

  private InfrastructureDefinition exists(InfrastructureDefinition infrastructureDefinition) {
    return wingsPersistence.createQuery(InfrastructureDefinition.class)
        .filter(InfrastructureDefinitionKeys.appId, infrastructureDefinition.getAppId())
        .filter(InfrastructureDefinitionKeys.envId, infrastructureDefinition.getEnvId())
        .filter(InfrastructureDefinitionKeys.name, infrastructureDefinition.getName())
        .get();
  }
}
