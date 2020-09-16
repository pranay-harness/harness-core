package io.harness.generator;

import static io.harness.generator.SettingGenerator.Settings.AWS_SPOTINST_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.AWS_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.DEV_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.GITHUB_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.PHYSICAL_DATA_CENTER;
import static io.harness.generator.constants.SettingsGeneratorConstants.PCF_END_POINT;
import static io.harness.generator.constants.SettingsGeneratorConstants.PCF_KEY;
import static io.harness.generator.constants.SettingsGeneratorConstants.PCF_USERNAME;
import static io.harness.govern.Switch.unhandled;
import static io.harness.testframework.framework.utils.SettingUtils.createGitHubRepoSetting;
import static io.harness.testframework.framework.utils.SettingUtils.createPCFFunctionalTestGitRepoSetting;
import static io.harness.testframework.framework.utils.SettingUtils.createTerraformCityGitRepoSetting;
import static io.harness.testframework.framework.utils.SettingUtils.createTerraformMainGitRepoSetting;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingAttribute.SettingCategory.AZURE_ARTIFACTS;
import static software.wings.beans.SettingAttribute.SettingCategory.CLOUD_PROVIDER;
import static software.wings.beans.SettingAttribute.SettingCategory.CONNECTOR;
import static software.wings.beans.SettingAttribute.SettingCategory.HELM_REPO;
import static software.wings.beans.SettingAttribute.SettingCategory.SETTING;
import static software.wings.utils.UsageRestrictionsUtils.getAllAppAllEnvUsageRestrictions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.generator.AccountGenerator.Accounts;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
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
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.impl.analysis.ElkConnector;
import software.wings.service.impl.analysis.ElkValidationType;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingVariableTypes;

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
  private static final String PCF_CONNECTOR = "Harness PCF";
  private static final String HARNESS_AZURE_ARTIFACTS = "Harness Azure Artifacts";
  private static final String ELK_CONNECTOR = "Elk Connector";

  private static final String HELM_CHART_REPO_URL = "http://storage.googleapis.com/kubernetes-charts/";
  private static final String HELM_CHART_REPO = "Helm Chart Repo";
  private static final String HELM_SOURCE_REPO_URL = "https://github.com/helm/charts.git";
  private static final String HELM_SOURCE_REPO = "Helm Source Repo";
  private static final String HELM_S3_BUCKET = "anshul-test-123";
  private static final String HELM_S3 = "HELM S3";
  private static final String REGION_AP_SOUTH_1 = "ap-south-1";
  private static final String HARNESS_ADMIN = "harnessadmin";

  @Inject AccountGenerator accountGenerator;
  @Inject ScmSecret scmSecret;
  @Inject SecretGenerator secretGenerator;

  @Inject SettingsService settingsService;
  @Inject WingsPersistence wingsPersistence;

  public enum Settings {
    AWS_TEST_CLOUD_PROVIDER,
    AZURE_TEST_CLOUD_PROVIDER,
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
    HELM_S3_CONNECTOR,
    ELK
  }

  public void ensureAllPredefined(Randomizer.Seed seed, Owners owners) {
    EnumSet.allOf(Settings.class).forEach(predefined -> ensurePredefined(seed, owners, predefined));
  }

  public SettingAttribute ensurePredefined(Randomizer.Seed seed, Owners owners, Settings predefined) {
    switch (predefined) {
      case AWS_TEST_CLOUD_PROVIDER:
        return ensureAwsTest(seed, owners);
      case AZURE_TEST_CLOUD_PROVIDER:
        return ensureAzureTestCloudProvider(seed, owners);
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
      case HELM_S3_CONNECTOR:
        return ensureHelmS3Connector(seed, owners);
      case ELK:
        return ensureElkConnector(seed, owners);
      default:
        unhandled(predefined);
    }
    return null;
  }

  private SettingAttribute ensureHelmS3Connector(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute awsCloudProvider = ensurePredefined(seed, owners, Settings.AWS_TEST_CLOUD_PROVIDER);

    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withName(HELM_S3)
                                            .withAccountId(account.getUuid())
                                            .withCategory(HELM_REPO)
                                            .withAccountId(account.getUuid())
                                            .withValue(AmazonS3HelmRepoConfig.builder()
                                                           .accountId(account.getUuid())
                                                           .connectorId(awsCloudProvider.getUuid())
                                                           .bucketName(HELM_S3_BUCKET)
                                                           .region(REGION_AP_SOUTH_1)
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
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withName(PCF_CONNECTOR)
            .withCategory(CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(PcfConfig.builder()
                           .accountId(account.getUuid())
                           .username(PCF_USERNAME.toCharArray())
                           .endpointUrl(PCF_END_POINT)
                           .password(scmSecret.decryptToCharArray(new SecretName(PCF_KEY)))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureServiceNowConnector(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withName(SNOW_CONNECTOR)
            .withCategory(CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(ServiceNowConfig.builder()
                           .accountId(account.getUuid())
                           .baseUrl("https://ven03171.service-now.com")
                           .username(ADMIN)
                           .password(scmSecret.decryptToCharArray(new SecretName("snow_connector")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureTerraformMainGitRepo(Seed seed, Owners owners) {
    SettingAttribute githubKey = ensurePredefined(seed, owners, GITHUB_TEST_CONNECTOR);

    char[] password = scmSecret.decryptToCharArray(new SecretName("terraform_password"));
    SettingAttribute settingAttribute = createTerraformMainGitRepoSetting(githubKey, password);
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureTerraformMainGitAc(Seed seed, Owners owners) {
    SettingAttribute githubKey = ensurePredefined(seed, owners, GITHUB_TEST_CONNECTOR);

    String password = secretGenerator.ensureStored(owners, new SecretName("terraform_password"));
    SettingAttribute settingAttribute = createTerraformMainGitRepoSetting(githubKey, password);
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute ensureAzureTestCloudProvider(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
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
                           .key(scmSecret.decryptToCharArray(new SecretName("azure_key")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureSpotinstTestCloudProvider(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(CLOUD_PROVIDER)
                                            .withName("Test Spotinst Cloud Provider")
                                            .withAppId(GLOBAL_APP_ID)
                                            .withEnvId(GLOBAL_ENV_ID)
                                            .withAccountId(account.getUuid())
                                            .withValue(SpotInstConfig.builder()
                                                           .accountId(account.getUuid())
                                                           .spotInstAccountId("act-d48a11cf")
                                                           .encryptedSpotInstToken("o3j27am9QlygpJqDaKvkYQ")
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute ensureWinRmTestConnector(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
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
                                                           .password("H@rnessH@rness".toCharArray())
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
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CLOUD_PROVIDER)
            .withName(AWS_TEST_CLOUD_PROVIDER.name())
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(account.getUuid())
            .withValue(AwsConfig.builder()
                           .accessKey(scmSecret.decryptToString(new SecretName("aws_playground_access_key")))
                           .secretKey(scmSecret.decryptToCharArray(new SecretName("aws_playground_secret_key")))
                           .accountId(account.getUuid())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureAwsSpotinstTest(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CLOUD_PROVIDER)
            .withName(AWS_SPOTINST_TEST_CLOUD_PROVIDER.name())
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(account.getUuid())
            .withValue(AwsConfig.builder()
                           .accessKey(/*scmSecret.decryptToString(new SecretName("aws_playground_access_key")*/
                               "AKIA4GYQC5QTQPI2UHMY")
                           .secretKey(/*scmSecret.decryptToCharArray(new SecretName("aws_playground_secret_key")*/
                               "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".toCharArray())
                           .accountId(account.getUuid())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureDevTest(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensureRandom(seed, owners);

    final SettingAttribute settingAttribute =
        aSettingAttribute()
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
                           .withKey(scmSecret.decryptToCharArray(new SecretName("ubuntu_private_key")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureWinrmDevTest(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
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
                                                           .password("H@rnessH@rness".toCharArray())
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

    char[] key = scmSecret.decryptToCharArray(new SecretName("playground_private_key"));
    final SettingAttribute settingAttribute = createGitHubRepoSetting(account.getUuid(), key);
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureTerraformCityGitRepo(Randomizer.Seed seed, Owners owners) {
    SettingAttribute githubKey = ensurePredefined(seed, owners, GITHUB_TEST_CONNECTOR);

    char[] password = scmSecret.decryptToCharArray(new SecretName("terraform_password"));
    SettingAttribute settingAttribute = createTerraformCityGitRepoSetting(githubKey, password);
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensurePCFGitRepo(Randomizer.Seed seed, Owners owners) {
    SettingAttribute githubKey = ensurePredefined(seed, owners, GITHUB_TEST_CONNECTOR);

    char[] password = scmSecret.decryptToCharArray(new SecretName("terraform_password"));
    SettingAttribute settingAttribute = createPCFFunctionalTestGitRepoSetting(githubKey, password);
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureHarnessJenkins(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withName(HARNESS_JENKINS)
            .withCategory(CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(JenkinsConfig.builder()
                           .accountId(account.getUuid())
                           .jenkinsUrl("https://jenkinsint.harness.io")
                           .username("wingsbuild")
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_jenkins")))
                           .authMechanism(JenkinsConfig.USERNAME_DEFAULT_TEXT)
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureHarnessJenkinsDev(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withName(HARNESS_JENKINS_DEV)
            .withCategory(CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(JenkinsConfig.builder()
                           .accountId(account.getUuid())
                           .jenkinsUrl("https://jenkins.dev.harness.io")
                           .username("harnessadmin")
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_jenkins_dev")))
                           .authMechanism(JenkinsConfig.USERNAME_DEFAULT_TEXT)
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute ensureHarnessJira(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);

    char[] password = scmSecret.decryptToCharArray(new SecretName("harness_jira"));
    SettingAttribute settingAttribute = SettingUtils.createHarnessJIRASetting(account.getUuid(), password);
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  private SettingAttribute ensureHarnessBamboo(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute bambooSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_BAMBOO)
            .withCategory(SettingCategory.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(BambooConfig.builder()
                           .accountId(account.getUuid())
                           .bambooUrl("http://cdteam-bamboo.harness.io:8085/")
                           .username("wingsbuild")
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_bamboo")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, bambooSettingAttribute, owners);
  }

  private SettingAttribute ensureHarnessNexus(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute nexusSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_NEXUS)
            .withCategory(SettingCategory.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(NexusConfig.builder()
                           .accountId(account.getUuid())
                           .nexusUrl("https://nexus2.harness.io")
                           .username(ADMIN)
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_nexus")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, nexusSettingAttribute, owners);
  }

  private SettingAttribute ensureHarnessNexus2(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute nexusSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_NEXUS_2)
            .withCategory(SettingCategory.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(NexusConfig.builder()
                           .accountId(account.getUuid())
                           .nexusUrl("https://nexus2.dev.harness.io")
                           .username(HARNESS_ADMIN)
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_admin_nexus")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, nexusSettingAttribute, owners);
  }

  private SettingAttribute ensureHarnessNexus3(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute nexus3SettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_NEXUS_THREE)
            .withCategory(SettingCategory.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(NexusConfig.builder()
                           .accountId(account.getUuid())
                           .nexusUrl("https://nexus3.harness.io")
                           .username(ADMIN)
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_nexus")))
                           .version("3.x")
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, nexus3SettingAttribute, owners);
  }

  private SettingAttribute ensureHarnessArtifactory(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute artifactorySettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_ARTIFACTORY)
            .withCategory(SettingCategory.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(ArtifactoryConfig.builder()
                           .accountId(account.getUuid())
                           .artifactoryUrl("https://harness.jfrog.io/harness")
                           .username(ADMIN)
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_artifactory")))
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
            .withCategory(SettingCategory.CONNECTOR)
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
    SettingAttribute gcpSettingAttribute =
        aSettingAttribute()
            .withName(GCP_PLAYGROUND)
            .withCategory(SettingCategory.CLOUD_PROVIDER)
            .withAccountId(account.getUuid())
            .withValue(GcpConfig.builder()
                           .serviceAccountKeyFileContent(scmSecret.decryptToCharArray(new SecretName("gcp_playground")))
                           .accountId(account.getUuid())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, gcpSettingAttribute, owners);
  }

  private SettingAttribute ensurePaidSMTPSettings(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    String secret = scmSecret.decryptToString(new SecretName("smtp_paid_sendgrid_config_password"));
    SmtpConfig smtpConfig = SmtpConfig.builder()
                                .host("smtp.sendgrid.net")
                                .port(465)
                                .useSSL(true)
                                .fromAddress("automation@harness.io")
                                .username("apikey")
                                .password(secret.toCharArray())
                                .accountId(account.getUuid())
                                .build();

    SettingAttribute emailSettingAttribute = aSettingAttribute()
                                                 .withCategory(SettingCategory.CONNECTOR)
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

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withName(HARNESS_AZURE_ARTIFACTS)
            .withCategory(AZURE_ARTIFACTS)
            .withAccountId(account.getUuid())
            .withValue(AzureArtifactsPATConfig.builder()
                           .accountId(account.getUuid())
                           .azureDevopsUrl("https://dev.azure.com/garvit-test")
                           .pat(scmSecret.decryptToCharArray(new SecretName("harness_azure_devops_pat")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute, owners);
  }

  public SettingAttribute exists(SettingAttribute settingAttribute) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .filter(SettingAttributeKeys.accountId, settingAttribute.getAccountId())
        .filter(SettingAttributeKeys.appId, settingAttribute.getAppId())
        .filter(SettingAttribute.ENV_ID_KEY, settingAttribute.getEnvId())
        .filter(SettingAttribute.CATEGORY_KEY, settingAttribute.getCategory())
        .filter(SettingAttribute.NAME_KEY, settingAttribute.getName())
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
