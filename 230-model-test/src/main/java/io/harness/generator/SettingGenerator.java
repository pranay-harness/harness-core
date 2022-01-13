/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static io.harness.generator.SettingGenerator.Settings.AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.AWS_SPOTINST_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.AWS_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.AZURE_VMSS_SSH_PUBLIC_KEY_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.DEV_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.GITHUB_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.HELM_GCS_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.PHYSICAL_DATA_CENTER;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AWS_EC2_USER;
import static io.harness.generator.constants.InfraDefinitionGeneratorConstants.AZURE_VMSS_VM_USERNAME;
import static io.harness.generator.constants.SettingsGeneratorConstants.PCF_END_POINT;
import static io.harness.generator.constants.SettingsGeneratorConstants.PCF_KEY;
import static io.harness.generator.constants.SettingsGeneratorConstants.PCF_USERNAME;
import static io.harness.govern.Switch.unhandled;
import static io.harness.shell.AccessType.KEY;
import static io.harness.testframework.framework.utils.SettingUtils.createEcsFunctionalTestGitAccountSetting;
import static io.harness.testframework.framework.utils.SettingUtils.createEcsFunctionalTestGitRepoSetting;
import static io.harness.testframework.framework.utils.SettingUtils.createGitHubRepoSetting;
import static io.harness.testframework.framework.utils.SettingUtils.createPCFFunctionalTestGitRepoSetting;
import static io.harness.testframework.framework.utils.SettingUtils.createTerraformCityGitRepoSetting;
import static io.harness.testframework.framework.utils.SettingUtils.createTerraformMainGitAcSetting;
import static io.harness.testframework.framework.utils.SettingUtils.createTerraformMainGitRepoSetting;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.CGConstants.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingAttribute.SettingCategory.AZURE_ARTIFACTS;
import static software.wings.beans.SettingAttribute.SettingCategory.CLOUD_PROVIDER;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;
import static software.wings.beans.SettingAttribute.SettingCategory.HELM_REPO;
import static software.wings.beans.SettingAttribute.SettingCategory.SETTING;
import static software.wings.utils.UsageRestrictionsUtils.getAllAppAllEnvUsageRestrictions;

import io.harness.generator.AccountGenerator.Accounts;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.k8s.model.KubernetesClusterAuthType;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.testframework.framework.utils.SettingUtils;

import software.wings.beans.Account;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.ElkConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.PcfConfig;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.ServiceNowConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.beans.settings.azureartifacts.AzureArtifactsPATConfig;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.analysis.ElkValidationType;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.EnumSet;

@Singleton
public class SettingGenerator {
  private static final String ADMIN = "admin";
  private static final String HARNESS_NEXUS = "Harness Nexus";
  private static final String HARNESS_NEXUS_2 = "Harness Nexus 2";
  private static final String HARNESS_JENKINS = "Harness Jenkins";
  private static final String HARNESS_JENKINS_DEV = "Harness Jenkins Dev";
  public static final String HARNESS_JIRA = "Harness Jira";
  private static final String SNOW_CONNECTOR = "Service Now Connector";
  private static final String HARNESS_NEXUS_THREE = "Harness Nexus 3";
  private static final String HARNESS_ARTIFACTORY = "Harness Artifactory";
  private static final String HARNESS_BAMBOO = "Harness Bamboo";
  private static final String HARNESS_DOCKER_REGISTRY = "Harness Docker Registry";
  private static final String GCP_PLAYGROUND = "playground-gke-gcs-gcr";
  private static final String GCP_QA_TARGET = "qa-target-gke-gcs-gcr";

  private static final String PCF_CONNECTOR = "Harness PCF";
  private static final String HARNESS_AZURE_ARTIFACTS = "Harness Azure Artifacts";
  private static final String ELK_CONNECTOR = "Elk Connector";
  private static final String HARNESS_ACCOUNT_GIT_CONNECTOR = "Harness Account Git Connector";
  private static final String HARNESS_SSH_GIT_CONNECTOR = "Harness Ssh Git Connector with 7999 port";
  private static final String HARNESS_SSH_GIT_CONNECTOR_KEY = "Harness Ssh Git Connector with 7999 port key";
  private static final String OPENSHIFT_TEST_CLUSTER = "Openshift Test Cluster";

  private static final String HELM_CHART_REPO_URL = "http://storage.googleapis.com/kubernetes-charts/";
  private static final String HELM_CHART_REPO = "Helm Chart Repo";
  private static final String HELM_SOURCE_REPO_URL = "https://github.com/helm/charts.git";
  private static final String HELM_SOURCE_REPO = "Helm Source Repo";
  private static final String HELM_S3_BUCKET = "deployment-functional-tests-charts";
  private static final String HELM_GCS_BUCKET = "deployment-functional-tests-charts";
  private static final String HELM_S3 = "HELM S3";
  private static final String REGION_US_EAST_1 = "us-east-1";
  private static final String HARNESS_ADMIN = "harnessadmin";
  private static final String OPENSHIFT_TEST_CLUSTER_MASTER_URL = "https://35.209.119.47:8443";

