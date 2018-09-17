package software.wings.integration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static software.wings.beans.AppContainer.Builder.anAppContainer;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SystemCatalog.Builder.aSystemCatalog;
import static software.wings.beans.SystemCatalog.CatalogType.APPSTACK;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.common.Constants.HARNESS_NAME;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.generator.InfrastructureMappingGenerator.InfrastructureMappings.AWS_SSH_TEST;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_ARTIFACTORY_CONNECTOR;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_BAMBOO_CONNECTOR;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_DOCKER_REGISTRY;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_GCP_EXPLORATION;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_NEXU3_CONNECTOR;
import static software.wings.generator.SettingGenerator.Settings.HARNESS_NEXUS_CONNECTOR;
import static software.wings.integration.IntegrationTestUtil.randomInt;
import static software.wings.integration.SeedData.containerNames;
import static software.wings.integration.SeedData.seedNames;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.sm.StateType.TERRAFORM_DESTROY;
import static software.wings.sm.StateType.TERRAFORM_PROVISION;
import static software.wings.utils.ContainerFamily.TOMCAT;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.JUnitCore;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.app.DatabaseModule;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.AppContainer;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.Base;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureName;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.License;
import software.wings.beans.LicenseInfo;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.Role;
import software.wings.beans.RoleType;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SystemCatalog;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.beans.security.HarnessUserGroup;
import software.wings.beans.security.UserGroup;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.generator.AccountGenerator;
import software.wings.generator.ApplicationGenerator;
import software.wings.generator.ArtifactStreamGenerator;
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
import software.wings.generator.PipelineGenerator.Pipelines;
import software.wings.generator.Randomizer.Seed;
import software.wings.generator.ResourceConstraintGenerator;
import software.wings.generator.ResourceConstraintGenerator.ResourceConstraints;
import software.wings.generator.ScmSecret;
import software.wings.generator.SecretGenerator;
import software.wings.generator.SecretName;
import software.wings.generator.ServiceGenerator;
import software.wings.generator.ServiceGenerator.Services;
import software.wings.generator.ServiceTemplateGenerator;
import software.wings.generator.SettingGenerator;
import software.wings.generator.WorkflowGenerator;
import software.wings.generator.WorkflowGenerator.Workflows;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.integration.setup.rest.AppResourceRestClient;
import software.wings.integration.setup.rest.EnvResourceRestClient;
import software.wings.integration.setup.rest.ServiceResourceRestClient;
import software.wings.integration.setup.rest.SettingsResourceRestClient;
import software.wings.integration.setup.rest.WorkflowResourceRestClient;
import software.wings.rules.SetupScheduler;
import software.wings.security.PermissionAttribute.Action;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SystemCatalogService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.utils.ArtifactType;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.ContainerFamily;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@SetupScheduler
public class DataGenUtil extends BaseIntegrationTest {
  private static final int NUM_APPS = 1; /* Max 1000 */
  private static final int NUM_APP_CONTAINER_PER_APP = 2; /* Max 1000 */
  private static final int NUM_SERVICES_PER_APP = 1; /* Max 1000 */
  private static final int NUM_CONFIG_FILE_PER_SERVICE = 2; /* Max 100  */
  private static final int NUM_ENV_PER_APP = 0; /* Max 6. 4 are created by default */
  private static final int NUM_TAG_GROUPS_PER_ENV = 3; /* Max 10   */
  private static final int TAG_HIERARCHY_DEPTH = 3; /* Max 10   */
  public static final String AWS_PLAY_GROUND = "aws-playground";
  public static final String WINGS_KEY = "Wings Key";

  @Inject SecretGenerator secretGenerator;

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
  @Inject private ResourceConstraintGenerator resourceConstraintGenerator;
  @Inject private PipelineGenerator pipelineGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private ServiceTemplateGenerator serviceTemplateGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private ScmSecret scmSecret;

  @Inject private AppResourceRestClient appResourceRestClient;
  @Inject private ServiceResourceRestClient serviceResourceRestClient;
  @Inject private SettingsResourceRestClient settingsResourceRestClient;
  @Inject private EnvResourceRestClient envResourceRestClient;
  @Inject private WorkflowResourceRestClient workflowResourceRestClient;
  @Inject private UserResourceRestClient userResourceRestClient;

