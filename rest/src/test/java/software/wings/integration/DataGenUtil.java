package software.wings.integration;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.InfrastructureMappingType.AWS_SSH;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Pipeline.Builder.aPipeline;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.artifact.JenkinsArtifactStream.Builder.aJenkinsArtifactStream;
import static software.wings.integration.IntegrationTestUtil.randomInt;
import static software.wings.integration.SeedData.containerNames;
import static software.wings.integration.SeedData.envNames;
import static software.wings.integration.SeedData.seedNames;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.ArtifactType.WAR;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.JUnitCore;
import software.wings.api.DeploymentType;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AppContainer;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.AwsInstanceFilter.Tag;
import software.wings.beans.BambooConfig;
import software.wings.beans.Base;
import software.wings.beans.DockerConfig;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.GitConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.KmsConfig;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.SplunkConfig;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.config.ArtifactoryConfig;
import software.wings.beans.config.NexusConfig;
import software.wings.common.Constants;
import software.wings.dl.PageResponse;
import software.wings.generator.AccountGenerator;
import software.wings.generator.ApplicationGenerator;
import software.wings.generator.ArtifactStreamGenerator;
import software.wings.generator.EnvironmentGenerator;
import software.wings.generator.EnvironmentGenerator.Environments;
import software.wings.generator.InfrastructureMappingGenerator;
import software.wings.generator.PipelineGenerator;
import software.wings.generator.ServiceGenerator;
import software.wings.generator.ServiceTemplateGenerator;
import software.wings.generator.WorkflowGenerator;
import software.wings.generator.WorkflowGenerator.PostProcessInfo;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.rules.SetupScheduler;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SystemCatalogService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.ArtifactType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 * Created by anubhaw on 5/6/16.
 */
@SetupScheduler
public class DataGenUtil extends BaseIntegrationTest {
  private static final int NUM_APPS = 1; /* Max 1000 */
  private static final int NUM_APP_CONTAINER_PER_APP = 2; /* Max 1000 */
  private static final int NUM_SERVICES_PER_APP = 1; /* Max 1000 */
  private static final int NUM_CONFIG_FILE_PER_SERVICE = 2; /* Max 100  */
  private static final int NUM_ENV_PER_APP = 0; /* Max 6. 4 are created by default */
  private static final int NUM_TAG_GROUPS_PER_ENV = 3; /* Max 10   */
  private static final int TAG_HIERARCHY_DEPTH = 3; /* Max 10   */
  public static final String HARNESS_NEXUS = "Harness Nexus";
  public static final String HARNESS_NEXUS_THREE = "Harness Nexus 3";
  public static final String HARNESS_ARTIFACTORY = "Harness Artifactory";
  public static final String HARNESS_BAMBOO_SERVICE = "Harness BambooService";
  public static final String HARNESS_DOCKER_REGISTRY = "Harness Docker Registry";
  public static final String TESTING_ENVIRONMENT = "Testing Environment";
  public static final String TERRAFORM_TEST_SCRIPTS = "Terraform test scripts";
  public static final String GIT_REPO_TERRAFORM_TEST = "Git Repo terraform test";
  public static final String AWS_NON_PROD = "Aws non-prod";
  public static final String AWS_TEST = "Aws test";
  public static final String WINGS_KEY = "Wings Key";
  public static final String DEV_KEY = "Dev Test Key";

  /**
   * The Test folder.
   */
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  private List<String> appNames = new ArrayList<>(seedNames);
  private List<String> serviceNames;
  private List<String> configFileNames;
  private SettingAttribute envAttr;
  @Inject private WorkflowService workflowService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private MainConfiguration configuration;
  @Inject private SystemCatalogService systemCatalogService;
  @Inject private SettingsService settingsService;
  @Inject private EnvironmentService environmentService;
  @Inject private FeatureFlagService featureFlagService;

  @Inject private AccountGenerator accountGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ArtifactStreamGenerator artifactStreamGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureMappingGenerator infrastructureMappingGenerator;
  @Inject private PipelineGenerator pipelineGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private ServiceTemplateGenerator serviceTemplateGenerator;
  @Inject private WorkflowGenerator workflowGenerator;

