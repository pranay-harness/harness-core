package io.harness;

import static io.harness.SeedData.randomText;
import static io.harness.SeedData.seedNames;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.generator.AccountGenerator.Accounts;
import static io.harness.generator.InfrastructureMappingGenerator.InfrastructureMappings.AWS_SSH_TEST;
import static io.harness.persistence.HPersistence.DEFAULT_STORE;
import static java.util.Arrays.asList;
import static software.wings.beans.AppContainer.Builder.anAppContainer;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SystemCatalog.Builder.aSystemCatalog;
import static software.wings.beans.SystemCatalog.CatalogType.APPSTACK;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.ContainerFamily.TOMCAT;
import static software.wings.utils.UsageRestrictionsUtil.getAllAppAllEnvUsageRestrictions;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoCommandException;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.generator.AccountGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.InfrastructureMappingGenerator;
import io.harness.generator.InfrastructureProvisionerGenerator;
import io.harness.generator.LicenseGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.PipelineGenerator;
import io.harness.generator.PipelineGenerator.Pipelines;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ResourceConstraintGenerator;
import io.harness.generator.ResourceConstraintGenerator.ResourceConstraints;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.SettingGenerator;
import io.harness.generator.WorkflowGenerator;
import io.harness.generator.WorkflowGenerator.Workflows;
import io.harness.generator.artifactstream.ArtifactStreamManager;
import io.harness.mongo.HObjectFactory;
import io.harness.mongo.IndexManagement;
import io.harness.persistence.ReadPref;
import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;
import io.harness.seeddata.SampleDataProviderService;
import org.junit.rules.TemporaryFolder;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.mapping.MappedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AppContainer;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.Base;
import software.wings.beans.NewRelicConfig;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.ServiceSecretKey;
import software.wings.beans.ServiceSecretKey.ServiceType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.SplunkConfig;
import software.wings.beans.SystemCatalog;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.HarnessUserGroupService;
import software.wings.service.intfc.LearningEngineService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SystemCatalogService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.settings.UsageRestrictions;
import software.wings.utils.ArtifactType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class DataGenService {
  private static final Logger logger = LoggerFactory.getLogger(DataGenService.class);
  private static final String NEW_RELIC_CONNECTOR_NAME = "NewRelic";
  private static final int NUM_APPS = 1; /* Max 1000 */
  private static final int NUM_APP_CONTAINER_PER_APP = 2; /* Max 1000 */
  private static final int NUM_SERVICES_PER_APP = 1; /* Max 1000 */
  public static final String AWS_PLAY_GROUND = "aws-playground";
  public static final String WINGS_KEY = "Wings Key";

  private static Random random = new Random();

  /**
   * The Test folder.
   */
  public TemporaryFolder testFolder = new TemporaryFolder();

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
  @Inject private ArtifactStreamManager artifactStreamManager;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private InfrastructureMappingGenerator infrastructureMappingGenerator;
  @Inject private InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;
  @Inject private LicenseGenerator licenseGenerator;
  @Inject private ResourceConstraintGenerator resourceConstraintGenerator;
  @Inject private PipelineGenerator pipelineGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private ScmSecret scmSecret;
  @Inject private LearningEngineService learningEngineService;
  @Inject protected AuthHandler authHandler;
  @Inject private AppContainerService appContainerService;
  @Inject private TemplateGalleryService templateGalleryService;
  @Inject private TemplateService templateService;
  @Inject private ExecutorService executorService;
  @Inject private ConfigService configService;
  @Inject private HarnessUserGroupService harnessUserGroupService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SampleDataProviderService sampleDataProviderService;

  protected String accountId = "INVALID_ID";
  protected String userToken = "INVALID_TOKEN";
  protected final int TIMES_TO_REPEAT = 3;
  protected final int SUCCESS_COUNT = 1;

  private static final String SAMPLE_ACCOUNT_ID = "kmpySmUISimoRrJL6NL73w";

  /**
   * Populate data.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void populateData() throws IOException {
    dropDBAndEnsureIndexes();
    templateGalleryService.loadHarnessGallery();
    templateGalleryService.copyHarnessTemplates();

    final Seed seed = new Seed(0);

    accountGenerator.ensurePredefined(seed, ownerManager.create(), Accounts.HARNESS_TEST);

    Account account = accountGenerator.ensurePredefined(seed, ownerManager.create(), Accounts.GENERIC_TEST);
    createGlobalSettings(account);
    createApps(account);
    createTestApplication(account);

    loadAppStackCatalogs();
  }

  private void createApps(Account account) {
    List<Application> apps = createApplications(account.getUuid());

    try {
      for (Application application : apps) {
        addServices(application.getAppId());
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage());
    }

    featureFlagService.initializeFeatureFlags();
    wingsPersistence.save(ServiceSecretKey.builder()
                              .serviceType(ServiceType.LEARNING_ENGINE)
                              .serviceSecret("67d9b94d9856665afc21acd3aa745401")
                              .build());
    learningEngineService.initializeServiceSecretKeys();
  }

  protected void dropDBAndEnsureIndexes() {
    wingsPersistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL).getDB().dropDatabase();
    Morphia morphia = new Morphia();
    morphia.getMapper().getOptions().setObjectFactory(new HObjectFactory());
    morphia.getMapper().getOptions().setMapSubPackages(true);
    morphia.mapPackage("software.wings");
    morphia.mapPackage("io.harness");
    ensureIndex(morphia, wingsPersistence.getDatastore(DEFAULT_STORE, ReadPref.NORMAL));
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
            IndexManagement.reportDeprecatedUnique(index);

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
            IndexManagement.reportDeprecatedUnique(indexed);

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

  private void createGlobalSettings(Account account) {
    final Seed seed = new Seed(0);
    String accountId = account.getUuid();
    Owners owners = new Owners();
    owners.add(account);

    settingGenerator.ensureAllPredefined(seed, owners);

    UsageRestrictions defaultUsageRestrictions = getAllAppAllEnvUsageRestrictions();

    SettingAttribute smtpSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName("SMTP")
            .withAccountId(account.getUuid())
            .withValue(SmtpConfig.builder()
                           .accountId(account.getUuid())
                           .fromAddress("systemsupport2@harness.io")
                           .username("systemsupport2@harness.io")
                           .host("smtp.gmail.com")
                           .password(scmSecret.decryptToCharArray(new SecretName("smtp_config_password")))
                           .port(465)
                           .useSSL(true)
                           .build())
            .withUsageRestrictions(defaultUsageRestrictions)
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
            .withUsageRestrictions(defaultUsageRestrictions)
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
            .withUsageRestrictions(defaultUsageRestrictions)
            .build();
    wingsPersistence.save(appdSettingAttribute);

    SettingAttribute newRelicSettingAttribute =
        aSettingAttribute()
            .withCategory(Category.CONNECTOR)
            .withName(NEW_RELIC_CONNECTOR_NAME)
            .withAccountId(accountId)
            .withValue(NewRelicConfig.builder()
                           .accountId(accountId)
                           .newRelicUrl("https://api.newrelic.com")
                           .apiKey("d8d3da54ce9355bd39cb7ced542a8acd2c1672312711610".toCharArray())
                           .build())
            .withUsageRestrictions(defaultUsageRestrictions)
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
            .withUsageRestrictions(defaultUsageRestrictions)
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
            .withUsageRestrictions(defaultUsageRestrictions)
            .build();
    wingsPersistence.save(hostConnection);
  }

  private void createTestApplication(Account account) {
    final Seed seed = new Seed(0);

    final Owners owners = ownerManager.create();

    owners.obtainEnvironment(() -> environmentGenerator.ensurePredefined(seed, owners, Environments.GENERIC_TEST));

    owners.obtainService(() -> serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST));

    resourceConstraintGenerator.ensurePredefined(seed, owners, ResourceConstraints.GENERIC_ASAP_TEST);
    resourceConstraintGenerator.ensurePredefined(seed, owners, ResourceConstraints.GENERIC_FIFO_TEST);

    pipelineGenerator.ensurePredefined(seed, owners, Pipelines.RESOURCE_CONSTRAINT_WORKFLOW);
    workflowGenerator.ensurePredefined(seed, owners, Workflows.PERMANENTLY_BLOCKED_RESOURCE_CONSTRAINT);

    Workflow workflow1 = workflowGenerator.ensurePredefined(seed, owners, Workflows.BASIC_SIMPLE);

    infrastructureMappingGenerator.ensurePredefined(seed, owners, AWS_SSH_TEST);

    Workflow workflow2 = workflowGenerator.ensurePredefined(seed, owners, Workflows.BASIC_10_NODES);

    pipelineGenerator.ensurePredefined(seed, owners, Pipelines.BARRIER);

    pipelineGenerator.ensurePipeline(seed, owners,
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
                        .pipelineStageElements(asList(PipelineStageElement.builder()
                                                          .name("5 nodes")
                                                          .type(ENV_STATE.name())
                                                          .properties(ImmutableMap.of("envId", workflow2.getEnvId(),
                                                              "workflowId", workflow2.getUuid()))
                                                          .build()))
                        .build()))
            .build());

    final Owners terraformOwners = ownerManager.create();
    workflowGenerator.ensurePredefined(seed, terraformOwners, Workflows.TERRAFORM);
  }

  private List<Application> createApplications(String accountId) {
    List<Application> apps = new ArrayList<>();
    for (int i = 0; i < NUM_APPS; i++) {
      String name = getName(appNames);
      Application application =
          applicationGenerator.ensureApplication(anApplication().withAccountId(accountId).withName(name).build());
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
      services.add(service);
    }
    return services;
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
      systemCatalog.setAccountId(GLOBAL_ACCOUNT_ID);
      systemCatalogService.save(systemCatalog, AWS_S3_CATALOG_TOMCAT7, PLATFORMS, fileSize);
    } else {
      // call update --> Support the update
      systemCatalog = fileToSystemCatalog.get("apache-tomcat-7.0.78.tar.gz");
      systemCatalog.setVersion("7.0.78");
      systemCatalogService.update(systemCatalog, AWS_S3_CATALOG_TOMCAT7, PLATFORMS, fileSize);
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
  public static void main(String[] args) throws IOException {
    logger.info("Running tests!");
    new DataGenService().populateData();
  }
}
