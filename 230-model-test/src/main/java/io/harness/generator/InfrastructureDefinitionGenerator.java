/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.generator.SettingGenerator.Settings.AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.AWS_SPOTINST_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.AWS_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.AZURE_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.AZURE_VMSS_SSH_PUBLIC_KEY_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.DEV_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.GCP_PLAYGROUND;
import static io.harness.generator.SettingGenerator.Settings.GCP_QA_TARGET;
import static io.harness.generator.SettingGenerator.Settings.OPENSHIFT_TEST_CLUSTER;
import static io.harness.generator.SettingGenerator.Settings.PHYSICAL_DATA_CENTER;
import static io.harness.generator.SettingGenerator.Settings.SPOTINST_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.WINRM_DEV_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.WINRM_TEST_CONNECTOR;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_FUNCTIONAL_TEST_APP_SERVICE_RESOURCE_GROUP;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_FUNCTIONAL_TEST_RESOURCE_GROUP;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_SUBSCRIPTION_ID;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_API_INFRA_DEFINITION_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_BASE_SCALE_SET_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_BASIC_INFRA_DEFINITION_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_BLUE_GREEN_INFRA_DEFINITION_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_VM_USERNAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_WEB_APP_API_INFRA_DEFINITION_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_WEB_APP_BLUE_GREEN_INFRA_DEFINITION_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_WEB_APP_BLUE_GREEN_ROLLBACK_INFRA_DEFINITION_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_WEB_APP_CANARY_INFRA_DEFINITION_NAME;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.SSH_DEPLOY_HOST;
import static io.harness.govern.Switch.unhandled;

import static software.wings.beans.InfrastructureType.AWS_AMI;
import static software.wings.beans.InfrastructureType.AWS_AMI_LT;
import static software.wings.beans.InfrastructureType.AWS_ECS;
import static software.wings.beans.InfrastructureType.AWS_INSTANCE;
import static software.wings.beans.InfrastructureType.AWS_LAMBDA;
import static software.wings.beans.InfrastructureType.AZURE_SSH;
import static software.wings.beans.InfrastructureType.GCP_KUBERNETES_ENGINE;
import static software.wings.beans.InfrastructureType.PCF_INFRASTRUCTURE;
import static software.wings.beans.InfrastructureType.PDC;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.HarnessStringUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureProvisionerGenerator.InfrastructureProvisioners;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.generator.constants.InfraDefinitionGeneratorConstants;
import io.harness.testframework.restutils.InfrastructureDefinitionRestUtils;

import software.wings.api.CloudProviderType;
import software.wings.api.DeploymentType;
import software.wings.beans.AmiDeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.AwsInstanceFilter.Tag;
import software.wings.beans.Environment;
import software.wings.beans.HostConnectionType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.VMSSAuthType;
import software.wings.beans.VMSSDeploymentType;
import software.wings.dl.WingsPersistence;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.AwsEcsInfrastructure;
import software.wings.infra.AwsInstanceInfrastructure;
import software.wings.infra.AwsLambdaInfrastructure;
import software.wings.infra.AzureInstanceInfrastructure;
import software.wings.infra.AzureKubernetesService;
import software.wings.infra.AzureVMSSInfra;
import software.wings.infra.AzureWebAppInfra;
import software.wings.infra.CustomInfrastructure;
import software.wings.infra.DirectKubernetesInfrastructure;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;
import software.wings.infra.PcfInfraStructure;
import software.wings.infra.PhysicalInfra;
import software.wings.infra.PhysicalInfraWinrm;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;

