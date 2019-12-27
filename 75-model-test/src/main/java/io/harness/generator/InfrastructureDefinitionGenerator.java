package io.harness.generator;

import static io.harness.generator.SettingGenerator.Settings.AWS_SPOTINST_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.AWS_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.AZURE_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.DEV_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.GCP_PLAYGROUND;
import static io.harness.generator.SettingGenerator.Settings.PHYSICAL_DATA_CENTER;
import static io.harness.generator.SettingGenerator.Settings.SPOTINST_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.WINRM_TEST_CONNECTOR;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.SSH_DEPLOY_HOST;
import static io.harness.govern.Switch.unhandled;
import static java.util.Arrays.asList;
import static software.wings.beans.InfrastructureType.AWS_AMI;
import static software.wings.beans.InfrastructureType.AWS_AMI_LT;
import static software.wings.beans.InfrastructureType.AWS_ECS;
import static software.wings.beans.InfrastructureType.AWS_INSTANCE;
import static software.wings.beans.InfrastructureType.AWS_LAMBDA;
import static software.wings.beans.InfrastructureType.AZURE_SSH;
import static software.wings.beans.InfrastructureType.GCP_KUBERNETES_ENGINE;
import static software.wings.beans.InfrastructureType.PCF_INFRASTRUCTURE;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.services.ecs.model.LaunchType;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureProvisionerGenerator.InfrastructureProvisioners;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.generator.constants.InfraDefinitionGeneratorConstants;
import io.harness.testframework.restutils.InfrastructureDefinitionRestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.AmiDeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.AwsInstanceFilter.Tag;
import software.wings.beans.Environment;
import software.wings.beans.HostConnectionType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.dl.WingsPersistence;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.AwsEcsInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AwsLambdaInfrastructure;
import software.wings.infra.AzureInstanceInfrastructure;
import software.wings.infra.AzureKubernetesService;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;
import software.wings.infra.PcfInfraStructure;
import software.wings.infra.PhysicalInfra;
import software.wings.infra.PhysicalInfraWinrm;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;

@Singleton
public class InfrastructureDefinitionGenerator {
  private static final String AZURE_HELM_NAME = "Azure Helm";
  private static final String SUBSCRIPTION_ID = "12d2db62-5aa9-471d-84bb-faa489b3e319";
  private static final String PUNEET_AKS_RESOURCE_GROUP = "puneet-aks";
  private static final String MY_HARNESS_CLUSTER = "myHarnessCluster";
  private static final String DEFAULT_NAMESPACE = "default";
  private static final String DEFAULT_RELEASE_NAME = "release-${infra.kubernetes.infraId}";

  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;

  @Inject private InfrastructureDefinitionService infrastructureDefinitionService;
  @Inject private EnvironmentService environmentService;
  @Inject private ApplicationManifestService applicationManifestService;
  @Inject private WingsPersistence wingsPersistence;

  public InfrastructureDefinition ensurePredefined(
      Randomizer.Seed seed, Owners owners, String predefined, String bearerToken) {
    switch (predefined) {
      case GCP_KUBERNETES_ENGINE:
        return ensureGcpK8s(seed, owners, "gcp-k8s", bearerToken);
      case AWS_AMI:
        return ensureAwsAmi(seed, owners, bearerToken);
      case AWS_AMI_LT:
        return ensureAwsAmiWithLaunchTemplate(seed, owners, bearerToken);
      case AWS_INSTANCE:
        return ensureAwsSsh(seed, owners);
      case AWS_ECS:
        return ensureAwsEcs(seed, owners, bearerToken);
      case AZURE_SSH:
        return ensureAzureInstance(seed, owners, bearerToken);
      case AWS_LAMBDA:
        return ensureAwsLambda(seed, owners, bearerToken);
      case PCF_INFRASTRUCTURE:
        return ensurePcf(seed, owners, bearerToken);
      default:
        return null;
    }
  }

  public enum InfrastructureDefinitions {
    AWS_SSH_TEST,
    TERRAFORM_AWS_SSH_TEST,
    AWS_SSH_FUNCTIONAL_TEST,
    PHYSICAL_WINRM_TEST,
    PHYSICAL_SSH_TEST,
    AZURE_WINRM_TEST,
    ECS_EC2_TEST,
    ECS_FARGATE_TEST,
    K8S_ROLLING_TEST,
    K8S_CANARY_TEST,
    K8S_BLUE_GREEN_TEST,
    MULTI_ARTIFACT_AWS_SSH_FUNCTIONAL_TEST,
    AZURE_HELM,
    PIPELINE_RBAC_QA_AWS_SSH_TEST,
    PIPELINE_RBAC_PROD_AWS_SSH_TEST,
  }