  /**
   * Generated Data for across the API use.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();
    initMocks(this);

    assertThat(NUM_APPS).isBetween(1, 1000);
    assertThat(NUM_APP_CONTAINER_PER_APP).isBetween(1, 1000);
    assertThat(NUM_SERVICES_PER_APP).isBetween(1, 1000);
    assertThat(NUM_ENV_PER_APP).isBetween(0, 10);
    assertThat(NUM_TAG_GROUPS_PER_ENV).isBetween(1, 10);
    assertThat(TAG_HIERARCHY_DEPTH).isBetween(1, 10);

    dropDBAndEnsureIndexes();
  }

  /**
   * Populate data.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void populateData() throws IOException {
    Account account = createLicenseAndDefaultUsers();
    accountGenerator.setAccount(account);
    createGlobalSettings();

    List<Application> apps = createApplications();
    Map<String, List<AppContainer>> containers = new HashMap<>();
    Map<String, List<Service>> services = new HashMap<>();
    Map<String, List<Environment>> appEnvs = new HashMap<>();

    for (Application application : apps) {
      appEnvs.put(application.getUuid(), addEnvs(application.getUuid()));
      containers.put(application.getUuid(), addAppContainers(application.getUuid()));
      services.put(application.getUuid(),
          addServices(application.getAccountId(), application.getUuid(), containers.get(GLOBAL_APP_ID)));
    }
    featureFlagService.initializeFeatureFlags();
    addAndEnableKms();
    enableRbac();
    learningEngineService.initializeServiceSecretKeys();

    createTestApplication(account);
  }

  private void enableRbac() {
    FeatureFlag featureFlag =
        wingsPersistence.createQuery(FeatureFlag.class).field("name").equal(FeatureName.RBAC.name()).get();

    if (featureFlag == null) {
      featureFlag = FeatureFlag.builder().name(FeatureName.RBAC.name()).enabled(true).obsolete(false).build();
    } else {
      featureFlag.setEnabled(true);
      featureFlag.setObsolete(false);
    }
    wingsPersistence.save(featureFlag);
  }

  private void createGlobalSettings() {
    SettingAttribute jenkinsSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_JENKINS)
            .withCategory(Category.CONNECTOR)
            .withAccountId(accountId)
            .withValue(JenkinsConfig.builder()
                           .accountId(accountId)
                           .jenkinsUrl("https://jenkins.wings.software")
                           .username("wingsbuild")
                           .password("06b13aea6f5f13ec69577689a899bbaad69eeb2f".toCharArray())
                           .build())
            .build();
    wingsPersistence.save(jenkinsSettingAttribute);

    SettingAttribute nexusSettingAttribute = aSettingAttribute()
                                                 .withName(HARNESS_NEXUS)
                                                 .withCategory(Category.CONNECTOR)
                                                 .withAccountId(accountId)
                                                 .withValue(NexusConfig.builder()
                                                                .accountId(accountId)
                                                                .nexusUrl("https://nexus.wings.software")
                                                                .username("admin")
                                                                .password("wings123!".toCharArray())
                                                                .build())
                                                 .build();
    wingsPersistence.save(nexusSettingAttribute);

    SettingAttribute artifactorySettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_ARTIFACTORY)
            .withCategory(Category.CONNECTOR)
            .withAccountId(accountId)
            .withValue(ArtifactoryConfig.builder()
                           .accountId(accountId)
                           .artifactoryUrl("https://harness.jfrog.io/harness")
                           .username("admin")
                           .password("harness123!".toCharArray())
                           .build())
            .build();
    wingsPersistence.save(artifactorySettingAttribute);

    SettingAttribute bambooSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_BAMBOO_SERVICE)
            .withCategory(Category.CONNECTOR)
            .withAccountId(accountId)
            .withValue(BambooConfig.builder()
                           .accountId(accountId)
                           .bambooUrl("http://ec2-34-205-16-35.compute-1.amazonaws.com:8085/")
                           .username("wingsbuild")
                           .password("0db28aa0f4fc0685df9a216fc7af0ca96254b7c2".toCharArray())
                           .build())
            .build();
    wingsPersistence.save(bambooSettingAttribute);

    SettingAttribute dockerSettingAttribute =
        aSettingAttribute()
            .withName(HARNESS_DOCKER_REGISTRY)
            .withCategory(Category.CONNECTOR)
            .withAccountId(accountId)
            .withValue(DockerConfig.builder()
                           .accountId(accountId)
                           .dockerRegistryUrl("https://registry.hub.docker.com/v2/")
                           .username("wingsplugins")
                           .password("W!ngs@DockerHub".toCharArray())
                           .build())
            .build();
    wingsPersistence.save(dockerSettingAttribute);

    SettingAttribute smtpSettingAttribute = aSettingAttribute()
                                                .withCategory(Category.CONNECTOR)
                                                .withName("SMTP")
                                                .withAccountId(accountId)
                                                .withValue(SmtpConfig.builder()
                                                               .accountId(accountId)
                                                               .fromAddress("support@harness.io")
                                                               .username("support@harness.io")
                                                               .host("smtp.gmail.com")
                                                               .password("@wes0me@pp".toCharArray())
                                                               .port(465)
                                                               .useSSL(true)
                                                               .build())
                                                .build();
    wingsPersistence.save(smtpSettingAttribute);

    SettingAttribute splunkSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName("Splunk")
            .withAccountId(accountId)
            .withValue(SplunkConfig.builder()
                           .accountId(accountId)
                           .splunkUrl("https://ec2-52-54-103-49.compute-1.amazonaws.com:8089")
                           .password("W!ngs@Splunk".toCharArray())
                           .username("admin")
                           .build())
            .build();
    wingsPersistence.save(splunkSettingAttribute);

    SettingAttribute appdSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName("AppDynamics")
            .withAccountId(accountId)
            .withValue(AppDynamicsConfig.builder()
                           .accountId(accountId)
                           .controllerUrl("https://wingsnfr.saas.appdynamics.com/controller")
                           .username("wingsnfr")
                           .accountname("wingsnfr")
                           .password("cbm411sjesma".toCharArray())
                           .build())
            .build();
    wingsPersistence.save(appdSettingAttribute);

    SettingAttribute newRelicSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName("NewRelic")
            .withAccountId(accountId)
            .withValue(NewRelicConfig.builder()
                           .accountId(accountId)
                           .newRelicUrl("https://api.newrelic.com")
                           .apiKey("5ed76b50ebcfda54b77cd1daaabe635bd7f2e13dc6c5b11".toCharArray())
                           .build())
            .build();

    wingsPersistence.save(newRelicSettingAttribute);

    SettingAttribute terraformGitRepoAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName(GIT_REPO_TERRAFORM_TEST)
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(accountId)
            .withValue(GitConfig.builder()
                           .repoUrl("https://github.com/wings-software/terraform-test.git")
                           .branch("master")
                           .accountId(accountId)
                           .build())
            .build();
    wingsPersistence.save(terraformGitRepoAttribute);

    SettingAttribute awsNonProdAttribute =
        aSettingAttribute()
            .withCategory(Category.CLOUD_PROVIDER)
            .withName(AWS_NON_PROD)
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(accountId)
            .withValue(AwsConfig.builder()
                           .accessKey("AKIAIVRKRUMJ3LAVBMSQ")
                           .secretKey("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".toCharArray())
                           .accountId(accountId)
                           .build())
            .build();
    wingsPersistence.save(awsNonProdAttribute);

    SettingAttribute awsTestAttribute =
        aSettingAttribute()
            .withCategory(Category.CLOUD_PROVIDER)
            .withName(AWS_TEST)
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(accountId)
            .withValue(AwsConfig.builder()
                           .accessKey("AKIAJJUEMEKQBYHZCQSA")
                           .secretKey("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".toCharArray())
                           .accountId(accountId)
                           .build())
            .build();
    wingsPersistence.save(awsTestAttribute);

    final SettingAttribute hostConnection =
        aSettingAttribute()
            .withAccountId(accountId)
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withName(WINGS_KEY)
            .withValue(aHostConnectionAttributes()
                           .withConnectionType(SSH)
                           .withAccessType(KEY)
                           .withAccountId(accountId)
                           .withUserName("ubuntu")
                           .withKey(("-----BEGIN RSA PRIVATE KEY-----\n"
                               + "MIIEogIBAAKCAQEArCtMvZebz8vGCUah4C4kThYOAEjrdgaGe8M8w+66jPKEnX1GDXj4mrlIxRxO\n"
                               + "ErJTwNirPLhIhw/8mGojcsbc5iY7wK6TThJI0uyzUtPfZ1g8zzcQxh7aMOYso/Nxoz6YtO6HRQhd\n"
                               + "rxiFuadVo+RuVUeBvVBiQauZMoESh1vGZ2r1eTuXKrSiStaafSfVzSEfvtJYNWnPguqcuGlrX3yv\n"
                               + "sNOlIWzU3YETk0bMG3bejChGAKh35AhZdDO+U4g7zH8NI5KjT9IH7MyKAFxiCPYkNm7Y2Bw8j2eL\n"
                               + "DIkqIA1YX0VxXBiCC2Vg78o7TxJ7Df7f3V+Q+Xhtj4rRtYCFw1pqBwIDAQABAoIBAGA//LDpNuQe\n"
                               + "SWIaKJkJcqZs0fr6yRe8YiaCaVAoAAaX9eeNh0I05NaqyrHXNxZgt03SUzio1XMcTtxuSc76ube4\n"
                               + "nCMF9bfppOi2BzJA3F4MCELXx/raeKRpqX8ms9rNPdW4m8rN+IHQtcGqeMgdBkmKpk9NxwBrjEOd\n"
                               + "wNwHRI2/Y/ZCApkQDhRPXfEJXnY65SJJ8Vh1NAm6RuiKXv9+8J1//OHAeRfIXTJI4KiwP2EFHeXF\n"
                               + "6K0EBVEb/M2kg81bh7iq2OoDxBVrF1Uozg4KUK2EMoCe5OidcSdD1G8ICTsRQlb9iW5e/c2UeCrb\n"
                               + "HGkcmQyvDniyfFmVVymyr0vJTnECgYEA6FsPq4T+M0Cj6yUqsIdijqgpY31iX2BAibrLTOFUYhj3\n"
                               + "oPpy2bciREXffMGpqiAY8czy3aEroNDC5c7lcwS1HuMgNls0nKuaPLaSg0rSXX9wRn0mYpasBEZ8\n"
                               + "5pxFX44FnqTDa37Y7MqKykoMpEB71s1DtG9Ug1cMRuPftZZQ5qsCgYEAvbBcEiPFyKf5g2QRVA/k\n"
                               + "FDQcX9hVm7cvDTo6+Qq6XUwrQ2cm9ZJ+zf+Jak+NSN88GNTzAPCWzd8zbZ2D7q4qAyDcSSy0PR3K\n"
                               + "bHpLFZnYYOIkSfYcM3CwDhIFTnb9uvG8mypfMFGZ2qUZY/jbI0/cCctsUaXt03g4cM4Q04peehUC\n"
                               + "gYAcsWoM9z5g2+GiHxPXetB75149D/W+62bs2ylR1B2Ug5rIwUS/h/LuVWaUxGGMRaxu560yGz4E\n"
                               + "/OKkeFkzS+iF6OxIahjkI/jG+JC9L9csfplByyCbWhnh6UZxP+j9NM+S2KvdMWveSeC7vEs1WVUx\n"
                               + "oGV0+a6JDY3Rj0BH70kMQwKBgD1ZaK3FPBalnSFNn/0cFpwiLnshMK7oFCOnDaO2QIgkNmnaVtNd\n"
                               + "yf0+BGeJyxwidwFg/ibzqRJ0eeGd7Cmp0pSocBaKitCpbeqfsuENnNnYyfvRyVUpwQcL9QNnoLBx\n"
                               + "tppInfi2q5f3hbq7pcRJ89SHIkVV8RFP9JEnVHHWcq/xAoGAJNbaYQMmLOpGRVwt7bdK5FXXV5OX\n"
                               + "uzSUPICQJsflhj4KPxJ7sdthiFNLslAOyNYEP+mRy90ANbI1x7XildsB2wqBmqiXaQsyHBXRh37j\n"
                               + "dMX4iYY1mW7JjS9Y2jy7xbxIBYDpwnqHLTMPSKFQpwsi7thP+0DRthj62sCjM/YB7Es=\n"
                               + "-----END RSA PRIVATE KEY-----")
                                        .toCharArray())
                           .build())
            .build();
    wingsPersistence.save(hostConnection);

    final SettingAttribute devTestKey =
        aSettingAttribute()
            .withAccountId(accountId)
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withName(DEV_KEY)
            .withValue(aHostConnectionAttributes()
                           .withConnectionType(SSH)
                           .withAccessType(KEY)
                           .withAccountId(accountId)
                           .withUserName("ubuntu")
                           .withKey(("-----BEGIN RSA PRIVATE KEY-----\n"
                               + "MIIEpQIBAAKCAQEA1Bxs1dMQSD25VBrTVvMvTFwTagL+N9qKAptxdBBRvxFm9Kfv\n"
                               + "TsZAibtfFgXa71gy7id+uMDPGQEHtIeXXvzkYPq/MPltVooWhouadGzrOr1hVDHM\n"
                               + "UGwDGQrpy7XyZPfHKjjGmNUd+B27zDon5RtOZbCbRCvevoPnCvTtItfSFLiF/mE+\n"
                               + "q///1jpyf6jLPz/vpARLr2VoZDNvxhU/RdJOSXQVkxEQKzDUMTsgCTZkh1xc9Nb1\n"
                               + "gfDvd1BfJv6l+2nh2sWmRSy72lbupxDcUG5CUPD4V/ka9duVfGKmylo9QooiW5ER\n"
                               + "0qa0lCHGbzil8xRZwR4gqAct7YU8da1FEBWGlQIDAQABAoIBAQCWz8MeYRxRkPll\n"
                               + "cFFVoEC/9TOki44/HjZEVktbb4L/7Bqc146SHumiREP+P5mD1d0YcaJrMEPPjmjx\n"
                               + "FfstgXfL8FziMGZqQnJzpWzjXNH/iMlb+LBBehrVwmmq+qnm2jmUrpud7OGLGXD+\n"
                               + "a1cUUc7zBJfQ57RPFy++HZlBzdvD+IcPuVqyyQoS6f0PzGrC3nuqsqYKjmAoOJsx\n"
                               + "kLuKS59QJ/HXEGJtduw0UvjfQS4l3qebbFAcImIldZ0kVumhIlxcpes6kqZw8dfH\n"
                               + "dZOndMujWYaJIxRhLHwla+myE6p3eneVg15EcBj9PGKHZkXrmk7Jlt+2j6PVQikw\n"
                               + "Z7HJDwThAoGBAPW1dGbR5ml1wYQnqwLUp9TtMmZqFMC3gMgNXd3NIJkyK1vM0rZs\n"
                               + "qokZB2SxyXwCHw+FjUG9WT/Jahy/Pk1D4cBxGgO5CqK+GjON27tn+HcSjt20ZUnl\n"
                               + "dRhsEIyau034ecIR6zsyHXxJxcU1+yfMp1DD8u0n1wq8OWo3HRH6pn+JAoGBANz+\n"
                               + "ukC8TAF/WnTXaLrYR2KB9KmbD9KmdUT0289xafFIlF8WFdz6baZCXIXmo/oiOURv\n"
                               + "bPnJEqZHsowfdky6m8CHC3zsH6GZDrRP03qj1rHxgu5LP5Na4dHXRB03/dg5nZSV\n"
                               + "mfkFI3swI+9nC0g49g0djT/aqleLbezPUrdRcd+tAoGBAIVtzVFMqOgaF0Vx2S8H\n"
                               + "VkCNsnHlJ3Hj9J4ujAu3qf0nPl5yovaHmjArFFW9KiIacM2YA7ZwYbf+443K2MVS\n"
                               + "mJRNlwfwg3MO8uGOJoXllwrqXATPQrXXUjg57t674/0actxNqMUTmOl2klxezQ22\n"
                               + "2CFG13Orz943iqJAXZv21lWpAoGBAI6+LdnAhit1ch0EQg5lwn4bSMgAc1Dx2c9H\n"
                               + "hW9RZ0fFRKjCYC7Sxt5cAN0wY3wefPT6L96LhPNIXkhpzgSziATsdXwkHC5J6ZiH\n"
                               + "8yZFC1j2kUaP7imkyzW6ILHqx5jRZjpiAwk4y3k3WA67dSsaN7uy+dhjyiEv2znZ\n"
                               + "lCj6f14lAoGAWGMSz05Ugzk7W7XWDkbM+I3K1nCRX7Dws32dWmyPNoEGy+x8sCcu\n"
                               + "9XdXmwNc7akrF8jG8Zk/0qwlfvYh4kSRQr037sdQpB1HrSAP4LeVSeJZFohi1QZG\n"
                               + "lcqQrz6/ZvrHFG/VTrr1JOGSNlKmmptsk9IQAm0nedOh+rWx63w+kJQ=\n"
                               + "-----END RSA PRIVATE KEY-----\n")
                                        .toCharArray())
                           .build())
            .build();
    wingsPersistence.save(devTestKey);
  }

  private void createTestApplication(Account account) {
    int seed = 0;

    Environment environment = environmentGenerator.ensurePredefined(seed, Environments.GENERIC_TEST);

    Service service = serviceGenerator.createService(seed,
        aService()
            .withAppId(environment.getAppId())
            .withName("Test Service")
            .withArtifactType(ArtifactType.WAR)
            .build());

    ServiceTemplate serviceTemplate = wingsPersistence.createQuery(ServiceTemplate.class)
                                          .filter("appId", service.getAppId())
                                          .filter("serviceId", service.getUuid())
                                          .filter("envId", environment.getUuid())
                                          .get();

    final SettingAttribute awsTest = settingsService.getByName(accountId, GLOBAL_APP_ID, AWS_TEST);
    final SettingAttribute devKey = settingsService.getByName(accountId, GLOBAL_APP_ID, DEV_KEY);
    final SettingAttribute terraformTest = settingsService.getByName(accountId, GLOBAL_APP_ID, GIT_REPO_TERRAFORM_TEST);

    final List<Tag> tags = asList(Tag.builder().key("Purpose").value("test").build(),
        Tag.builder().key("User").value(System.getProperty("user.name")).build());

    InfrastructureMapping infrastructureMapping = infrastructureMappingGenerator.createInfrastructureMapping(seed,
        anAwsInfrastructureMapping()
            .withName("Aws non prod - ssh workflow test")
            .withAutoPopulate(false)
            .withInfraMappingType(AWS_SSH.name())
            .withAccountId(account.getUuid())
            .withAppId(environment.getAppId())
            .withServiceTemplateId(serviceTemplate.getUuid())
            .withEnvId(environment.getUuid())
            .withDeploymentType(DeploymentType.SSH.name())
            .withComputeProviderType(SettingVariableTypes.AWS.name())
            .withComputeProviderSettingId(awsTest.getUuid())
            .withHostConnectionAttrs(devKey.getUuid())
            .withUsePublicDns(true)
            .withRegion("us-east-1")
            .withAwsInstanceFilter(AwsInstanceFilter.builder().tags(tags).build())
            .build());

    final SettingAttribute jenkins = settingsService.getByName(accountId, GLOBAL_APP_ID, HARNESS_JENKINS);

    ArtifactStream artifactStream = artifactStreamGenerator.createArtifactStream(seed,
        aJenkinsArtifactStream()
            .withAppId(environment.getAppId())
            .withServiceId(service.getUuid())
            .withSourceName(HARNESS_JENKINS)
            .withJobname("harness-samples")
            .withArtifactPaths(asList("echo/target/echo.war"))
            .withSettingId(jenkins.getUuid())
            .build());

    Workflow workflow1 = workflowGenerator.createWorkflow(seed,
        aWorkflow()
            .withName("Basic - simplest")
            .withAppId(environment.getAppId())
            .withEnvId(environment.getUuid())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withServiceId(service.getUuid())
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build());

    Workflow workflow2 = workflowGenerator.createWorkflow(seed,
        aWorkflow()
            .withName("Basic - 5 nodes")
            .withAppId(environment.getAppId())
            .withEnvId(environment.getUuid())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withServiceId(service.getUuid())
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build());

    workflow2 = workflowGenerator.postProcess(workflow2, PostProcessInfo.builder().selectNodeCount(5).build());

    Pipeline pipeline = pipelineGenerator.createPipeline(seed,
        aPipeline()
            .withAppId(workflow1.getAppId())
            .withName("Pipeline")
            .withPipelineStages(
                asList(PipelineStage.builder()
                           .pipelineStageElements(asList(PipelineStageElement.builder()
                                                             .name("Simple")
                                                             .type(ENV_STATE.name())
                                                             .properties(ImmutableMap.of("envId", workflow1.getEnvId(),
                                                                 "workflowId", workflow1.getUuid()))
                                                             .build()))
                           .build(),
                    PipelineStage.builder()
                        .pipelineStageElements(
                            asList(PipelineStageElement.builder()
                                       .name("5 nodes")
                                       .type(ENV_STATE.name())
                                       .properties(ImmutableMap.of(
                                           "envId", workflow2.getEnvId(), "workflowId", workflow2.getUuid()))
                                       .build()))
                        .build()))
            .build());
  }

  private List<Application> createApplications() {
    List<Application> apps = new ArrayList<>();

    WebTarget target = client.target(API_BASE + "/apps?accountId=" + accountId);

    for (int i = 0; i < NUM_APPS; i++) {
      String name = getName(appNames);
      RestResponse<Application> response = getRequestBuilderWithAuthHeader(target).post(
          entity(
              anApplication().withName(name).withDescription(name).withAccountId(accountId).build(), APPLICATION_JSON),
          new GenericType<RestResponse<Application>>() {});
      assertThat(response.getResource()).isInstanceOf(Application.class);
      apps.add(response.getResource());
    }
    return apps;
  }

  private List<Service> addServices(String accountId, String appId, List<AppContainer> appContainers)
      throws IOException {
    serviceNames = new ArrayList<>(seedNames);
    WebTarget target = client.target(API_BASE + "/services/?appId=" + appId);
    List<Service> services = new ArrayList<>();

    for (int i = 0; i < NUM_SERVICES_PER_APP; i++) {
      String name = getName(serviceNames);
      Map<String, Object> serviceMap = new HashMap<>();
      serviceMap.put("name", name);
      serviceMap.put("description", randomText(40));
      serviceMap.put("appId", appId);
      serviceMap.put("artifactType", WAR.name());
      //      serviceMap.put("appContainer", appContainers.get(randomInt(0, appContainers.size()))); //TODO:: create
      //      service with tomcat/jboss family container type
      RestResponse<Service> response = getRequestBuilderWithAuthHeader(target).post(
          entity(serviceMap, APPLICATION_JSON), new GenericType<RestResponse<Service>>() { // FIXME
          });
      assertThat(response.getResource()).isInstanceOf(Service.class);
      String serviceId = response.getResource().getUuid();

      Service service = wingsPersistence.get(Service.class, serviceId);

      services.add(service);
      assertThat(service).isNotNull();

      configFileNames = new ArrayList<>(seedNames);
      addConfigFilesToEntity(service, DEFAULT_TEMPLATE_ID, NUM_CONFIG_FILE_PER_SERVICE, SERVICE);
    }
    return services;
  }

  private void addConfigFilesToEntity(
      Base entity, String templateId, int numConfigFilesToBeAdded, EntityType entityType) throws IOException {
    while (numConfigFilesToBeAdded > 0) {
      if (addOneConfigFileToEntity(entity.getAppId(), templateId, entity.getUuid(), entityType)) {
        numConfigFilesToBeAdded--;
      }
    }
  }

  private boolean addOneConfigFileToEntity(String appId, String templateId, String entityId, EntityType entityType)
      throws IOException {
    WebTarget target = client.target(format(API_BASE + "/configs/?appId=%s&entityId=%s&entityType=%s&templateId=%s",
        appId, entityId, entityType, templateId));
    File file = getTestFile(getName(configFileNames) + ".properties");
    FileDataBodyPart filePart = new FileDataBodyPart("file", file);
    FormDataMultiPart multiPart = new FormDataMultiPart()
                                      .field("fileName", file.getName())
                                      .field("name", file.getName())
                                      .field("relativeFilePath", "configs/" + file.getName());
    multiPart.bodyPart(filePart);
    Response response = getRequestBuilderWithAuthHeader(target).post(entity(multiPart, multiPart.getMediaType()));
    return response.getStatus() == 200;
  }

  private List<AppContainer> addAppContainers(String appId) {
    int containersToBeAdded = NUM_APP_CONTAINER_PER_APP;
    while (containersToBeAdded > 0) {
      if (addOneAppContainer(appId)) {
        containersToBeAdded--;
      }
    }
    return getAppContainers(appId);
  }

  private List<AppContainer> getAppContainers(String appId) {
    RestResponse<PageResponse<AppContainer>> response =
        getRequestBuilderWithAuthHeader(
            client.target(API_BASE + "/app-containers/?appId=" + appId + "&accountId=" + accountId))
            .get(new GenericType<RestResponse<PageResponse<AppContainer>>>() {});
    return response.getResource().getResponse();
  }

  private boolean addOneAppContainer(String appId) {
    WebTarget target = client.target(API_BASE + "/app-containers/?appId=" + appId + "&accountId=" + accountId);
    String version = format("%s.%s.%s", randomInt(10), randomInt(100), randomInt(1000));
    String name = containerNames.get(randomInt() % containerNames.size());

    try {
      File file = getTestFile(name);
      FileDataBodyPart filePart = new FileDataBodyPart("file", file);
      FormDataMultiPart multiPart = new FormDataMultiPart()
                                        .field("name", name)
                                        .field("version", version)
                                        .field("description", randomText(20))
                                        .field("sourceType", "FILE_UPLOAD")
                                        .field("standard", "false");
      multiPart.bodyPart(filePart);
      Response response = getRequestBuilderWithAuthHeader(target).post(entity(multiPart, multiPart.getMediaType()));
      return response.getStatus() == 200;
    } catch (IOException e) {
      log().info("Error occurred in uploading app container", e);
    }
    return false;
  }

  private List<Environment> addEnvs(String appId) throws IOException {
    List<Environment> environments =
        wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
    WebTarget target = client.target(API_BASE + "/environments?appId=" + appId);

    for (int i = 0; i < NUM_ENV_PER_APP; i++) {
      RestResponse<Environment> response = getRequestBuilderWithAuthHeader(target).post(
          entity(anEnvironment().withAppId(appId).withName(envNames.get(i)).withDescription(randomText(10)).build(),
              APPLICATION_JSON),
          new GenericType<RestResponse<Environment>>() {});
      assertThat(response.getResource()).isInstanceOf(Environment.class);
      environments.add(response.getResource());
    }
    // environments.forEach(this::addHostsToEnv);
    return environments;
  }

  private File getTestFile(String name) throws IOException {
    File file = new File(testFolder.getRoot().getAbsolutePath() + "/" + name);
    if (!file.isFile()) {
      file = testFolder.newFile(name);
    }
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write(randomText(100));
    out.close();
    return file;
  }

  private String getName(List<String> names) {
    int nameIdx = 0;
    String name = names.get(nameIdx);
    names.remove(nameIdx);
    return name;
  }

  private void addAndEnableKms() {
    kmsService.saveKmsConfig(accountId,
        KmsConfig.builder()
            .isDefault(true)
            .accountId(accountId)
            .name("Account_kms")
            .accessKey("AKIAIVRKRUMJ3LAVBMSQ")
            .secretKey("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")
            .kmsArn("arn:aws:kms:us-east-1:830767422336:key/6b64906a-b7ab-4f69-8159-e20fef1f204d")
            .build());
  }

  /**
   * Please do not remove the main class as UI test needs datagen as Jar
   * @param args
   */
  public static void main(String[] args) {
    logger.info("Running tests!");
    JUnitCore.main("software.wings.integration.DataGenUtil");
  }
}