  @Inject private AppContainerService appContainerService;
  @Inject private TemplateGalleryService templateGalleryService;
  @Inject private ExecutorService executorService;
  @Inject private ConfigService configService;
  @Inject private HarnessUserGroupService harnessUserGroupService;

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
    templateGalleryService.loadHarnessGallery();
    templateGalleryService.copyHarnessTemplates();

    Account account = createLicenseAndDefaultUsers();
    accountGenerator.setAccount(account);
    createGlobalSettings(account);

    List<Application> apps = createApplications();

    for (Application application : apps) {
      addAppContainers(application.getUuid());
      addServices(application.getAppId());
    }
    featureFlagService.initializeFeatureFlags();
    enableRbac();
    learningEngineService.initializeServiceSecretKeys();

    createTestApplication(account);
    // createSeedEntries();
    executorService.submit(() -> loadAppStackCatalogs());
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
            for (Field field : index.fields()) {
              keys.append(field.value(), 1);
            }
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
    LicenseInfo licenseInfo = new LicenseInfo();
    licenseInfo.setAccountType(AccountType.PAID);
    licenseInfo.setAccountStatus(AccountStatus.ACTIVE);
    licenseInfo.setExpiryTime(-1);
    account.setLicenseInfo(licenseInfo);

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
    addUser(rbac1UserName, rbac1Email, rbac1Password, account);
    addUser(rbac2UserName, rbac2Email, rbac2Password, account);
    addUserToUserGroup(adminUser, accountId, Constants.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME);
    UserGroup readOnlyUserGroup = authHandler.buildReadOnlyUserGroup(accountId, readOnlyUser, "ReadOnlyUserGroup");
    readOnlyUserGroup = wingsPersistence.saveAndGet(UserGroup.class, readOnlyUserGroup);

    addUserToUserGroup(readOnlyUser, readOnlyUserGroup);

    addUserToHarnessUserGroup(adminUser);

    //    loginAdminUser();