  public InfrastructureDefinition ensurePredefined(
      Randomizer.Seed seed, Owners owners, InfrastructureDefinitions infraType) {
    switch (infraType) {
      case AWS_SSH_TEST:
        return ensureAwsSsh(seed, owners);
      case AWS_SSH_FUNCTIONAL_TEST:
        return ensureAwsSshFunctionalTest(seed, owners);
      case TERRAFORM_AWS_SSH_TEST:
        return ensureTerraformAwsSshTest(seed, owners);
      case PHYSICAL_WINRM_TEST:
        return ensurePhysicalWinRMTest(seed, owners);
      case PHYSICAL_SSH_TEST:
        return ensurePhysicalSSHTest(seed, owners);
      case AZURE_WINRM_TEST:
        return ensureAzureWinRMTest(seed, owners);
      case ECS_EC2_TEST:
        return ensureEcsEc2Test(seed, owners);
      case K8S_ROLLING_TEST:
        return ensureK8sTest(seed, owners, "fn-test-rolling");
      case K8S_BLUE_GREEN_TEST:
        return ensureK8sTest(seed, owners, "fn-test-bg");
      case K8S_CANARY_TEST:
        return ensureK8sTest(seed, owners, "fn-test-canary");
      case MULTI_ARTIFACT_AWS_SSH_FUNCTIONAL_TEST:
        return ensureMultiArtifactAwsSshFunctionalTest(seed, owners);
      case AZURE_HELM:
        return ensureAzureHelmInfraDef(seed, owners);
      case PIPELINE_RBAC_QA_AWS_SSH_TEST:
        return ensurePipelineRbacQaK8sTest(seed, owners);
      case PIPELINE_RBAC_PROD_AWS_SSH_TEST:
        return ensurePipelineRbacProdK8sTest(seed, owners);
      default:
        unhandled(infraType);
    }
    return null;
  }