  @Inject AccountGenerator accountGenerator;
  @Inject ScmSecret scmSecret;
  @Inject SecretGenerator secretGenerator;

  @Inject SettingsService settingsService;
  @Inject WingsPersistence wingsPersistence;
  @Inject private SecretManager secretManager;

  public enum Settings {
    AWS_TEST_CLOUD_PROVIDER,
    AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER,
    AZURE_TEST_CLOUD_PROVIDER,
    AZURE_ARM_TEST_CLOUD_PROVIDER,
    AWS_SPOTINST_TEST_CLOUD_PROVIDER,
    SPOTINST_TEST_CLOUD_PROVIDER,
    DEV_TEST_CONNECTOR,
    WINRM_DEV_TEST_CONNECTOR,
    HARNESS_JENKINS_CONNECTOR,
    HARNESS_JENKINS_DEV_CONNECTOR,
    GITHUB_TEST_CONNECTOR,
    TERRAFORM_CITY_GIT_REPO,
    TERRAFORM_MAIN_GIT_REPO,
    TERRAFORM_MAIN_GIT_AC,
    HARNESS_BAMBOO_CONNECTOR,
    HARNESS_NEXUS_CONNECTOR,
    HARNESS_NEXUS2_CONNECTOR,
    HARNESS_NEXU3_CONNECTOR,
    HARNESS_ARTIFACTORY_CONNECTOR,
    HARNESS_DOCKER_REGISTRY,
    GCP_PLAYGROUND,
    GCP_QA_TARGET,
    HARNESS_JIRA,
    SERVICENOW_CONNECTOR,
    PHYSICAL_DATA_CENTER,
    WINRM_TEST_CONNECTOR,
    PAID_EMAIL_SMTP_CONNECTOR,
    HELM_CHART_REPO_CONNECTOR,
    HELM_SOURCE_REPO_CONNECTOR,
    PCF_CONNECTOR,
    AZURE_ARTIFACTS_CONNECTOR,
    PCF_FUNCTIONAL_TEST_GIT_REPO,
    ECS_FUNCTIONAL_TEST_GIT_REPO,
    ECS_FUNCTIONAL_TEST_GIT_ACCOUNT,
    HELM_S3_CONNECTOR,
    HELM_GCS_CONNECTOR,
    ELK,
    ACCOUNT_LEVEL_GIT_CONNECTOR,
    AZURE_VMSS_SSH_PUBLIC_KEY_CONNECTOR,
    OPENSHIFT_TEST_CLUSTER,
    SSH_GIT_CONNECTOR_WITH_7999_PORT,
  }

  public void ensureAllPredefined(Randomizer.Seed seed, Owners owners) {
    EnumSet.allOf(Settings.class).forEach(predefined -> ensurePredefined(seed, owners, predefined));
  }