    return account;
  }

  private void addUserToUserGroup(User user, String accountId, String userGroupName) {
    PageRequest<UserGroup> pageRequest =
        aPageRequest().addFilter("accountId", EQ, accountId).addFilter("name", EQ, userGroupName).build();
    PageResponse<UserGroup> pageResponse = userGroupService.list(accountId, pageRequest, true);
    UserGroup userGroup = pageResponse.get(0);
    userGroup.setMembers(asList(user));
    userGroupService.updateMembers(userGroup);
  }

  private void addUserToUserGroup(User user, UserGroup userGroup) {
    userGroup.setMembers(asList(user));
    userGroupService.updateMembers(userGroup);
  }

  private void addUserToHarnessUserGroup(User user) {
    HarnessUserGroup harnessUserGroup = HarnessUserGroup.builder()
                                            .actions(Sets.newHashSet(Action.READ))
                                            .applyToAllAccounts(true)
                                            .memberIds(Sets.newHashSet(user.getUuid()))
                                            .name("harnessUserGroup")
                                            .build();
    harnessUserGroupService.save(harnessUserGroup);
  }

  private User addUser(String userName, String email, char[] password, Account account) {
    User user = anUser()
                    .withName(userName)
                    .withEmail(email)
                    .withPassword(password)
                    .withRoles(wingsPersistence
                                   .query(Role.class,
                                       aPageRequest()
                                           .addFilter(ACCOUNT_ID_KEY, EQ, account.getUuid())
                                           .addFilter("roleType", EQ, RoleType.ACCOUNT_ADMIN)
                                           .build())
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

  private void createGlobalSettings(Account account) {
    final Seed seed = new Seed(0);

    Owners owners = new Owners();
    owners.add(account);

    settingGenerator.ensurePredefined(seed, owners, HARNESS_BAMBOO_CONNECTOR);
    settingGenerator.ensurePredefined(seed, owners, HARNESS_NEXUS_CONNECTOR);
    settingGenerator.ensurePredefined(seed, owners, HARNESS_NEXU3_CONNECTOR);
    settingGenerator.ensurePredefined(seed, owners, HARNESS_ARTIFACTORY_CONNECTOR);
    settingGenerator.ensurePredefined(seed, owners, HARNESS_DOCKER_REGISTRY);
    settingGenerator.ensurePredefined(seed, owners, HARNESS_GCP_EXPLORATION);

    SettingAttribute smtpSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName("SMTP")
            .withAccountId(accountId)
            .withValue(SmtpConfig.builder()
                           .accountId(accountId)
                           .fromAddress("support@harness.io")
                           .username("support@harness.io")
                           .host("smtp.gmail.com")
                           .password(scmSecret.decryptToCharArray(new SecretName("smtp_config_password")))
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
                           .password(scmSecret.decryptToCharArray(new SecretName("splunk_config_password")))
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
                           .password(scmSecret.decryptToCharArray(new SecretName("appd_config_password")))
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
            .withName(AWS_PLAY_GROUND)
            .withAppId(GLOBAL_APP_ID)
            .withEnvId(GLOBAL_ENV_ID)
            .withAccountId(accountId)
            .withValue(AwsConfig.builder()
                           .accessKey(scmSecret.decryptToString(new SecretName("aws_playground_access_key")))
                           .secretKey(scmSecret.decryptToCharArray(new SecretName("aws_playground_secret_key")))
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
                           .withKey(scmSecret.decryptToCharArray(new SecretName("ubuntu_ssh_key")))
                           .build())
            .build();
    wingsPersistence.save(hostConnection);
  }

  private void createTestApplication(Account account) {
    final Seed seed = new Seed(0);

    final Owners owners = ownerManager.create();

    Environment environment =
        owners.obtainEnvironment(() -> environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST));

    Service service =
        owners.obtainService(() -> serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST));

    final ResourceConstraint asapResourceConstraint =
        resourceConstraintGenerator.ensurePredefined(seed, owners, ResourceConstraints.GENERIC_ASAP_TEST);
    final ResourceConstraint fifoResourceConstraint =
        resourceConstraintGenerator.ensurePredefined(seed, owners, ResourceConstraints.GENERIC_FIFO_TEST);

    pipelineGenerator.ensurePredefined(seed, owners, Pipelines.RESOURCE_CONSTRAINT_WORKFLOW);
    workflowGenerator.ensurePredefined(seed, owners, Workflows.PERMANENTLY_BLOCKED_RESOURCE_CONSTRAINT);

    Workflow workflow1 = workflowGenerator.ensurePredefined(seed, owners, Workflows.BASIC_SIMPLE);

    InfrastructureMapping infrastructureMapping =
        infrastructureMappingGenerator.ensurePredefined(seed, owners, AWS_SSH_TEST);

    Workflow workflow2 = workflowGenerator.ensurePredefined(seed, owners, Workflows.BASIC_10_NODES);

    Pipeline pipeline1 = pipelineGenerator.ensurePredefined(seed, owners, Pipelines.BARRIER);

    Pipeline pipeline2 = pipelineGenerator.ensurePipeline(seed, owners,
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

    final InfrastructureMapping terraformInfrastructureMapping = infrastructureMappingGenerator.ensurePredefined(
        seed, terraformOwners, InfrastructureMappings.TERRAFORM_AWS_SSH_TEST);

    final SecretName awsPlaygroundAccessKeyName = SecretName.builder().value("aws_playground_access_key").build();
    final String awsPlaygroundAccessKey = scmSecret.decryptToString(awsPlaygroundAccessKeyName);
    final SecretName awsPlaygroundSecretKeyName = SecretName.builder().value("aws_playground_secret_key").build();
    final String awsPlaygroundSecretKeyId = secretGenerator.ensureStored(terraformOwners, awsPlaygroundSecretKeyName);

    // TODO: this is temporary adding second key, to workaround bug in the UI
    final SecretName terraformPasswordName = SecretName.builder().value("terraform_password").build();
    final String terraformPasswordId = secretGenerator.ensureStored(terraformOwners, terraformPasswordName);

    Workflow workflow5 = workflowGenerator.ensureWorkflow(seed, terraformOwners,
        aWorkflow()
            .withName("Terraform provision")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withInfraMappingId(infrastructureMapping.getUuid())
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(
                        aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT)
                            .addStep(
                                aGraphNode()
                                    .withType(TERRAFORM_PROVISION.name())
                                    .withName("Provision infra")
                                    .addProperty("provisionerId", infrastructureProvisioner.getUuid())
                                    .addProperty("variables",
                                        asList(ImmutableMap.of("name", "access_key", "value", awsPlaygroundAccessKey,
                                                   "valueType", Type.TEXT.name()),
                                            ImmutableMap.of("name", "secret_key", "value", awsPlaygroundSecretKeyId,
                                                "valueType", Type.ENCRYPTED_TEXT.name())))
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

    // Build Workflow
    workflowGenerator.ensurePredefined(seed, owners, Workflows.BUILD);
    // Build Pipeline
    pipelineGenerator.ensurePredefined(seed, owners, Pipelines.BUILD);
  }

  private List<Application> createApplications() {
    List<Application> apps = new ArrayList<>();
    for (int i = 0; i < NUM_APPS; i++) {
      String name = getName(appNames);
      Application application =
          applicationGenerator.ensureApplication(anApplication().withAccountId(accountId).withName(name).build());
      assertThat(application).isNotNull();
      apps.add(application);
    }
    return apps;
  }

  private List<Service> addServices(String appId) throws IOException {
    serviceNames = new ArrayList<>(seedNames);
    List<Service> services = new ArrayList<>();

    for (int i = 0; i < NUM_SERVICES_PER_APP; i++) {
      String name = getName(serviceNames);
      Service service = serviceGenerator.ensureService(
          Service.builder().name(name).description(randomText(40)).appId(appId).artifactType(ArtifactType.WAR).build());
      assertThat(service).isNotNull();
      services.add(service);

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
    File file = getTestFile(getName(configFileNames) + ".properties");
    InputStream fileInputStream = new java.io.FileInputStream(file);
    BoundedInputStream uploadedInputStream =
        new BoundedInputStream(fileInputStream, configuration.getFileUploadLimits().getConfigFileLimit());

    ConfigFile configFile = new ConfigFile();
    configFile.setAccountId(accountId);
    configFile.setAppId(appId);
    configFile.setEntityId(entityId);
    configFile.setEntityType(entityType);
    configFile.setTemplateId(templateId);
    configFile.setRelativeFilePath("configs/" + file.getName());
    configFile.setFileName(file.getName());

    String key = configService.save(configFile, uploadedInputStream);
    return key != null;
  }

  private List<AppContainer> addAppContainers(String appId) {
    int containersToBeAdded = NUM_APP_CONTAINER_PER_APP;
    List<String> seedContainerNames = new ArrayList<>(containerNames);
    while (containersToBeAdded > 0) {
      if (addOneAppContainer(appId, seedContainerNames)) {
        containersToBeAdded--;
      }
    }
    return getAppContainers(appId);
  }

  private List<AppContainer> getAppContainers(String appId) {
    return wingsPersistence.createQuery(AppContainer.class)
        .filter(Base.APP_ID_KEY, appId)
        .filter(ACCOUNT_ID_KEY, accountId)
        .asList();
  }

  private boolean addOneAppContainer(String appId, List<String> containerNames) {
    String version = format("%s.%s.%s", randomInt(10), randomInt(100), randomInt(1000));
    String name = getName(containerNames);

    try {
      File file = getTestFile(name);
      InputStream fileInputStream = new java.io.FileInputStream(file);
      BoundedInputStream uploadedInputStream =
          new BoundedInputStream(fileInputStream, configuration.getFileUploadLimits().getAppContainerLimit());
      AppContainer appContainer = AppContainer.Builder.anAppContainer()
                                      .withAppId(appId)
                                      .withAccountId(accountId)
                                      .withVersion(version)
                                      .withName(name)
                                      .withFileName(file.getName())
                                      .withFamily(ContainerFamily.TOMCAT)
                                      .build();
      AppContainer savedAppContainer = appContainerService.save(appContainer, uploadedInputStream, PLATFORMS);
      return savedAppContainer != null;
    } catch (IOException e) {
      log().info("Error occurred in uploading app container", e);
    }
    return false;
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

  private static final String AWS_S3_CATALOG_TOMCAT7 =
      "https://s3.amazonaws.com/harness-catalogs/appstack/apache-tomcat-7.0.78.tar.gz";
  private static final String AWS_S3_CATALOG_TOMCAT8 =
      "https://s3.amazonaws.com/harness-catalogs/appstack/apache-tomcat-8.5.15.tar.gz";

  private static final String AWS_S3_CATALOG_TOMCAT7_HARDENED =
      "https://s3.amazonaws.com/harness-catalogs/appstack/apache-tomcat-7.0.78-hardened.tar.gz";
  private static final String AWS_S3_CATALOG_TOMCAT8_HARDENED =
      "https://s3.amazonaws.com/harness-catalogs/appstack/apache-tomcat-8.5.15-hardened.tar.gz";

  /**
   * Loads default application containers
   */
  public void loadAppStackCatalogs() {
    try {
      createOrUpdateSystemAppStackCatalogs();
      createOrUpdateSystemAppContainers();
    } catch (Exception e) {
      logger.error("Failed to load app stack catalogs", e);
    }
  }

  public void createOrUpdateSystemAppStackCatalogs() {
    long fileSize = configuration.getFileUploadLimits().getAppContainerLimit();

    List<SystemCatalog> systemCatalogs = systemCatalogService.list(aPageRequest()
                                                                       .addFilter("catalogType", EQ, APPSTACK)
                                                                       .addFilter("family", EQ, TOMCAT)
                                                                       .addFilter("appId", EQ, Base.GLOBAL_APP_ID)
                                                                       .build());

    Map<String, SystemCatalog> fileToSystemCatalog =
        systemCatalogs.stream().collect(Collectors.toMap(SystemCatalog::getFileName, Function.identity()));
    logger.info("Creating System App Stack Catalogs");
    // Create Tomcat 7 Standard
    SystemCatalog systemCatalog;
    if (!fileToSystemCatalog.containsKey("apache-tomcat-7.0.78.tar.gz")) {
      systemCatalog = aSystemCatalog()
                          .withCatalogType(APPSTACK)
                          .withName("Standard Tomcat 7")
                          .withFileName("apache-tomcat-7.0.78.tar.gz")
                          .withAppId(Base.GLOBAL_APP_ID)
                          .withFamily(TOMCAT)
                          .withNotes("System created.")
                          .withVersion("7.0.78")
                          .build();
      systemCatalogService.save(systemCatalog, AWS_S3_CATALOG_TOMCAT7, PLATFORMS, fileSize);
    } else {
      // call update --> Support the update
      systemCatalog = fileToSystemCatalog.get("apache-tomcat-7.0.78.tar.gz");
      systemCatalog.setVersion("7.0.78");
      systemCatalogService.update(systemCatalog, AWS_S3_CATALOG_TOMCAT7, PLATFORMS, fileSize);
    }
    if (!fileToSystemCatalog.containsKey("apache-tomcat-7.0.78-hardened.tar.gz")) {
      systemCatalog = aSystemCatalog()
                          .withCatalogType(APPSTACK)
                          .withName("Hardened Tomcat 7")
                          .withFileName("apache-tomcat-7.0.78-hardened.tar.gz")
                          .withAppId(Base.GLOBAL_APP_ID)
                          .withFamily(TOMCAT)
                          .withNotes("System created. Hardened Version")
                          .withVersion("7.0.78")
                          .withHardened(true)
                          .build();
      systemCatalogService.save(systemCatalog, AWS_S3_CATALOG_TOMCAT7_HARDENED, PLATFORMS, fileSize);
    } else {
      systemCatalog = fileToSystemCatalog.get("apache-tomcat-7.0.78-hardened.tar.gz");
      systemCatalog.setVersion("7.0.78");
      systemCatalog.setHardened(true);
      systemCatalog.setStackRootDirectory("apache-tomcat-7.0.78-hardened");
      systemCatalogService.update(systemCatalog, AWS_S3_CATALOG_TOMCAT7_HARDENED, PLATFORMS, fileSize);
    }
    if (!fileToSystemCatalog.containsKey("apache-tomcat-8.5.15.tar.gz")) {
      systemCatalog = aSystemCatalog()
                          .withCatalogType(APPSTACK)
                          .withName("Standard Tomcat 8")
                          .withFileName("apache-tomcat-8.5.15.tar.gz")
                          .withAppId(Base.GLOBAL_APP_ID)
                          .withFamily(TOMCAT)
                          .withNotes("System created.")
                          .withVersion("8.5.15")
                          .build();
      systemCatalogService.save(systemCatalog, AWS_S3_CATALOG_TOMCAT8, PLATFORMS, fileSize);
    } else {
      systemCatalog = fileToSystemCatalog.get("apache-tomcat-8.5.15.tar.gz");
      systemCatalog.setVersion("8.5.15");
      systemCatalogService.update(systemCatalog, AWS_S3_CATALOG_TOMCAT8, PLATFORMS, fileSize);
    }
    if (!fileToSystemCatalog.containsKey("apache-tomcat-8.5.15-hardened.tar.gz")) {
      systemCatalog = aSystemCatalog()
                          .withCatalogType(APPSTACK)
                          .withName("Hardened Tomcat 8")
                          .withFileName("apache-tomcat-8.5.15-hardened.tar.gz")
                          .withAppId(Base.GLOBAL_APP_ID)
                          .withFamily(TOMCAT)
                          .withNotes("System created. Hardened Version.")
                          .withVersion("8.5.15")
                          .withHardened(true)
                          .build();
      systemCatalogService.save(systemCatalog, AWS_S3_CATALOG_TOMCAT8_HARDENED, PLATFORMS, fileSize);
    } else {
      systemCatalog = fileToSystemCatalog.get("apache-tomcat-8.5.15-hardened.tar.gz");
      systemCatalog.setVersion("8.5.15");
      systemCatalog.setHardened(true);
      systemCatalog.setStackRootDirectory("apache-tomcat-8.5.15-hardened");
      systemCatalogService.update(systemCatalog, AWS_S3_CATALOG_TOMCAT8_HARDENED, PLATFORMS, fileSize);
    }
  }

  public void createOrUpdateSystemAppContainers() {
    logger.info("Creating System App Containers");
    List<Account> accounts =
        accountService.list(aPageRequest().withLimit(PageRequest.UNLIMITED).addFieldsIncluded("uuid").build());
    if (isEmpty(accounts)) {
      return;
    }
    List<SystemCatalog> systemCatalogs = systemCatalogService.list(aPageRequest()
                                                                       .addFilter("catalogType", EQ, APPSTACK)
                                                                       .addFilter("family", EQ, TOMCAT)
                                                                       .addFilter("appId", EQ, Base.GLOBAL_APP_ID)
                                                                       .build());
    accounts.forEach(account -> {
      for (SystemCatalog systemCatalog : systemCatalogs) {
        AppContainer appContainer = anAppContainer()
                                        .withAccountId(account.getUuid())
                                        .withAppId(systemCatalog.getAppId())
                                        .withChecksum(systemCatalog.getChecksum())
                                        .withChecksumType(systemCatalog.getChecksumType())
                                        .withFamily(systemCatalog.getFamily())
                                        .withStackRootDirectory(systemCatalog.getStackRootDirectory())
                                        .withFileName(systemCatalog.getFileName())
                                        .withFileUuid(systemCatalog.getFileUuid())
                                        .withFileType(systemCatalog.getFileType())
                                        .withSize(systemCatalog.getSize())
                                        .withName(systemCatalog.getName())
                                        .withSystemCreated(true)
                                        .withDescription(systemCatalog.getNotes())
                                        .withHardened(systemCatalog.isHardened())
                                        .withVersion(systemCatalog.getVersion())
                                        .build();
        try {
          PageResponse<AppContainer> pageResponse =
              appContainerService.list(aPageRequest()
                                           .addFilter("accountId", EQ, account.getUuid())
                                           .addFilter("fileUuid", EQ, systemCatalog.getFileUuid())
                                           .build());
          if (isEmpty(pageResponse.getResponse())) {
            appContainerService.save(appContainer);
          } else {
            AppContainer storedAppContainer = pageResponse.getResponse().get(0);
            storedAppContainer.setVersion(systemCatalog.getVersion());
            storedAppContainer.setHardened(systemCatalog.isHardened());
            storedAppContainer.setDescription(systemCatalog.getNotes());
            storedAppContainer.setFileName(systemCatalog.getFileName());
            storedAppContainer.setStackRootDirectory(systemCatalog.getStackRootDirectory());
            appContainerService.update(storedAppContainer);
          }
        } catch (Exception e) {
          logger.error("", e);
          logger.info("Error while creating system app container " + appContainer);
        }
      }
    });
    logger.info("System App Containers created successfully");
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