  private InfrastructureDefinition ensureAzureHelmInfraDef(Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.FUNCTIONAL_TEST);
      owners.add(environment);
    }

    final SettingAttribute azureCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AZURE_TEST_CLOUD_PROVIDER);

    return ensureInfrastructureDefinition(InfrastructureDefinition.builder()
                                              .name(AZURE_HELM_NAME)
                                              .envId(environment.getUuid())
                                              .appId(environment.getAppId())
                                              .infrastructure(AzureKubernetesService.builder()
                                                                  .cloudProviderId(azureCloudProvider.getUuid())
                                                                  .subscriptionId(SUBSCRIPTION_ID)
                                                                  .resourceGroup(PUNEET_AKS_RESOURCE_GROUP)
                                                                  .clusterName(MY_HARNESS_CLUSTER)
                                                                  .namespace(DEFAULT_NAMESPACE)
                                                                  .releaseName(DEFAULT_RELEASE_NAME)
                                                                  .build())
                                              .deploymentType(DeploymentType.HELM)
                                              .cloudProviderType(CloudProviderType.AZURE)
                                              .build());
  }

  private InfrastructureDefinition ensureAzureWinRMTest(Randomizer.Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.WINDOWS_TEST);
      owners.add(service);
    }

    final SettingAttribute azureCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AZURE_TEST_CLOUD_PROVIDER);
    final SettingAttribute winRmSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, WINRM_TEST_CONNECTOR);

    return ensureInfrastructureDefinition(
        InfrastructureDefinition.builder()
            .name("Windows non prod - winrm azure workflow test")
            .envId(environment.getUuid())
            .appId(environment.getAppId())
            .infrastructure(AzureInstanceInfrastructure.builder()
                                .cloudProviderId(azureCloudProvider.getUuid())
                                .winRmConnectionAttributes(winRmSettingAttribute.getUuid())
                                .subscriptionId(InfraDefinitionGeneratorConstants.AZURE_SUBSCRIPTION_ID)
                                .resourceGroup(InfraDefinitionGeneratorConstants.AZURE_RESOURCE_GROUP)
                                .usePublicDns(true)
                                .build())
            .deploymentType(DeploymentType.WINRM)
            .cloudProviderType(CloudProviderType.AZURE)
            .scopedToServices(Collections.singletonList(service.getUuid()))
            .build());
  }

  private InfrastructureDefinition ensureEcsEc2Test(Randomizer.Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.ECS_TEST);
      owners.add(service);
    }

    final SettingAttribute ecsCloudProvider = settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);

    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .name("Ecs Ec2 type deployment Functional test" + System.currentTimeMillis())
            .infrastructure(AwsEcsInfrastructure.builder()
                                .cloudProviderId(ecsCloudProvider.getUuid())
                                .clusterName("SdkTesting")
                                .region("us-east-1")
                                .launchType("EC2")
                                .assignPublicIp(false)
                                .build())
            .deploymentType(DeploymentType.ECS)
            .cloudProviderType(CloudProviderType.AWS)
            .scopedToServices(Collections.singletonList(service.getUuid()))
            .envId(environment.getUuid())
            .appId(owners.obtainApplication().getUuid())
            .build();

    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  private InfrastructureDefinition ensureK8sTest(Randomizer.Seed seed, Owners owners, String namespace) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    return ensureK8sTest(seed, owners, environment, namespace);
  }

  private InfrastructureDefinition ensureK8sTest(
      Randomizer.Seed seed, Owners owners, Environment environment, String namespace) {
    ApplicationManifest applicationManifest =
        applicationManifestService.getByEnvId(environment.getAppId(), environment.getUuid(), AppManifestKind.VALUES);

    if (applicationManifest == null) {
      environmentService.createValues(environment.getAppId(), environment.getUuid(), null,
          ManifestFile.builder().fileName("values.yaml").fileContent("serviceType: ClusterIP\n").build(),
          AppManifestKind.VALUES);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.K8S_V2_TEST);
      owners.add(service);
    }

    final SettingAttribute gcpCloudProvider = settingGenerator.ensurePredefined(seed, owners, GCP_PLAYGROUND);

    String namespaceUnique = namespace + '-' + System.currentTimeMillis();

    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .name("exploration-harness-test-" + namespaceUnique)
            .infrastructure(GoogleKubernetesEngine.builder()
                                .cloudProviderId(gcpCloudProvider.getUuid())
                                .clusterName("us-central1-a/harness-test")
                                .namespace(namespaceUnique)
                                .build())
            .deploymentType(DeploymentType.KUBERNETES)
            .cloudProviderType(CloudProviderType.GCP)
            .scopedToServices(Collections.singletonList(service.getUuid()))
            .envId(environment.getUuid())
            .appId(owners.obtainApplication().getUuid())
            .build();

    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  private InfrastructureDefinition ensurePhysicalWinRMTest(Randomizer.Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.WINDOWS_TEST);
      owners.add(service);
    }

    final SettingAttribute physicalInfraSettingAttr =
        settingGenerator.ensurePredefined(seed, owners, PHYSICAL_DATA_CENTER);
    final SettingAttribute winRmSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, WINRM_TEST_CONNECTOR);

    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .name("Windows - winrm physical-infra workflow test")
            .infrastructure(
                PhysicalInfraWinrm.builder()
                    .cloudProviderId(physicalInfraSettingAttr.getUuid())
                    .winRmConnectionAttributes(winRmSettingAttribute.getUuid())
                    .hostNames(Collections.singletonList(InfraDefinitionGeneratorConstants.AZURE_DEPLOY_HOST))
                    .build())
            .deploymentType(DeploymentType.WINRM)
            .cloudProviderType(CloudProviderType.PHYSICAL_DATA_CENTER)
            .scopedToServices(Collections.singletonList(service.getUuid()))
            .envId(environment.getUuid())
            .appId(owners.obtainApplication().getUuid())
            .build();

    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  private InfrastructureDefinition ensurePhysicalSSHTest(Randomizer.Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);
      owners.add(service);
    }

    final SettingAttribute physicalInfraSettingAttr =
        settingGenerator.ensurePredefined(seed, owners, PHYSICAL_DATA_CENTER);
    final SettingAttribute hostConnectionAttribute =
        settingGenerator.ensurePredefined(seed, owners, DEV_TEST_CONNECTOR);

    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .name("SSH - physical-infra workflow test")
            .infrastructure(PhysicalInfra.builder()
                                .cloudProviderId(physicalInfraSettingAttr.getUuid())
                                .hostConnectionAttrs(hostConnectionAttribute.getUuid())
                                .hostNames(SSH_DEPLOY_HOST)
                                .build())
            .deploymentType(DeploymentType.SSH)
            .cloudProviderType(CloudProviderType.PHYSICAL_DATA_CENTER)
            .envId(environment.getUuid())
            .appId(owners.obtainApplication().getUuid())
            .build();

    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  private InfrastructureDefinition ensureAwsSshFunctionalTest(Randomizer.Seed seed, Owners owners) {
    return ensureAwsSshInfraDefinition(
        seed, owners, Environments.FUNCTIONAL_TEST, Services.FUNCTIONAL_TEST, "Aws non prod - ssh workflow test");
  }

  private InfrastructureDefinition ensureMultiArtifactAwsSshFunctionalTest(Randomizer.Seed seed, Owners owners) {
    return ensureAwsSshInfraDefinition(seed, owners, Environments.FUNCTIONAL_TEST,
        Services.MULTI_ARTIFACT_FUNCTIONAL_TEST, "Aws non prod - ssh workflow test-multi-artifact");
  }

  private InfrastructureDefinition ensurePipelineRbacQaK8sTest(Randomizer.Seed seed, Owners owners) {
    Environment environment = environmentGenerator.ensurePredefined(seed, owners, Environments.PIPELINE_RBAC_QA_TEST);
    return ensureK8sTest(seed, owners, environment, "fn-test-pipeline-rbac-qa");
  }

  private InfrastructureDefinition ensurePipelineRbacProdK8sTest(Randomizer.Seed seed, Owners owners) {
    Environment environment = environmentGenerator.ensurePredefined(seed, owners, Environments.PIPELINE_RBAC_PROD_TEST);
    return ensureK8sTest(seed, owners, environment, "fn-test-pipeline-prod-prod");
  }

  private InfrastructureDefinition ensureAwsSshInfraDefinition(
      Randomizer.Seed seed, Owners owners, Environments environments, Services services, String name) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, environments);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, services);
      owners.add(service);
    }

    final List<Tag> tags = asList(Tag.builder().key("Purpose").value("test").build(),
        Tag.builder().key("User").value(System.getProperty("user.name")).build());

    final SettingAttribute awsTestSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);
    final SettingAttribute devKeySettingAttribute = settingGenerator.ensurePredefined(seed, owners, DEV_TEST_CONNECTOR);

    return ensureInfrastructureDefinition(
        createInfraDefinition(tags, awsTestSettingAttribute.getUuid(), devKeySettingAttribute.getUuid(), name));
  }

  @NotNull
  private InfrastructureDefinition createInfraDefinition(
      List<Tag> tags, String awsTestSettingAttributeId, String devKeySettingAttributeId, String name) {
    AwsInstanceInfrastructure awsInstanceInfrastructure =
        AwsInstanceInfrastructure.builder()
            .cloudProviderId(awsTestSettingAttributeId)
            .hostConnectionAttrs(devKeySettingAttributeId)
            .region("us-east-1")
            .awsInstanceFilter(AwsInstanceFilter.builder().tags(tags).build())
            .usePublicDns(true)
            .hostConnectionType(HostConnectionType.PUBLIC_DNS.name())
            .build();

    return InfrastructureDefinition.builder()
        .name(name)
        .deploymentType(DeploymentType.SSH)
        .cloudProviderType(CloudProviderType.AWS)
        .infrastructure(awsInstanceInfrastructure)
        .build();
  }

  private InfrastructureDefinition ensureTerraformAwsSshTest(Randomizer.Seed seed, Owners owners) {
    InfrastructureProvisioner infrastructureProvisioner = owners.obtainInfrastructureProvisioner();
    if (infrastructureProvisioner == null) {
      infrastructureProvisioner =
          infrastructureProvisionerGenerator.ensurePredefined(seed, owners, InfrastructureProvisioners.TERRAFORM_TEST);
      owners.add(infrastructureProvisioner);
    }

    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    Service service = owners.obtainService();
    if (service == null) {
      service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);
      owners.add(service);
    }

    final SettingAttribute awsTestSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);
    final SettingAttribute devKeySettingAttribute = settingGenerator.ensurePredefined(seed, owners, DEV_TEST_CONNECTOR);

    AwsInstanceInfrastructure awsInstanceInfrastructure = AwsInstanceInfrastructure.builder()
                                                              .cloudProviderId(awsTestSettingAttribute.getUuid())
                                                              .hostConnectionAttrs(devKeySettingAttribute.getUuid())
                                                              .region("us-east-1")
                                                              .awsInstanceFilter(AwsInstanceFilter.builder().build())
                                                              .usePublicDns(true)
                                                              .hostConnectionType(HostConnectionType.PUBLIC_DNS.name())
                                                              .build();

    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name("Aws non prod - ssh terraform provisioner test")
                                                            .deploymentType(DeploymentType.SSH)
                                                            .cloudProviderType(CloudProviderType.AWS)
                                                            .infrastructure(awsInstanceInfrastructure)
                                                            .provisionerId(infrastructureProvisioner.getUuid())
                                                            .build();

    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  public InfrastructureDefinition ensureInfrastructureDefinition(InfrastructureDefinition infrastructureDefinition) {
    InfrastructureDefinition existing = exists(infrastructureDefinition);
    if (existing != null) {
      return existing;
    }

    return infrastructureDefinitionService.save(infrastructureDefinition, false, true);
  }

  private InfrastructureDefinition ensurePcf(Seed seed, Owners owners, String bearerToken) {
    Environment environment = ensureEnv(seed, owners);
    final SettingAttribute pcfCloudProvider = settingGenerator.ensurePredefined(seed, owners, Settings.PCF_CONNECTOR);

    PcfInfraStructure pcfInfraStructure = PcfInfraStructure.builder()
                                              .organization("Harness")
                                              .space("CD-Test-space")
                                              .cloudProviderId(pcfCloudProvider.getUuid())
                                              .build();

    String name = HarnessStringUtils.join(StringUtils.EMPTY, "pcf-definition");
    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(name)
                                                            .infrastructure(pcfInfraStructure)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .cloudProviderType(CloudProviderType.PCF)
                                                            .deploymentType(DeploymentType.PCF)
                                                            .build();
    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  private Environment ensureEnv(Randomizer.Seed seed, Owners owners) {
    Application application = owners.obtainApplication();
    if (application == null) {
      applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    }
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
    }
    return environment;
  }

  private InfrastructureDefinition ensureAwsEcs(Randomizer.Seed seed, Owners owners, String bearerToken) {
    Environment environment = ensureEnv(seed, owners);
    final SettingAttribute awsCloudProvider = settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);

    AwsEcsInfrastructure awsEcsInfrastructure = AwsEcsInfrastructure.builder()
                                                    .region("us-east-1")
                                                    .clusterName("qb-dev-cluster")
                                                    .cloudProviderId(awsCloudProvider.getUuid())
                                                    .launchType(LaunchType.EC2.toString())
                                                    .build();

    String name = HarnessStringUtils.join(StringUtils.EMPTY, "aws-ecs-", Long.toString(System.currentTimeMillis()));
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

  private InfrastructureDefinition ensureAwsSsh(Seed seed, Owners owners) {
    Environment environment = ensureEnv(seed, owners);
    final SettingAttribute awsCloudProvider = settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);

    final SettingAttribute devKeySettingAttribute = settingGenerator.ensurePredefined(seed, owners, DEV_TEST_CONNECTOR);

    AwsInstanceInfrastructure awsInstanceInfrastructure = AwsInstanceInfrastructure.builder()
                                                              .cloudProviderId(awsCloudProvider.getUuid())
                                                              .hostConnectionAttrs(devKeySettingAttribute.getUuid())
                                                              .region("us-east-1")
                                                              .awsInstanceFilter(AwsInstanceFilter.builder().build())
                                                              .build();

    String name = "aws-ssh";

    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(name)
                                                            .deploymentType(DeploymentType.SSH)
                                                            .cloudProviderType(CloudProviderType.AWS)
                                                            .infrastructure(awsInstanceInfrastructure)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .build();

    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  private InfrastructureDefinition ensureGcpK8s(
      Randomizer.Seed seed, Owners owners, String namespace, String bearerToken) {
    Environment environment = ensureEnv(seed, owners);

    final SettingAttribute gcpK8sCloudProvider = settingGenerator.ensurePredefined(seed, owners, GCP_PLAYGROUND);

    final String nameSpaceUnique =
        HarnessStringUtils.join(StringUtils.EMPTY, namespace, Long.toString(System.currentTimeMillis()));

    String name = HarnessStringUtils.join(StringUtils.EMPTY, "gcp-k8s-", Long.toString(System.currentTimeMillis()));

    GoogleKubernetesEngine gcpK8sInfra = GoogleKubernetesEngine.builder()
                                             .cloudProviderId(gcpK8sCloudProvider.getUuid())
                                             .namespace(nameSpaceUnique)
                                             .clusterName(InfraDefinitionGeneratorConstants.GCP_CLUSTER)
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

    final SettingAttribute awsCloudProvider = settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);

    List<String> autoScalingGroups = InfrastructureDefinitionRestUtils.listAutoScalingGroups(
        bearerToken, accountId, appId, awsCloudProvider.getUuid(), region);

    Assertions.assertThat(autoScalingGroups).isNotEmpty();

    return ensureAwsAmi(seed, owners, autoScalingGroups.get(0), "aws-ami-infradef");
  }
  private InfrastructureDefinition ensureAwsAmiWithLaunchTemplate(
      Randomizer.Seed seed, Owners owners, String bearerToken) {
    return ensureAwsAmi(seed, owners, "asg-ami-functional-test", "aws-ami-lt-infradef");
  }

  private InfrastructureDefinition ensureAwsAmi(
      Randomizer.Seed seed, Owners owners, String asgName, String infradefName) {
    Environment environment = ensureEnv(seed, owners);
    final String region = "us-east-1";

    final SettingAttribute awsCloudProvider = settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);
    AwsAmiInfrastructure awsAmiInfrastructure = AwsAmiInfrastructure.builder()
                                                    .cloudProviderId(awsCloudProvider.getUuid())
                                                    .region(region)
                                                    .autoScalingGroupName(asgName)
                                                    .build();

    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(infradefName)
                                                            .cloudProviderType(CloudProviderType.AWS)
                                                            .deploymentType(DeploymentType.AMI)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .infrastructure(awsAmiInfrastructure)
                                                            .build();
    return GeneratorUtils.suppressDuplicateException(
        ()
            -> infrastructureDefinitionService.save(infrastructureDefinition, false),
        () -> exists(infrastructureDefinition));
  }

  public InfrastructureDefinition ensureSpotinstAmiDeployment(Randomizer.Seed seed, Owners owners, String bearerToken)
      throws IOException {
    Environment environment = ensureEnv(seed, owners);
    final String region = "us-east-1";

    final SettingAttribute awsCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AWS_SPOTINST_TEST_CLOUD_PROVIDER);
    final SettingAttribute spotinstProvider =
        settingGenerator.ensurePredefined(seed, owners, SPOTINST_TEST_CLOUD_PROVIDER);

    InputStream in = getClass().getClassLoader().getResourceAsStream("generator/elasticGroupConfig.json");

    Preconditions.checkNotNull(in);

    AwsAmiInfrastructure awsAmiInfrastructure =
        AwsAmiInfrastructure.builder()
            .cloudProviderId(awsCloudProvider.getUuid())
            .region(region)
            .hostNameConvention("${host.ec2Instance.privateDnsName.split('\\.')[0]}")
            .amiDeploymentType(AmiDeploymentType.SPOTINST)
            .spotinstElastiGroupJson(IOUtils.toString(in, StandardCharsets.UTF_8))
            .spotinstCloudProvider(spotinstProvider.getUuid())
            .build();

    String name =
        HarnessStringUtils.join(StringUtils.EMPTY, "aws-ami-spotinst-", Long.toString(System.currentTimeMillis()));
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

  private InfrastructureDefinition ensureAwsLambda(Randomizer.Seed seed, Owners owners, String bearerToken) {
    Environment environment = ensureEnv(seed, owners);
    final String region = "us-east-1";

    final SettingAttribute awsCloudProvider = settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);

    AwsLambdaInfrastructure awsLambdaInfrastructure = AwsLambdaInfrastructure.builder()
                                                          .cloudProviderId(awsCloudProvider.getUuid())
                                                          .region(region)
                                                          .role(GeneratorConstants.AWS_TEST_LAMBDA_ROLE)
                                                          .build();

    String name = HarnessStringUtils.join(StringUtils.EMPTY, "aws-lamda-", Long.toString(System.currentTimeMillis()));
    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(name)
                                                            .cloudProviderType(CloudProviderType.AWS)
                                                            .deploymentType(DeploymentType.AWS_LAMBDA)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .infrastructure(awsLambdaInfrastructure)
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

    String name = HarnessStringUtils.join(StringUtils.EMPTY, "azure-winrm-", Long.toString(System.currentTimeMillis()));

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