  public SettingAttribute ensurePredefined(Randomizer.Seed seed, Owners owners, Settings predefined) {
    switch (predefined) {
      case AWS_TEST_CLOUD_PROVIDER:
        return ensureAwsTest(seed, owners);
      case AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER:
        return ensureAwsDeploymentFunctionalTestsCloudProvider(seed, owners);
      case AZURE_TEST_CLOUD_PROVIDER:
        return ensureAzureTestCloudProvider(seed, owners);
      case AZURE_ARM_TEST_CLOUD_PROVIDER:
        return ensureAzureARMTestCloudProvider(seed, owners);
      case AWS_SPOTINST_TEST_CLOUD_PROVIDER:
        return ensureAwsSpotinstTest(seed, owners);
      case SPOTINST_TEST_CLOUD_PROVIDER:
        return ensureSpotinstTestCloudProvider(seed, owners);
      case DEV_TEST_CONNECTOR:
        return ensureDevTest(seed, owners);
      case WINRM_DEV_TEST_CONNECTOR:
        return ensureWinrmDevTest(seed, owners);
      case HARNESS_JENKINS_CONNECTOR:
        return ensureHarnessJenkins(seed, owners);
      case HARNESS_JENKINS_DEV_CONNECTOR:
        return ensureHarnessJenkinsDev(seed, owners);
      case GITHUB_TEST_CONNECTOR:
        return ensureGithubTest(seed, owners);
      case TERRAFORM_CITY_GIT_REPO:
        return ensureTerraformCityGitRepo(seed, owners);
      case HARNESS_BAMBOO_CONNECTOR:
        return ensureHarnessBamboo(seed, owners);
      case HARNESS_NEXUS_CONNECTOR:
        return ensureHarnessNexus(seed, owners);
      case HARNESS_NEXUS2_CONNECTOR:
        return ensureHarnessNexus2(seed, owners);
      case HARNESS_NEXU3_CONNECTOR:
        return ensureHarnessNexus3(seed, owners);
      case HARNESS_ARTIFACTORY_CONNECTOR:
        return ensureHarnessArtifactory(seed, owners);
      case HARNESS_DOCKER_REGISTRY:
        return ensureHarnessDocker(seed, owners);
      case GCP_PLAYGROUND:
        return ensureGcpPlayground(seed, owners);
      case GCP_QA_TARGET:
        return ensureGcpQaTarget(seed, owners);
      case HARNESS_JIRA:
        return ensureHarnessJira(seed, owners);
      case SERVICENOW_CONNECTOR:
        return ensureServiceNowConnector(seed, owners);
      case PHYSICAL_DATA_CENTER:
        return ensurePhysicalDataCenter(seed, owners);
      case WINRM_TEST_CONNECTOR:
        return ensureWinRmTestConnector(seed, owners);
      case TERRAFORM_MAIN_GIT_REPO:
        return ensureTerraformMainGitRepo(seed, owners);
      case TERRAFORM_MAIN_GIT_AC:
        return ensureTerraformMainGitAc(seed, owners);
      case PAID_EMAIL_SMTP_CONNECTOR:
        return ensurePaidSMTPSettings(seed, owners);
      case HELM_CHART_REPO_CONNECTOR:
        return ensureHelmChartRepoSetting(seed, owners);
      case HELM_SOURCE_REPO_CONNECTOR:
        return ensureHelmSourceRepoSetting(seed, owners);
      case PCF_CONNECTOR:
        return ensurePcfConnector(seed, owners);
      case AZURE_ARTIFACTS_CONNECTOR:
        return ensureAzureArtifactsSetting(seed, owners);
      case PCF_FUNCTIONAL_TEST_GIT_REPO:
        return ensurePCFGitRepo(seed, owners);
      case ECS_FUNCTIONAL_TEST_GIT_REPO:
        return ensureEcsGitRepo(seed, owners);
      case ECS_FUNCTIONAL_TEST_GIT_ACCOUNT:
        return ensureEcsGitAccount(seed, owners);
      case HELM_S3_CONNECTOR:
        return ensureHelmS3Connector(seed, owners);
      case HELM_GCS_CONNECTOR:
        return ensureHelmGCSConnector(seed, owners);
      case ELK:
        return ensureElkConnector(seed, owners);
      case ACCOUNT_LEVEL_GIT_CONNECTOR:
        return ensureAccountLevelGitConnector(seed, owners);
      case AZURE_VMSS_SSH_PUBLIC_KEY_CONNECTOR:
        return ensureAzureVMSSSSHPublicKey(seed, owners);
      case OPENSHIFT_TEST_CLUSTER:
        return ensureOpenshiftTestCluster(seed, owners);
      case SSH_GIT_CONNECTOR_WITH_7999_PORT:
        return ensureSshGitConnector(seed, owners);
      default:
        unhandled(predefined);
    }
    return null;
  }

