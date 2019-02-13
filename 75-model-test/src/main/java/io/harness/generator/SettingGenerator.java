package io.harness.generator;

import static io.harness.generator.SettingGenerator.Settings.AWS_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.AZURE_TEST_CLOUD_PROVIDER;
import static io.harness.generator.SettingGenerator.Settings.DEV_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.GITHUB_TEST_CONNECTOR;
import static io.harness.generator.SettingGenerator.Settings.HARNESS_EXPLORATION_GCS;
import static io.harness.generator.SettingGenerator.Settings.PHYSICAL_DATA_CENTER;
import static io.harness.generator.SettingGenerator.Settings.TERRAFORM_TEST_GIT_REPO;
import static io.harness.govern.Switch.unhandled;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingAttribute.Category.CLOUD_PROVIDER;
import static software.wings.beans.SettingAttribute.Category.CONNECTOR;
import static software.wings.beans.SettingAttribute.Category.SETTING;
import static software.wings.utils.UsageRestrictionsUtil.getAllAppAllEnvUsageRestrictions;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DuplicateKeyException;
import io.harness.exception.WingsException;
import io.harness.generator.AccountGenerator.Accounts;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import software.wings.beans.Account;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.BambooConfig;
import software.wings.beans.DockerConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.GitConfig;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.JiraConfig;
import software.wings.beans.PhysicalDataCenterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.common.Constants;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SettingsService;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.EnumSet;

@Singleton
public class SettingGenerator {
  private static final String HARNESS_NEXUS = "Harness Nexus";
  private static final String HARNESS_JENKINS = "Harness Jenkins";
  private static final String HARNESS_JIRA = "Harness Jira";
  private static final String HARNESS_NEXUS_THREE = "Harness Nexus 3";
  private static final String HARNESS_ARTIFACTORY = "Harness Artifactory";
  private static final String HARNESS_BAMBOO = "Harness Bamboo";
  private static final String HARNESS_DOCKER_REGISTRY = "Harness Docker Registry";
  private static final String HARNESS_GCP_EXPLORATION = "harness-exploration";
  private static final String HARNESS_EXPLORATION_GCS = "harness-exploration-gcs";
  private static final String AZURE_CLIENT_ID = "2d7ae800-e1dd-4098-97f8-f6ae330abf82";
  private static final String AZURE_TENANT_ID = "b229b2bb-5f33-4d22-bce0-730f6474e906";
  private static final String AZURE_KEY = "ON3N2Ce+GxKcnculN2A7tDUr4EGhp5bUVne4v7Wg4Zg=";

  @Inject AccountGenerator accountGenerator;
  @Inject ScmSecret scmSecret;

  @Inject SettingsService settingsService;
  @Inject WingsPersistence wingsPersistence;

