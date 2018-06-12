package software.wings.integration;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.common.Constants.HARNESS_NAME;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.generator.InfrastructureMappingGenerator.InfrastructureMappings.AWS_SSH_TEST;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_ARTIFACTORY_CONNECTOR;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_BAMBOO_CONNECTOR;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_DOCKER_REGISTRY;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_NEXU3_CONNECTOR;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_NEXUS_CONNECTOR;
import static software.wings.integration.IntegrationTestUtil.randomInt;
import static software.wings.integration.SeedData.containerNames;
import static software.wings.integration.SeedData.envNames;
import static software.wings.integration.SeedData.seedNames;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.sm.StateType.TERRAFORM_DESTROY;
import static software.wings.sm.StateType.TERRAFORM_PROVISION;
import static software.wings.utils.ArtifactType.WAR;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
import org.assertj.core.util.Lists;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.JUnitCore;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.app.DatabaseModule;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AppContainer;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.Base;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.License;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.SplunkConfig;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.security.UserGroup;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.generator.AccountGenerator;
import software.wings.generator.ApplicationGenerator;
import software.wings.generator.ArtifactStreamGenerator;
import software.wings.generator.ArtifactStreamGenerator.ArtifactStreams;
import software.wings.generator.EnvironmentGenerator;
import software.wings.generator.EnvironmentGenerator.Environments;
import software.wings.generator.InfrastructureMappingGenerator;
import software.wings.generator.InfrastructureMappingGenerator.InfrastructureMappings;
import software.wings.generator.InfrastructureProvisionerGenerator;
import software.wings.generator.InfrastructureProvisionerGenerator.InfrastructureProvisioners;
import software.wings.generator.LicenseGenerator;
import software.wings.generator.LicenseGenerator.Licenses;
import software.wings.generator.OwnerManager;
import software.wings.generator.OwnerManager.Owners;
import software.wings.generator.PipelineGenerator;
import software.wings.generator.Randomizer.Seed;
import software.wings.generator.ServiceGenerator;
import software.wings.generator.ServiceGenerator.Services;
import software.wings.generator.ServiceTemplateGenerator;
import software.wings.generator.SettingGenerator;
import software.wings.generator.WorkflowGenerator;
import software.wings.generator.WorkflowGenerator.PostProcessInfo;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.integration.setup.rest.AppResourceRestClient;
import software.wings.integration.setup.rest.EnvResourceRestClient;
import software.wings.integration.setup.rest.ServiceResourceRestClient;
import software.wings.integration.setup.rest.SettingsResourceRestClient;
import software.wings.integration.setup.rest.WorkflowResourceRestClient;
import software.wings.rules.SetupScheduler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SystemCatalogService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
  public static final String AWS_NON_PROD = "Aws non-prod";
  public static final String WINGS_KEY = "Wings Key";

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
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private AccountService accountService;
  @Inject private UserGroupService userGroupService;
  @Inject private UserService userService;

  @Inject private OwnerManager ownerManager;
  @Inject private AccountGenerator accountGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private ArtifactStreamGenerator artifactStreamGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureMappingGenerator infrastructureMappingGenerator;
  @Inject private InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;
  @Inject private LicenseGenerator licenseGenerator;
  @Inject private PipelineGenerator pipelineGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private ServiceTemplateGenerator serviceTemplateGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private SettingGenerator settingGenerator;

  @Inject private AppResourceRestClient appResourceRestClient;
  @Inject private ServiceResourceRestClient serviceResourceRestClient;
  @Inject private SettingsResourceRestClient settingsResourceRestClient;
  @Inject private EnvResourceRestClient envResourceRestClient;
  @Inject private WorkflowResourceRestClient workflowResourceRestClient;
  @Inject private UserResourceRestClient userResourceRestClient;

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
    enableRbac();
    learningEngineService.initializeServiceSecretKeys();

    createTestApplication(account);
    // createSeedEntries();
  }

  private void createSeedEntries() {
    userResourceRestClient.getUserToken(client);
    appResourceRestClient.getSeedApplication(client);
    serviceResourceRestClient.getSeedWarService(client);
    serviceResourceRestClient.getSeedDockerService(client);
    settingsResourceRestClient.seedDataCenter(client);
    settingsResourceRestClient.seedSshKey(client);
    envResourceRestClient.getSeedEnvironment(client);
    envResourceRestClient.getSeedFakeHostsDcInfra(client);
    workflowResourceRestClient.getSeedBasicWorkflow(client);
  }

  protected void dropDBAndEnsureIndexes() {
    wingsPersistence.getDatastore().getDB().dropDatabase();
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.mapPackage("software.wings");
    ensureIndex(morphia, wingsPersistence.getDatastore());
  }

  protected void ensureIndex(Morphia morphia, Datastore primaryDatastore) {
    /*
    Morphia auto creates embedded/nested Entity indexes with the parent Entity indexes.
    There is no way to override this behavior.
    https://github.com/mongodb/morphia/issues/706
     */

    morphia.getMapper().getMappedClasses().forEach(mc -> {
      if (mc.getEntityAnnotation() != null && !mc.isAbstract()) {
        // Read Entity level "Indexes" annotation
        List<Indexes> indexesAnnotations = mc.getAnnotations(Indexes.class);
        if (indexesAnnotations != null) {
          indexesAnnotations.stream().flatMap(indexes -> Arrays.stream(indexes.value())).forEach(index -> {
            DatabaseModule.reportDeprecatedUnique(index);

            BasicDBObject keys = new BasicDBObject();
            Arrays.stream(index.fields()).forEach(field -> keys.append(field.value(), 1));
            primaryDatastore.getCollection(mc.getClazz())
                .createIndex(keys, index.options().name(), index.options().unique());
          });
        }

        // Read field level "Indexed" annotation
        for (final MappedField mf : mc.getPersistenceFields()) {
          if (mf.hasAnnotation(Indexed.class)) {
            final Indexed indexed = mf.getAnnotation(Indexed.class);
            DatabaseModule.reportDeprecatedUnique(indexed);

            try {
              primaryDatastore.getCollection(mc.getClazz())
                  .createIndex(new BasicDBObject().append(mf.getNameToStore(), 1), null, indexed.options().unique());
            } catch (MongoCommandException mex) {
              logger.error("Index creation failed for class {}", mc.getClazz().getCanonicalName());
              throw mex;
            }
          }
        }
      }
    });
  }

  protected Account createLicenseAndDefaultUsers() {
    final Seed seed = new Seed(0);

    License license = licenseGenerator.ensurePredefined(seed, Licenses.TRIAL);

    Account account = wingsPersistence.createQuery(Account.class).get();
    boolean oldAccountExists = false;
    if (account == null) {
      account = Account.Builder.anAccount().build();
      account.setCompanyName("Harness");
      account.setAccountName("Harness");
    } else {
      oldAccountExists = true;
    }

    //    String oldAccountId = account.getUuid();
    String accountKey = delegateAccountSecret;
    account.setAccountKey(accountKey);
    account.setLicenseExpiryTime(-1);

    account.setUuid(defaultAccountId);
    accountId = defaultAccountId;
    if (oldAccountExists) {
      accountService.delete(account.getUuid());
    }

    accountService.save(account);

    // wingsPersistence.save(account);
    // Update account key to make delegate works
    UpdateOperations<Account> accountUpdateOperations = wingsPersistence.createUpdateOperations(Account.class);
    accountUpdateOperations.set("accountKey", accountKey);
    wingsPersistence.update(wingsPersistence.createQuery(Account.class), accountUpdateOperations);

    UpdateOperations<User> userUpdateOperations = wingsPersistence.createUpdateOperations(User.class);
    userUpdateOperations.set("accounts", Lists.newArrayList(account));
    wingsPersistence.update(wingsPersistence.createQuery(User.class), userUpdateOperations);

    UpdateOperations<Role> roleUpdateOperations = wingsPersistence.createUpdateOperations(Role.class);
    roleUpdateOperations.set("accountId", "kmpySmUISimoRrJL6NL73w");
    wingsPersistence.update(wingsPersistence.createQuery(Role.class), roleUpdateOperations);

    User adminUser = addUser(adminUserName, adminUserEmail, adminPassword, account);
    addUser(defaultUserName, defaultEmail, defaultPassword, account);
    User readOnlyUser = addUser(readOnlyUserName, readOnlyEmail, readOnlyPassword, account);

    addUserToUserGroup(adminUser, accountId, Constants.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME);
    UserGroup readOnlyUserGroup = authHandler.buildReadOnlyUserGroup(accountId, readOnlyUser, "ReadOnlyUserGroup");
    readOnlyUserGroup = wingsPersistence.saveAndGet(UserGroup.class, readOnlyUserGroup);

    addUserToUserGroup(readOnlyUser, readOnlyUserGroup);

    loginAdminUser();

    return account;
  }

  private void addUserToUserGroup(User user, String accountId, String userGroupName) {
    PageRequest<UserGroup> pageRequest = aPageRequest()
                                             .addFilter("accountId", Operator.EQ, accountId)
                                             .addFilter("name", Operator.EQ, userGroupName)
                                             .build();
    PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, true);
    UserGroup userGroup = pageResponse.get(0);
    userGroup.setMembers(asList(user));
    userGroupService.updateMembers(userGroup);
  }

  private void addUserToUserGroup(User user, UserGroup userGroup) {
    userGroup.setMembers(asList(user));
    userGroupService.updateMembers(userGroup);
  }

  private User addUser(String userName, String email, char[] password, Account account) {
    User user =
        anUser()
            .withName(userName)
            .withEmail(email)
            .withPassword(password)
            .withRoles(wingsPersistence
                           .query(Role.class,
                               aPageRequest().addFilter("roleType", Operator.EQ, RoleType.ACCOUNT_ADMIN).build())
                           .getResponse())
            .withAccountName(HARNESS_NAME)
            .withCompanyName(HARNESS_NAME)
            .build();
    User newUser = userService.registerNewUser(user, account);
    wingsPersistence.updateFields(User.class, newUser.getUuid(), ImmutableMap.of("emailVerified", true));

    return wingsPersistence.get(User.class, newUser.getUuid());
  }

  private void enableRbac() {
    FeatureFlag featureFlag =
        wingsPersistence.createQuery(FeatureFlag.class).filter("name", FeatureName.RBAC.name()).get();

    if (featureFlag == null) {
      featureFlag = FeatureFlag.builder().name(FeatureName.RBAC.name()).enabled(true).obsolete(false).build();
    } else {
      featureFlag.setEnabled(true);
      featureFlag.setObsolete(false);
    }
    wingsPersistence.save(featureFlag);
  }

  private void createGlobalSettings() {
    final Seed seed = new Seed(0);

    settingGenerator.ensurePredefined(seed, HARNESS_BAMBOO_CONNECTOR);
    settingGenerator.ensurePredefined(seed, HARNESS_NEXUS_CONNECTOR);
    settingGenerator.ensurePredefined(seed, HARNESS_NEXU3_CONNECTOR);
    settingGenerator.ensurePredefined(seed, HARNESS_ARTIFACTORY_CONNECTOR);
    settingGenerator.ensurePredefined(seed, HARNESS_DOCKER_REGISTRY);

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
                           .controllerUrl("https://harness-test.saas.appdynamics.com/controller")
                           .username("raghu@harness.io")
                           .accountname("harness-test")
                           .password("(idlk2e9idcs@ej".toCharArray())
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
                           .apiKey("d8d3da54ce9355bd39cb7ced542a8acd2c1672312711610".toCharArray())
                           .build())
            .build();

    wingsPersistence.save(newRelicSettingAttribute);

    SettingAttribute awsNonProdAttribute =
        aSettingAttribute()
            .withCategory(Category.CLOUD_PROVIDER)
            .withName(AWS_NON_PROD)
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(accountId)
            .withValue(AwsConfig.builder()
                           .accessKey("AKIAIKL7FYYF2TIYHCLQ")
                           .secretKey("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".toCharArray())
                           .accountId(accountId)
                           .build())
            .build();
    wingsPersistence.save(awsNonProdAttribute);

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
  }

  private void createTestApplication(Account account) {
    final Seed seed = new Seed(0);

    final Owners owners = ownerManager.create();

    Environment environment = environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST);
    owners.add(environment);

    Service service = serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST);
    owners.add(service);

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, AWS_SSH_TEST);

    ArtifactStream artifactStream =
        artifactStreamGenerator.ensurePredefined(seed, owners, ArtifactStreams.HARNESS_SAMPLE_ECHO_WAR);

    Workflow workflow1 = workflowGenerator.ensureWorkflow(seed, owners,
        aWorkflow()
            .withName("Basic - simple")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build());

    Workflow workflow2 = workflowGenerator.ensureWorkflow(seed, owners,
        aWorkflow()
            .withName("Basic - 10 nodes")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build());

    workflow2 = workflowGenerator.postProcess(workflow2, PostProcessInfo.builder().selectNodeCount(10).build());

    Workflow workflow3 = workflowGenerator.ensureWorkflow(seed, owners,
        aWorkflow()
            .withName("Barrier Parallel Section 2-1")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build());

    workflow3 = workflowGenerator.postProcess(workflow3, PostProcessInfo.builder().selectNodeCount(2).build());

    Workflow workflow4 = workflowGenerator.ensureWorkflow(seed, owners,
        aWorkflow()
            .withName("Barrier Parallel Section 2-2")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build());

    workflow4 = workflowGenerator.postProcess(workflow4, PostProcessInfo.builder().selectNodeCount(2).build());

    Pipeline pipeline1 = pipelineGenerator.createPipeline(seed,
        Pipeline.builder()
            .appId(workflow1.getAppId())
            .name("Barrier Pipeline")
            .pipelineStages(
                asList(PipelineStage.builder()
                           .pipelineStageElements(asList(PipelineStageElement.builder()
                                                             .name("Parallel section 1-1")
                                                             .type(ENV_STATE.name())
                                                             .properties(ImmutableMap.of("envId", workflow1.getEnvId(),
                                                                 "workflowId", workflow1.getUuid()))
                                                             .build()))
                           .build(),
                    PipelineStage.builder()
                        .parallel(true)
                        .pipelineStageElements(
                            asList(PipelineStageElement.builder()
                                       .name("Parallel section 1-2")
                                       .type(ENV_STATE.name())
                                       .properties(ImmutableMap.of(
                                           "envId", workflow2.getEnvId(), "workflowId", workflow2.getUuid()))
                                       .build()))
                        .build(),
                    PipelineStage.builder()
                        .pipelineStageElements(
                            asList(PipelineStageElement.builder()
                                       .name("Parallel section 2-1")
                                       .type(ENV_STATE.name())
                                       .properties(ImmutableMap.of(
                                           "envId", workflow3.getEnvId(), "workflowId", workflow3.getUuid()))
                                       .build()))
                        .build(),
                    PipelineStage.builder()
                        .parallel(true)
                        .pipelineStageElements(
                            asList(PipelineStageElement.builder()
                                       .name("Parallel section 2-2")
                                       .type(ENV_STATE.name())
                                       .properties(ImmutableMap.of(
                                           "envId", workflow4.getEnvId(), "workflowId", workflow4.getUuid()))
                                       .build()))
                        .build()

                        ))
            .build());

    Pipeline pipeline2 = pipelineGenerator.createPipeline(seed,
        Pipeline.builder()
            .appId(workflow1.getAppId())
            .name("Pipeline")
            .pipelineStages(
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

    final Owners terraformOwners = ownerManager.create();
    terraformOwners.add(environment);
    terraformOwners.add(service);
    final InfrastructureProvisioner infrastructureProvisioner = infrastructureProvisionerGenerator.ensurePredefined(
        seed, terraformOwners, InfrastructureProvisioners.TERRAFORM_TEST);

    final InfrastructureMapping terraformInfrastructureProvisioner = infrastructureMappingGenerator.ensurePredefined(
        seed, terraformOwners, InfrastructureMappings.TERRAFORM_AWS_SSH_TEST);

    Workflow workflow5 = workflowGenerator.ensureWorkflow(seed, terraformOwners,
        aWorkflow()
            .withName("Terraform provision")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(
                        aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT)
                            .addStep(aGraphNode()
                                         .withType(TERRAFORM_PROVISION.name())
                                         .withName("Provision infra")
                                         .addProperty("provisionerId", infrastructureProvisioner.getUuid())
                                         .build())
                            .build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT)
                            .addStep(aGraphNode()
                                         .withType(TERRAFORM_DESTROY.name())
                                         .withName("Deprovision infra")
                                         .addProperty("provisionerId", infrastructureProvisioner.getUuid())
                                         .build())
                            .build())
                    .build())
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
    List<Environment> environments = wingsPersistence.createQuery(Environment.class).filter("appId", appId).asList();
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

  /**
   * Please do not remove the main class as UI test needs datagen as Jar
   * @param args
   */
  public static void main(String[] args) {
    logger.info("Running tests!");
    JUnitCore.main("software.wings.integration.DataGenUtil");
  }
}