  private SettingAttribute ensureHelmS3Connector(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute awsCloudProvider = ensurePredefined(seed, owners, AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER);

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withName(HELM_S3)
                                            .withAccountId(account.getUuid())
                                            .withCategory(HELM_REPO)
                                            .withAccountId(account.getUuid())
                                            .withValue(AmazonS3HelmRepoConfig.builder()
                                                           .accountId(account.getUuid())
                                                           .connectorId(awsCloudProvider.getUuid())
                                                           .bucketName(HELM_S3_BUCKET)
                                                           .region(REGION_US_EAST_1)
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();

    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureHelmGCSConnector(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute gcpCloudProvider = ensurePredefined(seed, owners, Settings.GCP_PLAYGROUND);

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withName(HELM_GCS_CONNECTOR.name())
                                            .withAccountId(account.getUuid())
                                            .withCategory(HELM_REPO)
                                            .withAccountId(account.getUuid())
                                            .withValue(GCSHelmRepoConfig.builder()
                                                           .accountId(account.getUuid())
                                                           .connectorId(gcpCloudProvider.getUuid())
                                                           .bucketName(HELM_GCS_BUCKET)
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();

    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureElkConnector(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withName(ELK_CONNECTOR)
            .withCategory(CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(ElkConfig.builder()
                           .accountId(account.getUuid())
                           .elkConnector(ElkConnector.ELASTIC_SEARCH_SERVER)
                           .elkUrl("http://ec2-34-227-84-170.compute-1.amazonaws.com:9200/")
                           .username("admin@harness.io")
                           .encryptedPassword("uCgt5xOJRT2HuyQkXQgOaA")
                           .validationType(ElkValidationType.PASSWORD)
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensurePcfConnector(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password = secretGenerator.ensureStored(owners, SecretName.builder().value(PCF_KEY).build());
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withName(PCF_CONNECTOR)
                                            .withCategory(CONNECTOR)
                                            .withAccountId(account.getUuid())
                                            .withValue(PcfConfig.builder()
                                                           .accountId(account.getUuid())
                                                           .username(PCF_USERNAME.toCharArray())
                                                           .endpointUrl(PCF_END_POINT)
                                                           .password(password.toCharArray())
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureServiceNowConnector(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password = secretGenerator.ensureStored(owners, SecretName.builder().value("snow_connector").build());

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withName(SNOW_CONNECTOR)
                                            .withCategory(CONNECTOR)
                                            .withAccountId(account.getUuid())
                                            .withValue(ServiceNowConfig.builder()
                                                           .accountId(account.getUuid())
                                                           .baseUrl("https://ven03171.service-now.com")
                                                           .username(ADMIN)
                                                           .password(password.toCharArray())
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureTerraformMainGitRepo(Seed seed, Owners owners) {
    SettingAttribute githubKey = ensurePredefined(seed, owners, GITHUB_TEST_CONNECTOR);

    final String password =
        secretGenerator.ensureStored(owners, SecretName.builder().value("terraform_password").build());
    SettingAttribute settingAttribute = createTerraformMainGitRepoSetting(githubKey, password.toCharArray());
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureTerraformMainGitAc(Seed seed, Owners owners) {
    SettingAttribute githubKey = ensurePredefined(seed, owners, GITHUB_TEST_CONNECTOR);

    final String password =
        secretGenerator.ensureStored(owners, SecretName.builder().value("terraform_password").build());
    SettingAttribute settingAttribute = createTerraformMainGitAcSetting(githubKey, password.toCharArray());
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute ensureAzureTestCloudProvider(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String azure_key = secretGenerator.ensureStored(owners, SecretName.builder().value("azure_key").build());

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CLOUD_PROVIDER)
            .withName("Test Azure Cloud Provider")
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(account.getUuid())
            .withValue(AzureConfig.builder()
                           .accountId(account.getUuid())
                           .clientId(scmSecret.decryptToString(new SecretName("azure_client_id")))
                           .tenantId(scmSecret.decryptToString(new SecretName("azure_tenant_id")))
                           .key(azure_key.toCharArray())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute ensureAzureARMTestCloudProvider(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String azure_key = secretGenerator.ensureStored(owners, SecretName.builder().value("azure_arm_key").build());

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CLOUD_PROVIDER)
            .withName("Test ARM Azure Cloud Provider")
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(account.getUuid())
            .withValue(AzureConfig.builder()
                           .accountId(account.getUuid())
                           .clientId(scmSecret.decryptToString(new SecretName("azure_arm_client_id")))
                           .tenantId(scmSecret.decryptToString(new SecretName("azure_arm_tenant_id")))
                           .key(azure_key.toCharArray())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureSpotinstTestCloudProvider(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String spotInstToken = secretGenerator.ensureStored(owners, SecretName.builder().value("secret_key").build());
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(CLOUD_PROVIDER)
                                            .withName("Test Spotinst Cloud Provider")
                                            .withAppId(GLOBAL_APP_ID)
                                            .withEnvId(GLOBAL_ENV_ID)
                                            .withAccountId(account.getUuid())
                                            .withValue(SpotInstConfig.builder()
                                                           .accountId(account.getUuid())
                                                           .spotInstAccountId("act-d48a11cf")
                                                           .encryptedSpotInstToken(spotInstToken)
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute ensureWinRmTestConnector(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password =
        secretGenerator.ensureStored(owners, SecretName.builder().value("user_admin_password").build());
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(SETTING)
                                            .withName("Test WinRM Connection")
                                            .withAppId(GLOBAL_APP_ID)
                                            .withEnvId(GLOBAL_ENV_ID)
                                            .withAccountId(account.getUuid())
                                            .withValue(WinRmConnectionAttributes.builder()
                                                           .accountId(account.getUuid())
                                                           .authenticationScheme(AuthenticationScheme.NTLM)
                                                           .username("harnessadmin")
                                                           .password(password.toCharArray())
                                                           .port(5986)
                                                           .useSSL(true)
                                                           .skipCertChecks(true)
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute ensurePhysicalDataCenter(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(CLOUD_PROVIDER)
                                            .withName(PHYSICAL_DATA_CENTER.name())
                                            .withAppId(GLOBAL_APP_ID)
                                            .withEnvId(GLOBAL_ENV_ID)
                                            .withAccountId(account.getUuid())
                                            .withValue(PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig()
                                                           .withType(SettingVariableTypes.PHYSICAL_DATA_CENTER.name())
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute ensureAwsTest(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String secretKey =
        secretGenerator.ensureStored(owners, SecretName.builder().value("aws_qa_setup_secret_key").build());
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CLOUD_PROVIDER)
            .withName(AWS_TEST_CLOUD_PROVIDER.name())
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(account.getUuid())
            .withValue(AwsConfig.builder()
                           .accessKey(scmSecret.decryptToCharArray(new SecretName("aws_qa_setup_access_key")))
                           .secretKey(secretKey.toCharArray())
                           .accountId(account.getUuid())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureAwsSpotinstTest(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String secretKey =
        secretGenerator.ensureStored(owners, SecretName.builder().value("aws_playground_secret_key").build());
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CLOUD_PROVIDER)
            .withName(AWS_SPOTINST_TEST_CLOUD_PROVIDER.name())
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(account.getUuid())
            .withValue(AwsConfig.builder()
                           .accessKey(/*scmSecret.decryptToString(new SecretName("aws_playground_access_key")*/
                               "AKIA4GYQC5QTQPI2UHMY".toCharArray())
                           .secretKey(/*scmSecret.decryptToCharArray(new SecretName("aws_playground_secret_key")*/
                               secretKey.toCharArray())
                           .accountId(account.getUuid())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute ensureAwsDeploymentFunctionalTestsCloudProvider(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String secretKey =
        secretGenerator.ensureStored(owners, SecretName.builder().value("aws_qa_setup_secret_key").build());
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CLOUD_PROVIDER)
            .withName(AWS_DEPLOYMENT_FUNCTIONAL_TESTS_CLOUD_PROVIDER.name())
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(account.getUuid())
            .withValue(AwsConfig.builder()
                           .accessKey(scmSecret.decryptToCharArray(new SecretName("aws_qa_setup_access_key")))
                           .secretKey(secretKey.toCharArray())
                           .accountId(account.getUuid())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureDevTest(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensureRandom(seed, owners);
    final String secretId = secretGenerator.ensureStored(account.getUuid(), new SecretName("ubuntu_private_key"));
    final SettingAttribute settingAttribute = aSettingAttribute()
                                                  .withCategory(SETTING)
                                                  .withAccountId(account.getUuid())
                                                  .withAppId(GLOBAL_APP_ID)
                                                  .withEnvId(GLOBAL_ENV_ID)
                                                  .withName(DEV_TEST_CONNECTOR.name())
                                                  .withValue(aHostConnectionAttributes()
                                                                 .withConnectionType(SSH)
                                                                 .withAccessType(KEY)
                                                                 .withAccountId(account.getUuid())
                                                                 .withUserName("ubuntu")
                                                                 .withKey(secretId.toCharArray())
                                                                 .build())
                                                  .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                                  .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureWinrmDevTest(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password =
        secretGenerator.ensureStored(owners, SecretName.builder().value("user_admin_password").build());
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(SETTING)
                                            .withName("Test Aws WinRM Connection")
                                            .withAppId(GLOBAL_APP_ID)
                                            .withEnvId(GLOBAL_ENV_ID)
                                            .withAccountId(account.getUuid())
                                            .withValue(WinRmConnectionAttributes.builder()
                                                           .accountId(account.getUuid())
                                                           .authenticationScheme(AuthenticationScheme.NTLM)
                                                           .username("Administrator")
                                                           .password(password.toCharArray())
                                                           .port(5986)
                                                           .useSSL(true)
                                                           .skipCertChecks(true)
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureGithubTest(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);

    final String key =
        secretGenerator.ensureStored(owners, SecretName.builder().value("playground_private_key").build());
    final SettingAttribute settingAttribute = createGitHubRepoSetting(account.getUuid(), key.toCharArray());
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureTerraformCityGitRepo(Randomizer.Seed seed, Owners owners) {
    SettingAttribute githubKey = ensurePredefined(seed, owners, GITHUB_TEST_CONNECTOR);

    final String password =
        secretGenerator.ensureStored(owners, SecretName.builder().value("terraform_password").build());
    SettingAttribute settingAttribute = createTerraformCityGitRepoSetting(githubKey, password.toCharArray());
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensurePCFGitRepo(Randomizer.Seed seed, Owners owners) {
    SettingAttribute githubKey = ensurePredefined(seed, owners, GITHUB_TEST_CONNECTOR);

    final String password =
        secretGenerator.ensureStored(owners, SecretName.builder().value("terraform_password").build());
    SettingAttribute settingAttribute = createPCFFunctionalTestGitRepoSetting(githubKey, password.toCharArray());
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute ensureEcsGitRepo(Randomizer.Seed seed, Owners owners) {
    SettingAttribute githubKey = ensurePredefined(seed, owners, GITHUB_TEST_CONNECTOR);
    final String password =
        secretGenerator.ensureStored(owners, SecretName.builder().value("git_automation_password").build());
    SettingAttribute settingAttribute = createEcsFunctionalTestGitRepoSetting(githubKey, password.toCharArray());
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureEcsGitAccount(Randomizer.Seed seed, Owners owners) {
    SettingAttribute githubKey = ensurePredefined(seed, owners, GITHUB_TEST_CONNECTOR);
    SettingAttribute settingAttribute = createEcsFunctionalTestGitAccountSetting(githubKey);
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureHarnessJenkins(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password = secretGenerator.ensureStored(owners, SecretName.builder().value("harness_jenkins").build());

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withName(HARNESS_JENKINS)
                                            .withCategory(CONNECTOR)
                                            .withAccountId(account.getUuid())
                                            .withValue(JenkinsConfig.builder()
                                                           .accountId(account.getUuid())
                                                           .jenkinsUrl("https://jenkinsint.harness.io")
                                                           .username("wingsbuild")
                                                           .password(password.toCharArray())
                                                           .authMechanism(JenkinsConfig.USERNAME_DEFAULT_TEXT)
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureHarnessJenkinsDev(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password =
        secretGenerator.ensureStored(owners, SecretName.builder().value("harness_jenkins_dev").build());

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withName(HARNESS_JENKINS_DEV)
                                            .withCategory(CONNECTOR)
                                            .withAccountId(account.getUuid())
                                            .withValue(JenkinsConfig.builder()
                                                           .accountId(account.getUuid())
                                                           .jenkinsUrl("https://jenkins.dev.harness.io")
                                                           .username("harnessadmin")
                                                           .password(password.toCharArray())
                                                           .authMechanism(JenkinsConfig.USERNAME_DEFAULT_TEXT)
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute ensureHarnessJira(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    owners.add(account);
    final String password = secretGenerator.ensureStored(owners, SecretName.builder().value("harness_jira").build());
    SettingAttribute settingAttribute =
        SettingUtils.createHarnessJIRASetting(account.getUuid(), password.toCharArray());
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureHarnessBamboo(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password = secretGenerator.ensureStored(owners, SecretName.builder().value("harness_bamboo").build());
    SettingAttribute bambooSettingAttribute = aSettingAttribute()
                                                  .withName(HARNESS_BAMBOO)
                                                  .withCategory(CONNECTOR)
                                                  .withAccountId(account.getUuid())
                                                  .withValue(BambooConfig.builder()
                                                                 .accountId(account.getUuid())
                                                                 .bambooUrl("http://cdteam-bamboo.harness.io:8085/")
                                                                 .username("wingsbuild")
                                                                 .password(password.toCharArray())
                                                                 .build())
                                                  .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                                  .build();
    return ensureSettingAttribute(seed, bambooSettingAttribute, owners);
  }

  private SettingAttribute ensureHarnessNexus(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password = secretGenerator.ensureStored(owners, SecretName.builder().value("harness_nexus").build());
    SettingAttribute nexusSettingAttribute = aSettingAttribute()
                                                 .withName(HARNESS_NEXUS)
                                                 .withCategory(CONNECTOR)
                                                 .withAccountId(account.getUuid())
                                                 .withValue(NexusConfig.builder()
                                                                .accountId(account.getUuid())
                                                                .nexusUrl("https://nexus2.harness.io")
                                                                .username(ADMIN)
                                                                .password(password.toCharArray())
                                                                .build())
                                                 .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                                 .build();
    return ensureSettingAttribute(seed, nexusSettingAttribute, owners);
  }

  private SettingAttribute ensureHarnessNexus2(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password =
        secretGenerator.ensureStored(owners, SecretName.builder().value("harness_admin_nexus").build());
    SettingAttribute nexusSettingAttribute = aSettingAttribute()
                                                 .withName(HARNESS_NEXUS_2)
                                                 .withCategory(CONNECTOR)
                                                 .withAccountId(account.getUuid())
                                                 .withValue(NexusConfig.builder()
                                                                .accountId(account.getUuid())
                                                                .nexusUrl("https://nexus2.dev.harness.io")
                                                                .username(HARNESS_ADMIN)
                                                                .password(password.toCharArray())
                                                                .build())
                                                 .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                                 .build();
    return ensureSettingAttribute(seed, nexusSettingAttribute, owners);
  }

  private SettingAttribute ensureHarnessNexus3(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password = secretGenerator.ensureStored(owners, SecretName.builder().value("harness_nexus").build());
    SettingAttribute nexus3SettingAttribute = aSettingAttribute()
                                                  .withName(HARNESS_NEXUS_THREE)
                                                  .withCategory(CONNECTOR)
                                                  .withAccountId(account.getUuid())
                                                  .withValue(NexusConfig.builder()
                                                                 .accountId(account.getUuid())
                                                                 .nexusUrl("https://nexus3.harness.io")
                                                                 .username(ADMIN)
                                                                 .password(password.toCharArray())
                                                                 .version("3.x")
                                                                 .build())
                                                  .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                                  .build();
    return ensureSettingAttribute(seed, nexus3SettingAttribute, owners);
  }

  private SettingAttribute ensureHarnessArtifactory(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password =
        secretGenerator.ensureStored(owners, SecretName.builder().value("harness_artifactory").build());
    SettingAttribute artifactorySettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_ARTIFACTORY)
            .withCategory(CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(ArtifactoryConfig.builder()
                           .accountId(account.getUuid())
                           .artifactoryUrl("https://harness.jfrog.io/harness")
                           .username(ADMIN)
                           .password(password.toCharArray())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, artifactorySettingAttribute, owners);
  }

  private SettingAttribute ensureHarnessDocker(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute dockerSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_DOCKER_REGISTRY)
            .withCategory(CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(DockerConfig.builder()
                           .accountId(account.getUuid())
                           .dockerRegistryUrl("https://registry.hub.docker.com/v2/")
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, dockerSettingAttribute, owners);
  }

  private SettingAttribute ensureGcpPlayground(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String gcpPlaygroundKey =
        secretGenerator.ensureStored(owners, SecretName.builder().value("gcp_playground").build());
    SettingAttribute gcpSettingAttribute =
        aSettingAttribute()
            .withName(GCP_PLAYGROUND)
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withAccountId(account.getUuid())
            .withValue(GcpConfig.builder()
                           .serviceAccountKeyFileContent(gcpPlaygroundKey.toCharArray())
                           .accountId(account.getUuid())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, gcpSettingAttribute, owners);
  }

  private SettingAttribute ensureGcpQaTarget(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String gcpPlaygroundKey =
        secretGenerator.ensureStored(owners, SecretName.builder().value("gcp_qa_target_sa_key").build());
    SettingAttribute gcpSettingAttribute =
        aSettingAttribute()
            .withName(GCP_QA_TARGET)
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withAccountId(account.getUuid())
            .withValue(GcpConfig.builder()
                           .serviceAccountKeyFileContent(gcpPlaygroundKey.toCharArray())
                           .accountId(account.getUuid())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, gcpSettingAttribute, owners);
  }

  private SettingAttribute ensurePaidSMTPSettings(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password =
        secretGenerator.ensureStored(owners, SecretName.builder().value("smtp_paid_sendgrid_config_password").build());
    SmtpConfig smtpConfig = SmtpConfig.builder()
                                .host("smtp.sendgrid.net")
                                .port(465)
                                .useSSL(true)
                                .fromAddress("automation@harness.io")
                                .username("apikey")
                                .password(password.toCharArray())
                                .accountId(account.getUuid())
                                .build();

    SettingAttribute emailSettingAttribute = aSettingAttribute()
                                                 .withCategory(CONNECTOR)
                                                 .withName("EMAIL")
                                                 .withAccountId(account.getUuid())
                                                 .withValue(smtpConfig)
                                                 .build();
    return ensureSettingAttribute(seed, emailSettingAttribute, owners);
  }

  private SettingAttribute ensureHelmChartRepoSetting(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);

    HttpHelmRepoConfig httpHelmRepoConfig =
        HttpHelmRepoConfig.builder().accountId(account.getUuid()).chartRepoUrl(HELM_CHART_REPO_URL).build();

    SettingAttribute helmRepoSettingAttribute = aSettingAttribute()
                                                    .withCategory(SettingCategory.HELM_REPO)
                                                    .withName(HELM_CHART_REPO)
                                                    .withAccountId(account.getUuid())
                                                    .withValue(httpHelmRepoConfig)
                                                    .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                                    .build();

    return ensureSettingAttribute(seed, helmRepoSettingAttribute, owners);
  }

  private SettingAttribute ensureHelmSourceRepoSetting(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);

    GitConfig gitConfig =
        GitConfig.builder().repoUrl(HELM_SOURCE_REPO_URL).branch("master").accountId(account.getUuid()).build();

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(CONNECTOR)
                                            .withName(HELM_SOURCE_REPO)
                                            .withAccountId(account.getUuid())
                                            .withValue(gitConfig)
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureAzureArtifactsSetting(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password = secretGenerator.ensureStored(account.getUuid(), new SecretName("harness_azure_devops_pat"));

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withName(HARNESS_AZURE_ARTIFACTS)
                                            .withCategory(AZURE_ARTIFACTS)
                                            .withAccountId(account.getUuid())
                                            .withValue(AzureArtifactsPATConfig.builder()
                                                           .accountId(account.getUuid())
                                                           .azureDevopsUrl("https://dev.azure.com/garvit-test")
                                                           .pat(password.toCharArray())
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureAccountLevelGitConnector(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String password =
        secretGenerator.ensureStored(owners, SecretName.builder().value("git_automation_password").build());

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CONNECTOR)
            .withName(HARNESS_ACCOUNT_GIT_CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(GitConfig.builder()
                           .accountId(account.getUuid())
                           .repoUrl("https://github.com/wings-software/")
                           .urlType(GitConfig.UrlType.ACCOUNT)
                           .generateWebhookUrl(true)
                           .authenticationScheme(io.harness.shell.AuthenticationScheme.HTTP_PASSWORD)
                           .username(String.valueOf(
                               new ScmSecret().decryptToCharArray(new SecretName("git_automation_username"))))
                           .password(password.toCharArray())
                           .build())
            .build();

    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureSshGitConnector(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    secretGenerator.ensureStoredFile(owners, SecretName.builder().value("cdp_test_pem_key").build());

    SettingAttribute sshKey =
        aSettingAttribute()
            .withCategory(SETTING)
            .withAccountId(account.getUuid())
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withName(HARNESS_SSH_GIT_CONNECTOR_KEY)
            .withValue(
                aHostConnectionAttributes()
                    .withConnectionType(SSH)
                    .withAccessType(KEY)
                    .withAccountId(account.getUuid())
                    .withUserName(AWS_EC2_USER)
                    .withSshPort(22)
                    .withAuthenticationScheme(io.harness.shell.AuthenticationScheme.SSH_KEY)
                    .withKey(
                        secretManager.getSecretByName(account.getUuid(), "cdp_test_pem_key").getUuid().toCharArray())
                    .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();

    sshKey = ensureSettingAttribute(seed, sshKey, owners);

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CONNECTOR)
            .withName(HARNESS_SSH_GIT_CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(GitConfig.builder()
                           .accountId(account.getUuid())
                           .generateWebhookUrl(false)
                           .keyAuth(true)
                           .repoUrl("ssh://ec2-user@52.201.247.140:7999/home/ec2-user/harness-repo.git")
                           .urlType(GitConfig.UrlType.REPO)
                           .generateWebhookUrl(true)
                           .sshSettingId(sshKey.getUuid())
                           .build())
            .build();

    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureAzureVMSSSSHPublicKey(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensureRandom(seed, owners);
    final String secretId =
        secretGenerator.ensureStored(account.getUuid(), new SecretName("azure_new_vm_ssh_public_key"));
    final SettingAttribute settingAttribute = aSettingAttribute()
                                                  .withCategory(SETTING)
                                                  .withAccountId(account.getUuid())
                                                  .withAppId(GLOBAL_APP_ID)
                                                  .withEnvId(GLOBAL_ENV_ID)
                                                  .withName(AZURE_VMSS_SSH_PUBLIC_KEY_CONNECTOR.name())
                                                  .withValue(aHostConnectionAttributes()
                                                                 .withConnectionType(SSH)
                                                                 .withAccessType(KEY)
                                                                 .withAccountId(account.getUuid())
                                                                 .withUserName(AZURE_VMSS_VM_USERNAME)
                                                                 .withKey(secretId.toCharArray())
                                                                 .build())
                                                  .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                                  .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureOpenshiftTestCluster(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    final String openshiftTestClusterSaToken =
        secretGenerator.ensureStored(owners, SecretName.builder().value("openshift_test_cluster_sa_token").build());

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CLOUD_PROVIDER)
            .withName(OPENSHIFT_TEST_CLUSTER)
            .withAccountId(account.getUuid())
            .withValue(KubernetesClusterConfig.builder()
                           .masterUrl(OPENSHIFT_TEST_CLUSTER_MASTER_URL)
                           .authType(KubernetesClusterAuthType.SERVICE_ACCOUNT)
                           .serviceAccountToken(openshiftTestClusterSaToken.toCharArray())
                           .accountId(account.getUuid())
                           .build())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute exists(SettingAttribute settingAttribute) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .filter(SettingAttributeKeys.accountId, settingAttribute.getAccountId())
        .filter(SettingAttributeKeys.appId, settingAttribute.getAppId())
        .filter(SettingAttributeKeys.envId, settingAttribute.getEnvId())
        .filter(SettingAttributeKeys.category, settingAttribute.getCategory())
        .filter(SettingAttributeKeys.name, settingAttribute.getName())
        .get();
  }

  public SettingAttribute ensureSettingAttribute(Seed seed, SettingAttribute settingAttribute, Owners owners) {
    SettingAttribute.Builder builder = aSettingAttribute();

    if (settingAttribute != null && settingAttribute.getAccountId() != null) {
      builder.withAccountId(settingAttribute.getAccountId());
    } else {
      throw new UnsupportedOperationException();
    }

    builder.withAppId(settingAttribute.getAppId());

    builder.withEnvId(settingAttribute.getEnvId());

    builder.withCategory(settingAttribute.getCategory());

    builder.withName(settingAttribute.getName());

    builder.withUsageRestrictions(settingAttribute.getUsageRestrictions());

    if (settingAttribute.getCreatedBy() != null) {
      builder.withCreatedBy(settingAttribute.getCreatedBy());
    } else {
      builder.withCreatedBy(owners.obtainUser());
    }

    SettingAttribute existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }

    builder.withValue(settingAttribute.getValue());

    final SettingAttribute finalSettingAttribute = builder.build();

    return GeneratorUtils.suppressDuplicateException(
        () -> settingsService.forceSave(finalSettingAttribute), () -> exists(finalSettingAttribute));
  }
}