  public enum Settings {
    AWS_TEST_CLOUD_PROVIDER,
    AZURE_TEST_CLOUD_PROVIDER,
    DEV_TEST_CONNECTOR,
    HARNESS_JENKINS_CONNECTOR,
    GITHUB_TEST_CONNECTOR,
    TERRAFORM_TEST_GIT_REPO,
    HARNESS_BAMBOO_CONNECTOR,
    HARNESS_NEXUS_CONNECTOR,
    HARNESS_NEXU3_CONNECTOR,
    HARNESS_ARTIFACTORY_CONNECTOR,
    HARNESS_DOCKER_REGISTRY,
    HARNESS_GCP_EXPLORATION,
    HARNESS_EXPLORATION_GCS,
    HARNESS_JIRA,
    PHYSICAL_DATA_CENTER,
    WINRM_TEST_CONNECTOR
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
      case DEV_TEST_CONNECTOR:
        return ensureDevTest(seed);
      case HARNESS_JENKINS_CONNECTOR:
        return ensureHarnessJenkins(seed, owners);
      case GITHUB_TEST_CONNECTOR:
        return ensureGithubTest(seed, owners);
      case TERRAFORM_TEST_GIT_REPO:
        return ensureTerraformTestGitRepo(seed, owners);
      case HARNESS_BAMBOO_CONNECTOR:
        return ensureHarnessBamboo(seed, owners);
      case HARNESS_NEXUS_CONNECTOR:
        return ensureHarnessNexus(seed, owners);
      case HARNESS_NEXU3_CONNECTOR:
        return ensureHarnessNexus3(seed, owners);
      case HARNESS_ARTIFACTORY_CONNECTOR:
        return ensureHarnessArtifactory(seed, owners);
      case HARNESS_DOCKER_REGISTRY:
        return ensureHarnessDocker(seed, owners);
      case HARNESS_GCP_EXPLORATION:
        return ensureHarnessGcpExploration(seed, owners);
      case HARNESS_EXPLORATION_GCS:
        return ensureHarnessExplorationGcs(seed, owners);
      case HARNESS_JIRA:
        return ensureHarnessJira(seed, owners);
      case PHYSICAL_DATA_CENTER:
        return ensurePhysicalDataCenter(seed, owners);
      case WINRM_TEST_CONNECTOR:
        return ensureWinRmTestConnector(seed, owners);
      default:
        unhandled(predefined);
    }
    return null;
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
    return ensureSettingAttribute(seed, settingAttribute);
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
    return ensureSettingAttribute(seed, settingAttribute);
  }

  public SettingAttribute ensureAzureTestCloudProvider(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute settingAttribute = aSettingAttribute()
                                            .withCategory(CLOUD_PROVIDER)
                                            .withName(AZURE_TEST_CLOUD_PROVIDER.name())
                                            .withAppId(GLOBAL_APP_ID)
                                            .withEnvId(GLOBAL_ENV_ID)
                                            .withAccountId(account.getUuid())
                                            .withValue(AzureConfig.builder()
                                                           .clientId(AZURE_CLIENT_ID)
                                                           .tenantId(AZURE_TENANT_ID)
                                                           .accountId(account.getUuid())
                                                           .key(AZURE_KEY.toCharArray())
                                                           .build())
                                            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
                                            .build();
    return ensureSettingAttribute(seed, settingAttribute);
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
    return ensureSettingAttribute(seed, settingAttribute);
  }

  private SettingAttribute ensureDevTest(Randomizer.Seed seed) {
    final Account account = accountGenerator.randomAccount();

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
    return ensureSettingAttribute(seed, settingAttribute);
  }

  private SettingAttribute ensureGithubTest(Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);

    final SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CONNECTOR)
            .withAccountId(account.getUuid())
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withName(GITHUB_TEST_CONNECTOR.name())
            .withValue(aHostConnectionAttributes()
                           .withConnectionType(SSH)
                           .withAccessType(KEY)
                           .withAccountId(account.getUuid())
                           .withUserName("test-harness")
                           .withKey(scmSecret.decryptToCharArray(new SecretName("playground_private_key")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute);
  }

  private SettingAttribute ensureTerraformTestGitRepo(Randomizer.Seed seed, Owners owners) {
    SettingAttribute githubKey = ensurePredefined(seed, owners, GITHUB_TEST_CONNECTOR);

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withCategory(CONNECTOR)
            .withName(TERRAFORM_TEST_GIT_REPO.name())
            .withAppId(githubKey.getAppId())
            .withEnvId(githubKey.getEnvId())
            .withAccountId(githubKey.getAccountId())
            .withValue(GitConfig.builder()
                           .repoUrl("https://github.com/wings-software/terraform-test.git")
                           .username("test-harness")
                           .password(scmSecret.decryptToCharArray(new SecretName("terraform_password")))
                           .branch("master")
                           .accountId(githubKey.getAccountId())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute);
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
                           .authMechanism(Constants.USERNAME_PASSWORD_FIELD)
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute);
  }

  private SettingAttribute ensureHarnessJira(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);

    SettingAttribute settingAttribute =
        aSettingAttribute()
            .withName(HARNESS_JIRA)
            .withCategory(CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(JiraConfig.builder()
                           .accountId(account.getUuid())
                           .baseUrl("https://harness.atlassian.net")
                           .username("jirauser@harness.io")
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_jira")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, settingAttribute);
  }

  private SettingAttribute ensureHarnessBamboo(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute bambooSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_BAMBOO)
            .withCategory(Category.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(BambooConfig.builder()
                           .accountId(account.getUuid())
                           .bambooUrl("http://ec2-18-208-86-222.compute-1.amazonaws.com:8085/")
                           .username("wingsbuild")
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_bamboo")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, bambooSettingAttribute);
  }

  private SettingAttribute ensureHarnessNexus(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute nexusSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_NEXUS)
            .withCategory(Category.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(NexusConfig.builder()
                           .accountId(account.getUuid())
                           .nexusUrl("https://nexus2.harness.io")
                           .username("admin")
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_nexus")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, nexusSettingAttribute);
  }

  private SettingAttribute ensureHarnessNexus3(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute nexus3SettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_NEXUS_THREE)
            .withCategory(Category.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(NexusConfig.builder()
                           .accountId(account.getUuid())
                           .nexusUrl("https://nexus3.harness.io")
                           .username("admin")
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_nexus")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, nexus3SettingAttribute);
  }

  private SettingAttribute ensureHarnessArtifactory(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute artifactorySettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_ARTIFACTORY)
            .withCategory(Category.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(ArtifactoryConfig.builder()
                           .accountId(account.getUuid())
                           .artifactoryUrl("https://harness.jfrog.io/harness")
                           .username("admin")
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_artifactory")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, artifactorySettingAttribute);
  }

  private SettingAttribute ensureHarnessDocker(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute dockerSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_DOCKER_REGISTRY)
            .withCategory(Category.CONNECTOR)
            .withAccountId(account.getUuid())
            .withValue(DockerConfig.builder()
                           .accountId(account.getUuid())
                           .dockerRegistryUrl("https://registry.hub.docker.com/v2/")
                           .username("wingsplugins")
                           .password(scmSecret.decryptToCharArray(new SecretName("harness_docker_hub")))
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, dockerSettingAttribute);
  }

  private SettingAttribute ensureHarnessGcpExploration(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute gcpSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_GCP_EXPLORATION)
            .withCategory(Category.CLOUD_PROVIDER)
            .withAccountId(account.getUuid())
            .withValue(GcpConfig.builder()
                           .serviceAccountKeyFileContent(
                               scmSecret.decryptToCharArray(new SecretName("harness_gcp_exploration")))
                           .accountId(account.getUuid())
                           .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, gcpSettingAttribute);
  }

  private SettingAttribute ensureHarnessExplorationGcs(Randomizer.Seed seed, Owners owners) {
    final Account account = accountGenerator.ensurePredefined(seed, owners, Accounts.GENERIC_TEST);
    SettingAttribute gcpSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_EXPLORATION_GCS)
            .withCategory(Category.CLOUD_PROVIDER)
            .withAccountId(account.getUuid())
            .withValue(
                GcpConfig.builder()
                    .serviceAccountKeyFileContent(scmSecret.decryptToCharArray(new SecretName("harness_exploration")))
                    .accountId(account.getUuid())
                    .build())
            .withUsageRestrictions(getAllAppAllEnvUsageRestrictions())
            .build();
    return ensureSettingAttribute(seed, gcpSettingAttribute);
  }

  public SettingAttribute exists(SettingAttribute settingAttribute) {
    return wingsPersistence.createQuery(SettingAttribute.class)
        .filter(SettingAttribute.ACCOUNT_ID_KEY, settingAttribute.getAccountId())
        .filter(SettingAttribute.APP_ID_KEY, settingAttribute.getAppId())
        .filter(SettingAttribute.ENV_ID_KEY, settingAttribute.getEnvId())
        .filter(SettingAttribute.CATEGORY_KEY, settingAttribute.getCategory())
        .filter(SettingAttribute.NAME_KEY, settingAttribute.getName())
        .get();
  }

  public SettingAttribute ensureSettingAttribute(Randomizer.Seed seed, SettingAttribute settingAttribute) {
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

    SettingAttribute existing = exists(builder.build());
    if (existing != null) {
      return existing;
    }

    builder.withValue(settingAttribute.getValue());

    try {
      return settingsService.forceSave(builder.build());

    } catch (WingsException we) {
      if (we.getCause() instanceof DuplicateKeyException) {
        SettingAttribute exists = exists(builder.build());
        if (exists != null) {
          return exists;
        }
      }
      throw we;
    }
  }
}