import com.amazonaws.services.ecs.model.LaunchType;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDP)
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
        return ensureGcpK8s(seed, owners, "gcp-k8s");
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
      case PDC:
        return ensurePDC(seed, owners, "PDC");
      default:
        return null;
    }
  }

  public InfrastructureDefinition ensurePDC(Seed seed, Owners owners, String name) {
    Environment environment = ensureEnv(seed, owners);

    final SettingAttribute pcfCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, Settings.PHYSICAL_DATA_CENTER);
    final SettingAttribute hostConnectionAttribute =
        settingGenerator.ensurePredefined(seed, owners, DEV_TEST_CONNECTOR);

    PhysicalInfra pdcInfraStructure = PhysicalInfra.builder()
                                          .cloudProviderId(pcfCloudProvider.getUuid())
                                          .hostConnectionAttrs(hostConnectionAttribute.getUuid())
                                          .hostNames(asList("host1", "host2"))
                                          .build();
    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(name)
                                                            .infrastructure(pdcInfraStructure)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .cloudProviderType(CloudProviderType.PHYSICAL_DATA_CENTER)
                                                            .deploymentType(DeploymentType.SSH)
                                                            .build();
    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  public enum InfrastructureDefinitions {
    AWS_SSH_TEST,
    AWS_LAMBDA_TEST,
    TERRAFORM_AWS_SSH_TEST,
    AWS_SSH_FUNCTIONAL_TEST,
    AWS_WINRM_FUNCTIONAL_TEST,
    PHYSICAL_WINRM_TEST,
    PHYSICAL_SSH_TEST,
    AZURE_WINRM_TEST,
    ECS_EC2_TEST,
    ECS_FARGATE_TEST,
    ECS_DEPLOYMENT_FUNCTIONAL_TEST,
    K8S_ROLLING_TEST,
    K8S_CANARY_TEST,
    K8S_BLUE_GREEN_TEST,
    K8S_CUSTOM_MANIFEST,
    OPENSHIFT_CUSTOM_MANIFEST,
    MULTI_ARTIFACT_AWS_SSH_FUNCTIONAL_TEST,
    AZURE_HELM,
    GCP_HELM,
    GCP_HELM_CUSTOM_MANIFEST_TEST,
    PIPELINE_RBAC_QA_AWS_SSH_TEST,
    PIPELINE_RBAC_PROD_AWS_SSH_TEST,
    AWS_WINRM_DOWNLOAD,
    AWS_SSH_FUNCTIONAL_TEST_NAS,
    AZURE_VMSS_BASIC_TEST,
    AZURE_VMSS_BLUE_GREEN_TEST,
    AZURE_VMSS_API_TEST,
    AZURE_WEB_APP_BLUE_GREEN_TEST,
    AZURE_WEB_APP_BLUE_GREEN_ROLLBACK_TEST,
    AZURE_WEB_APP_CANARY_TEST,
    AZURE_WEB_APP_API_TEST;
  }

  public InfrastructureDefinition ensurePredefined(
      Randomizer.Seed seed, Owners owners, InfrastructureDefinitions infraType) {
    switch (infraType) {
      case AWS_SSH_TEST:
        return ensureAwsSsh(seed, owners);
      case AWS_LAMBDA_TEST:
        return ensureAwsLambda(seed, owners);
      case AWS_WINRM_FUNCTIONAL_TEST:
        return ensureAwsWinrmFunctionalTest(seed, owners);
      case AWS_SSH_FUNCTIONAL_TEST:
        return ensureAwsSshFunctionalTest(seed, owners);
      case AWS_SSH_FUNCTIONAL_TEST_NAS:
        return ensureAwsSshFunctionalTestNAS(seed, owners);
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
      case ECS_DEPLOYMENT_FUNCTIONAL_TEST:
        return ensureEcsEc2DeploymentTest(seed, owners);
      case K8S_ROLLING_TEST:
        return ensureK8sTest(seed, owners, "fn-test-rolling");
      case K8S_BLUE_GREEN_TEST:
        return ensureK8sTest(seed, owners, "fn-test-bg");
      case K8S_CANARY_TEST:
        return ensureK8sTest(seed, owners, "fn-test-canary");
      case K8S_CUSTOM_MANIFEST:
        return ensureK8sTest(seed, owners, "fn-test-custom-manifest");
      case OPENSHIFT_CUSTOM_MANIFEST:
        return ensureOpenshiftTest(seed, owners, "fn-test-custom-manifest");
      case MULTI_ARTIFACT_AWS_SSH_FUNCTIONAL_TEST:
        return ensureMultiArtifactAwsSshFunctionalTest(seed, owners);
      case AZURE_HELM:
        return ensureAzureHelmInfraDef(seed, owners);
      case GCP_HELM:
        return ensureGcpHelmInfraDef(seed, owners, "fn-test-helm");
      case GCP_HELM_CUSTOM_MANIFEST_TEST:
        return ensureGcpHelmCustomManifestInfraDef(seed, owners, "fn-test-helm");
      case PIPELINE_RBAC_QA_AWS_SSH_TEST:
        return ensurePipelineRbacQaK8sTest(seed, owners);
      case PIPELINE_RBAC_PROD_AWS_SSH_TEST:
        return ensurePipelineRbacProdK8sTest(seed, owners);
      case AWS_WINRM_DOWNLOAD:
        return ensureAwsWinrmDownloadTest(seed, owners);
      case AZURE_VMSS_BASIC_TEST:
        return ensureAzureVMSSBasicTest(seed, owners);
      case AZURE_VMSS_BLUE_GREEN_TEST:
        return ensureAzureVMSSBlueGreenTest(seed, owners);
      case AZURE_VMSS_API_TEST:
        return ensureAzureVMSSAPITest(seed, owners);
      case AZURE_WEB_APP_BLUE_GREEN_TEST:
        return ensureAzureWebAppBlueGreenTest(seed, owners);
      case AZURE_WEB_APP_BLUE_GREEN_ROLLBACK_TEST:
        return ensureAzureWebAppBlueGreenRollbackTest(seed, owners);
      case AZURE_WEB_APP_CANARY_TEST:
        return ensureAzureWebAppCanaryTest(seed, owners);
      case AZURE_WEB_APP_API_TEST:
        return ensureAzureWebAppAPITest(seed, owners);
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

  private InfrastructureDefinition ensureGcpHelmInfraDef(Seed seed, Owners owners, String namespace) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.FUNCTIONAL_TEST);
      owners.add(environment);
    }

    final SettingAttribute gcpCloudProvider = settingGenerator.ensurePredefined(seed, owners, GCP_PLAYGROUND);

    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .name("harness-test-helm-" + namespace)
            .infrastructure(GoogleKubernetesEngine.builder()
                                .cloudProviderId(gcpCloudProvider.getUuid())
                                .clusterName("us-central1-a/harness-test")
                                .namespace(namespace)
                                .build())
            .deploymentType(DeploymentType.HELM)
            .cloudProviderType(CloudProviderType.GCP)
            .envId(environment.getUuid())
            .appId(owners.obtainApplication().getUuid())
            .build();

    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  private InfrastructureDefinition ensureGcpHelmCustomManifestInfraDef(Seed seed, Owners owners, String namespace) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.FUNCTIONAL_TEST);
      owners.add(environment);
    }

    final SettingAttribute gcpCloudProvider = settingGenerator.ensurePredefined(seed, owners, GCP_QA_TARGET);

    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .name("qa-target-helm-" + namespace)
            .infrastructure(GoogleKubernetesEngine.builder()
                                .cloudProviderId(gcpCloudProvider.getUuid())
                                .clusterName("us-central1-a/qa-target")
                                .namespace(namespace)
                                .build())
            .deploymentType(DeploymentType.HELM)
            .cloudProviderType(CloudProviderType.GCP)
            .envId(environment.getUuid())
            .appId(owners.obtainApplication().getUuid())
            .build();

    return ensureInfrastructureDefinition(infrastructureDefinition);
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
                                .subscriptionId(AZURE_SUBSCRIPTION_ID)
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

    final SettingAttribute ecsCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER);

    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .name("Ecs Ec2 type deployment Functional test")
            .infrastructure(AwsEcsInfrastructure.builder()
                                .cloudProviderId(ecsCloudProvider.getUuid())
                                .clusterName("SdkTesting")
                                .region("us-east-1")
                                .launchType("EC2")
                                .assignPublicIp(false)
                                .clusterName("Q-FUNCTIONAL-TESTS-DO-NOT-DELETE")
                                .build())
            .deploymentType(DeploymentType.ECS)
            .cloudProviderType(CloudProviderType.AWS)
            .scopedToServices(Collections.singletonList(service.getUuid()))
            .envId(environment.getUuid())
            .appId(owners.obtainApplication().getUuid())
            .build();

    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  private InfrastructureDefinition ensureEcsEc2DeploymentTest(Randomizer.Seed seed, Owners owners) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }

    final SettingAttribute ecsCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER);
    List<String> serviceIds = owners.obtainAllServiceIds();
    if (isEmpty(serviceIds)) {
      Service service = serviceGenerator.ensurePredefined(seed, owners, Services.ECS_TEST);
      owners.add(service);
      serviceIds = Collections.singletonList(service.getUuid());
    }

    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .name("Ecs Ec2 type deployment Functional test")
            .infrastructure(AwsEcsInfrastructure.builder()
                                .cloudProviderId(ecsCloudProvider.getUuid())
                                .region("us-east-1")
                                .launchType("EC2")
                                .assignPublicIp(false)
                                .clusterName("qa-test-cluster")
                                .build())
            .deploymentType(DeploymentType.ECS)
            .cloudProviderType(CloudProviderType.AWS)
            .scopedToServices(serviceIds)
            .envId(environment.getUuid())
            .appId(owners.obtainApplication().getUuid())
            .build();

    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  private InfrastructureDefinition ensureAwsLambda(Randomizer.Seed seed, Owners owners) {
    Environment environment = ensureEnv(seed, owners);
    final String region = "us-east-2";

    final SettingAttribute awsCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER);

    AwsLambdaInfrastructure awsLambdaInfrastructure = AwsLambdaInfrastructure.builder()
                                                          .cloudProviderId(awsCloudProvider.getUuid())
                                                          .region(region)
                                                          .role(GeneratorConstants.AWS_TEST_LAMBDA_ROLE)
                                                          .build();

    String name = "aws-lamda-infra";
    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(name)
                                                            .cloudProviderType(CloudProviderType.AWS)
                                                            .deploymentType(DeploymentType.AWS_LAMBDA)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .infrastructure(awsLambdaInfrastructure)
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
    final SettingAttribute gcpCloudProvider = settingGenerator.ensurePredefined(seed, owners, GCP_PLAYGROUND);

    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .name("gcp-harness-test-" + namespace)
            .infrastructure(GoogleKubernetesEngine.builder()
                                .cloudProviderId(gcpCloudProvider.getUuid())
                                .clusterName("us-central1-a/harness-test")
                                .namespace(namespace)
                                .build())
            .deploymentType(DeploymentType.KUBERNETES)
            .cloudProviderType(CloudProviderType.GCP)
            .envId(environment.getUuid())
            .appId(owners.obtainApplication().getUuid())
            .build();

    InfrastructureDefinition savedInfraDef = ensureInfrastructureDefinition(infrastructureDefinition);
    owners.add(savedInfraDef);
    return savedInfraDef;
  }

  private InfrastructureDefinition ensureOpenshiftTest(Randomizer.Seed seed, Owners owners, String application) {
    final SettingAttribute openshiftCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, OPENSHIFT_TEST_CLUSTER);
    final Environment environment =
        owners.obtainEnvironment(() -> environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST));

    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .name("openshift-fn-tests-" + application)
            .infrastructure(DirectKubernetesInfrastructure.builder()
                                .cloudProviderId(openshiftCloudProvider.getUuid())
                                .namespace(application)
                                .build())
            .deploymentType(DeploymentType.KUBERNETES)
            .cloudProviderType(CloudProviderType.KUBERNETES_CLUSTER)
            .envId(environment.getUuid())
            .appId(environment.getAppId())
            .build();

    owners.add(ensureInfrastructureDefinition(infrastructureDefinition));
    return owners.obtainInfrastructureDefinition();
  }

  public InfrastructureDefinition ensurePredefinedCustomDeployment(
      Randomizer.Seed seed, Owners owners, String templateUuid, String infrastructureDefinitionName) {
    Environment environment = owners.obtainEnvironment();
    if (environment == null) {
      environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
      owners.add(environment);
    }
    Service service = owners.obtainService();
    if (service == null) {
      service =
          serviceGenerator.ensurePredefinedCustomDeployment(seed, owners, templateUuid, "CustomDeployment Service");
      owners.add(service);
    }
    List<NameValuePair> infraVariables = new ArrayList<>();
    infraVariables.add(NameValuePair.builder().name("url").value("qa.harness.io").build());
    infraVariables.add(NameValuePair.builder().name("namespace").value(null).build());
    infraVariables.add(NameValuePair.builder().name("cluster").value("myCluster").build());
    infraVariables.add(NameValuePair.builder().name("username").value("user").build());

    return ensureInfrastructureDefinition(
        InfrastructureDefinition.builder()
            .name(infrastructureDefinitionName)
            .deploymentType(DeploymentType.CUSTOM)
            .cloudProviderType(CloudProviderType.CUSTOM)
            .envId(environment.getUuid())
            .appId(owners.obtainApplication().getUuid())
            .deploymentTypeTemplateId(templateUuid)
            .infrastructure(CustomInfrastructure.builder().infraVariables(infraVariables).build())
            .build());
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

  private InfrastructureDefinition ensureAwsSshFunctionalTestNAS(Randomizer.Seed seed, Owners owners) {
    return ensureAwsSshInfraDefinition(
        seed, owners, Environments.FUNCTIONAL_TEST, Services.NAS_FUNCTIONAL_TEST, "Aws non prod - ssh workflow test");
  }

  private InfrastructureDefinition ensureAwsWinrmFunctionalTest(Seed seed, Owners owners) {
    final List<Tag> tags = Collections.singletonList(Tag.builder().key("role").value("rollback-ft").build());
    return ensureAwsWinrmInfraDefinition(
        seed, owners, Environments.FUNCTIONAL_TEST, Services.WINDOWS_TEST, "Aws WinRM InfraDef", tags);
  }

  private InfrastructureDefinition ensureAwsWinrmDownloadTest(Seed seed, Owners owners) {
    final List<Tag> tags = Collections.singletonList(Tag.builder().key("role").value("download-artifact").build());
    return ensureAwsWinrmInfraDefinition(seed, owners, Environments.FUNCTIONAL_TEST, Services.WINDOWS_TEST_DOWNLOAD,
        "Aws WinRM Download InfraDef", tags);
  }

  private InfrastructureDefinition ensureAwsWinrmInfraDefinition(
      Seed seed, Owners owners, Environments environments, Services services, String name, List<Tag> tags) {
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

    final SettingAttribute awsTestSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, AWS_TEST_CLOUD_PROVIDER);
    final SettingAttribute windowsSettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, WINRM_DEV_TEST_CONNECTOR);

    return ensureInfrastructureDefinition(createWinRMInfraDefinition(
        owners, tags, awsTestSettingAttribute.getUuid(), windowsSettingAttribute.getUuid(), name));
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
        createInfraDefinition(owners, tags, awsTestSettingAttribute.getUuid(), devKeySettingAttribute.getUuid(), name));
  }

  @NotNull
  private InfrastructureDefinition createInfraDefinition(
      Owners owners, List<Tag> tags, String awsTestSettingAttributeId, String devKeySettingAttributeId, String name) {
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
        .appId(owners.obtainApplication().getUuid())
        .envId(owners.obtainEnvironment().getUuid())
        .infrastructure(awsInstanceInfrastructure)
        .build();
  }

  @NotNull
  private InfrastructureDefinition createWinRMInfraDefinition(
      Owners owners, List<Tag> tags, String awsTestSettingAttributeId, String devKeySettingAttributeId, String name) {
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
        .appId(owners.obtainApplication().getUuid())
        .envId(owners.obtainEnvironment().getUuid())
        .deploymentType(DeploymentType.WINRM)
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

    try {
      return infrastructureDefinitionService.save(infrastructureDefinition, false, true);
    } catch (InvalidRequestException ex) {
      existing = exists(infrastructureDefinition);
      if (existing != null) {
        return existing;
      }
      throw ex;
    }
  }

  private InfrastructureDefinition ensurePcf(Seed seed, Owners owners, String bearerToken) {
    Environment environment = ensureEnv(seed, owners);
    final SettingAttribute pcfCloudProvider = settingGenerator.ensurePredefined(seed, owners, Settings.PCF_CONNECTOR);

    PcfInfraStructure pcfInfraStructure = PcfInfraStructure.builder()
                                              .organization("harness")
                                              .space("Qa_Verification_workflow_space")
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
    final SettingAttribute awsCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER);

    AwsEcsInfrastructure awsEcsInfrastructure = AwsEcsInfrastructure.builder()
                                                    .region("us-east-1")
                                                    .clusterName("qa-test-cluster")
                                                    .cloudProviderId(awsCloudProvider.getUuid())
                                                    .launchType(LaunchType.EC2.toString())
                                                    .build();

    String name = "aws-ecs-infra";
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

  private InfrastructureDefinition ensureGcpK8s(Randomizer.Seed seed, Owners owners, String namespace) {
    Environment environment = ensureEnv(seed, owners);

    final SettingAttribute gcpK8sCloudProvider = settingGenerator.ensurePredefined(seed, owners, GCP_PLAYGROUND);

    String name = "gcp-k8s-infra";

    GoogleKubernetesEngine gcpK8sInfra = GoogleKubernetesEngine.builder()
                                             .cloudProviderId(gcpK8sCloudProvider.getUuid())
                                             .namespace(namespace)
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
            -> infrastructureDefinitionService.save(infrastructureDefinition, false, true),
        () -> exists(infrastructureDefinition));
  }

  private InfrastructureDefinition ensureAwsAmi(Randomizer.Seed seed, Owners owners, String bearerToken) {
    Environment environment = ensureEnv(seed, owners);
    final String region = "us-east-1";
    final String accountId = environment.getAccountId();
    final String appId = environment.getAppId();

    final SettingAttribute awsCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER);

    List<String> autoScalingGroups = InfrastructureDefinitionRestUtils.listAutoScalingGroups(
        bearerToken, accountId, appId, awsCloudProvider.getUuid(), region);

    assertThat(autoScalingGroups).isNotEmpty();

    return ensureAwsAmi(seed, owners, autoScalingGroups.get(0), "aws-ami-infradef");
  }
  private InfrastructureDefinition ensureAwsAmiWithLaunchTemplate(
      Randomizer.Seed seed, Owners owners, String bearerToken) {
    return ensureAwsAmi(seed, owners, "AMI-BASE-ASG-TODOLIST", "aws-ami-lt-infradef");
  }

  private InfrastructureDefinition ensureAzureVMSSBasicTest(Seed seed, Owners owners) {
    return ensureAzureVMSS(seed, owners, AZURE_VMSS_BASIC_INFRA_DEFINITION_NAME);
  }

  private InfrastructureDefinition ensureAzureVMSSBlueGreenTest(Seed seed, Owners owners) {
    return ensureAzureVMSS(seed, owners, AZURE_VMSS_BLUE_GREEN_INFRA_DEFINITION_NAME);
  }

  private InfrastructureDefinition ensureAzureVMSSAPITest(Seed seed, Owners owners) {
    return ensureAzureVMSS(seed, owners, AZURE_VMSS_API_INFRA_DEFINITION_NAME);
  }

  private InfrastructureDefinition ensureAzureWebAppAPITest(Seed seed, Owners owners) {
    return ensureAzureWebApp(seed, owners, AZURE_WEB_APP_API_INFRA_DEFINITION_NAME);
  }

  private InfrastructureDefinition ensureAzureWebAppBlueGreenTest(Seed seed, Owners owners) {
    return ensureAzureWebApp(seed, owners, AZURE_WEB_APP_BLUE_GREEN_INFRA_DEFINITION_NAME);
  }

  private InfrastructureDefinition ensureAzureWebAppBlueGreenRollbackTest(Seed seed, Owners owners) {
    return ensureAzureWebApp(seed, owners, AZURE_WEB_APP_BLUE_GREEN_ROLLBACK_INFRA_DEFINITION_NAME);
  }

  private InfrastructureDefinition ensureAzureWebAppCanaryTest(Seed seed, Owners owners) {
    return ensureAzureWebApp(seed, owners, AZURE_WEB_APP_CANARY_INFRA_DEFINITION_NAME);
  }

  private InfrastructureDefinition ensureAzureVMSS(Seed seed, Owners owners, String infraDefName) {
    Environment environment = ensureEnv(seed, owners);

    final SettingAttribute azureCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AZURE_TEST_CLOUD_PROVIDER);
    final SettingAttribute sshPublicKeySettingAttribute =
        settingGenerator.ensurePredefined(seed, owners, AZURE_VMSS_SSH_PUBLIC_KEY_CONNECTOR);

    AzureVMSSInfra azureVMSSInfra = AzureVMSSInfra.builder()
                                        .cloudProviderId(azureCloudProvider.getUuid())
                                        .baseVMSSName(AZURE_VMSS_BASE_SCALE_SET_NAME)
                                        .resourceGroupName(AZURE_FUNCTIONAL_TEST_RESOURCE_GROUP)
                                        .subscriptionId(AZURE_SUBSCRIPTION_ID)
                                        .userName(AZURE_VMSS_VM_USERNAME)
                                        .hostConnectionAttrs(sshPublicKeySettingAttribute.getName())
                                        .vmssAuthType(VMSSAuthType.SSH_PUBLIC_KEY)
                                        .vmssDeploymentType(VMSSDeploymentType.NATIVE_VMSS)
                                        .build();

    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(infraDefName)
                                                            .cloudProviderType(CloudProviderType.AZURE)
                                                            .deploymentType(DeploymentType.AZURE_VMSS)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .infrastructure(azureVMSSInfra)
                                                            .build();

    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  private InfrastructureDefinition ensureAzureWebApp(Seed seed, Owners owners, String infraDefName) {
    Environment environment = ensureEnv(seed, owners);

    final SettingAttribute azureCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AZURE_TEST_CLOUD_PROVIDER);

    AzureWebAppInfra azureWebAppInfra = AzureWebAppInfra.builder()
                                            .cloudProviderId(azureCloudProvider.getUuid())
                                            .resourceGroup(AZURE_FUNCTIONAL_TEST_APP_SERVICE_RESOURCE_GROUP)
                                            .subscriptionId(AZURE_SUBSCRIPTION_ID)
                                            .build();

    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .name(infraDefName)
                                                            .cloudProviderType(CloudProviderType.AZURE)
                                                            .deploymentType(DeploymentType.AZURE_WEBAPP)
                                                            .appId(environment.getAppId())
                                                            .envId(environment.getUuid())
                                                            .infrastructure(azureWebAppInfra)
                                                            .build();

    return ensureInfrastructureDefinition(infrastructureDefinition);
  }

  private InfrastructureDefinition ensureAwsAmi(
      Randomizer.Seed seed, Owners owners, String asgName, String infradefName) {
    Environment environment = ensureEnv(seed, owners);
    final String region = "us-east-1";

    final SettingAttribute awsCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER);
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

    String name = "aws-ami-spotinst-infra";
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
    final String region = "us-east-2";

    final SettingAttribute awsCloudProvider =
        settingGenerator.ensurePredefined(seed, owners, AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER);

    AwsLambdaInfrastructure awsLambdaInfrastructure = AwsLambdaInfrastructure.builder()
                                                          .cloudProviderId(awsCloudProvider.getUuid())
                                                          .region(region)
                                                          .role(GeneratorConstants.AWS_TEST_LAMBDA_ROLE)
                                                          .build();

    String name = "aws-lamda-infra";
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

    String name = "azure-winrm-infra";

    AzureInstanceInfrastructure azureInstanceInfrastructure =
        AzureInstanceInfrastructure.builder()
            .cloudProviderId(azureCloudProvider.getUuid())
            .winRmConnectionAttributes(winRmConnectionAttr.getUuid())
            .subscriptionId(AZURE_SUBSCRIPTION_ID)
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
