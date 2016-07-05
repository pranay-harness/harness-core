package software.wings.integration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.shuffle;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Artifact.Builder.anArtifact;
import static software.wings.beans.ArtifactSource.ArtifactType.WAR;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.JenkinsConfig.Builder.aJenkinsConfig;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Orchestration.Builder.anOrchestration;
import static software.wings.beans.Pipeline.Builder.aPipeline;
import static software.wings.beans.Release.ReleaseBuilder.aRelease;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.helpers.ext.mail.SmtpConfig.Builder.aSmtpConfig;
import static software.wings.integration.IntegrationTestUtil.randomInt;
import static software.wings.integration.SeedData.containerNames;
import static software.wings.integration.SeedData.envNames;
import static software.wings.integration.SeedData.randomSeedString;
import static software.wings.integration.SeedData.seedNames;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.utils.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Status;
import software.wings.beans.AppContainer;
import software.wings.beans.Application;
import software.wings.beans.Artifact;
import software.wings.beans.Base;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.Environment;
import software.wings.beans.Graph;
import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.beans.Orchestration;
import software.wings.beans.Pipeline;
import software.wings.beans.Release;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.utils.Misc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/6/16.
 */
@Integration
public class DataGenUtil extends WingsBaseTest {
  private static final int NUM_APPS = 1; /* Max 1000 */
  private static final int NUM_APP_CONTAINER_PER_APP = 2; /* Max 1000 */
  private static final int NUM_SERVICES_PER_APP = 5; /* Max 1000 */
  private static final int NUM_CONFIG_FILE_PER_SERVICE = 2; /* Max 100  */
  private static final int NUM_ENV_PER_APP = 0; /* Max 6. 4 are created by default */
  private static final int NUM_HOSTS_PER_INFRA = 5; /* No limit */
  private static final int NUM_TAG_GROUPS_PER_ENV = 3; /* Max 10   */
  private static final int TAG_HIERARCHY_DEPTH = 3; /* Max 10   */
  private static final String API_BASE = "http://localhost:9090/api";

  private static String userToken = "INVALID_TOKEN";
  private final Logger logger = LoggerFactory.getLogger(getClass());
  /**
   * The Test folder.
   */
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();
  private Client client;
  @Inject private WingsPersistence wingsPersistence;
  private List<String> appNames = new ArrayList<String>(seedNames);
  private List<String> serviceNames;
  private List<String> configFileNames;
  private SettingAttribute envAttr = null;
  @Inject private WorkflowService workflowService;

  /**
   * Generated Data for across the API use.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    assertThat(NUM_APPS).isBetween(1, 1000);
    assertThat(NUM_APP_CONTAINER_PER_APP).isBetween(1, 1000);
    assertThat(NUM_SERVICES_PER_APP).isBetween(1, 1000);
    assertThat(NUM_CONFIG_FILE_PER_SERVICE).isBetween(0, 100);
    assertThat(NUM_ENV_PER_APP).isBetween(0, 10);
    assertThat(NUM_TAG_GROUPS_PER_ENV).isBetween(1, 10);
    assertThat(TAG_HIERARCHY_DEPTH).isBetween(1, 10);

    dropDBAndEnsureIndexes();

    ClientConfig config = new ClientConfig(new JacksonJsonProvider().configure(FAIL_ON_UNKNOWN_PROPERTIES, false));
    config.register(MultiPartWriter.class);
    client = ClientBuilder.newClient(config);
  }

  private void dropDBAndEnsureIndexes() throws IOException, ClassNotFoundException {
    wingsPersistence.getDatastore().getDB().dropDatabase();
    for (final Class clazz : ReflectionUtils.getClasses("software.wings.beans", false)) {
      final Embedded embeddedAnn = ReflectionUtils.getClassEmbeddedAnnotation(clazz);
      final org.mongodb.morphia.annotations.Entity entityAnn = ReflectionUtils.getClassEntityAnnotation(clazz);
      final boolean isAbstract = Modifier.isAbstract(clazz.getModifiers());
      if ((entityAnn != null || embeddedAnn != null) && !isAbstract) {
        wingsPersistence.getDatastore().ensureIndexes(clazz);
      }
    }
  }

  /**
   * Populate data.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void populateData() throws IOException {
    createGlobalSettings();
    addAdminUser();

    List<Application> apps = createApplications();
    Map<String, List<AppContainer>> containers = new HashMap<>();
    Map<String, List<Service>> services = new HashMap<>();
    Map<String, List<Environment>> appEnvs = new HashMap<>();

    for (Application application : apps) {
      containers.put(application.getUuid(), addAppContainers(application.getUuid()));
      services.put(application.getUuid(), addServices(application.getUuid(), containers.get(application.getUuid())));
      appEnvs.put(application.getUuid(), addEnvs(application.getUuid()));
      addServiceInstances(services.get(application.getUuid()), appEnvs.get(application.getUuid()));
      addActivitiesAndLogs(application, services.get(application.getUuid()), appEnvs.get(application.getUuid()));
      addOrchestrationAndPipeline(services, appEnvs, application);
    }
  }

  private void addOrchestrationAndPipeline(
      Map<String, List<Service>> services, Map<String, List<Environment>> appEnvs, Application application) {
    Map<String, String> envWorkflowMap = addOrchestrationAndPipeline(
        application, services.get(application.getUuid()), appEnvs.get(application.getUuid()));
    Pipeline pipeline = addPipeline(
        application, services.get(application.getUuid()), appEnvs.get(application.getUuid()), envWorkflowMap);
    addPipelineExecution(application, pipeline);
    addPipelineExecution(application, pipeline);
    addPipelineExecution(application, pipeline);
    addPipelineExecution(application, pipeline);
    addPipelineExecution(application, pipeline);
  }

  private void addPipelineExecution(Application application, Pipeline pipeline) {
    WorkflowExecution execution = workflowService.triggerPipelineExecution(application.getUuid(), pipeline.getUuid());
    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Pipeline executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    Misc.quietSleep(2000);
    execution = workflowService.getExecutionDetails(application.getUuid(), executionId);
    assertThat(execution)
        .isNotNull()
        .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
        .containsExactly(executionId, ExecutionStatus.SUCCESS);
  }

  private Pipeline addPipeline(Application application, List<Service> services, List<Environment> environments,
      Map<String, String> envWorkflowMap) {
    int x = 80;
    software.wings.beans.Graph.Builder graphBuilder =
        aGraph().addNodes(aNode().withId("n0").withName("ORIGIN").withX(x).withY(80).withType("ORIGIN").build());

    x += 200;
    graphBuilder
        .addNodes(aNode().withId("n1").withName("build").withX(x).withY(80).withType(StateType.BUILD.name()).build())
        .addLinks(aLink().withId("l0").withFrom("n0").withTo("n1").withType("success").build());

    String fromNode = "n1";
    for (Environment env : environments) {
      String toNode = "n-" + env.getName();
      x += 200;
      graphBuilder
          .addNodes(aNode()
                        .withId(toNode)
                        .withName(env.getName())
                        .withX(x)
                        .withY(80)
                        .withType(StateType.ENV_STATE.name())
                        .addProperty("envId", env.getUuid())
                        .addProperty("workflowId", envWorkflowMap.get(env.getUuid()))
                        .build())
          .addLinks(aLink().withId("l-" + env.getName()).withFrom(fromNode).withTo(toNode).withType("success").build());
      fromNode = toNode;
    }

    Graph graph = graphBuilder.build();
    Pipeline pipeline =
        aPipeline()
            .withAppId(application.getUuid())
            .withName("pipeline1")
            .withDescription("Sample Pipeline")
            .addServices(services.stream().map(Service::getUuid).collect(Collectors.toList()).toArray(new String[0]))
            .withGraph(graph)
            .build();

    pipeline = workflowService.createWorkflow(Pipeline.class, pipeline);
    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getUuid()).isNotNull();

    return pipeline;
  }

  private Map<String, String> addOrchestrationAndPipeline(
      Application application, List<Service> services, List<Environment> environments) {
    Graph graph =
        aGraph()
            .addNodes(aNode().withId("n0").withName("ORIGIN").withX(50).withY(80).withType("ORIGIN").build())
            .addNodes(
                aNode().withId("n1").withName("stop").withX(200).withY(80).withType(StateType.ENV_STATE.name()).build())
            .addNodes(aNode()
                          .withId("n2")
                          .withName("wait")
                          .withX(360)
                          .withY(80)
                          .withType(StateType.WAIT.name())
                          .addProperty("duration", 5000l)
                          .build())
            .addNodes(aNode()
                          .withId("n3")
                          .withName("start")
                          .withX(530)
                          .withY(80)
                          .withType(StateType.ENV_STATE.name())
                          .build())
            .addLinks(aLink().withId("l0").withFrom("n0").withTo("n1").withType("success").build())
            .addLinks(aLink().withId("l1").withFrom("n1").withTo("n2").withType("success").build())
            .addLinks(aLink().withId("l2").withFrom("n2").withTo("n3").withType("success").build())
            .build();

    Map<String, String> envWorkflowMap = new HashMap<>();
    environments.forEach(env -> {
      Orchestration orchestration = anOrchestration()
                                        .withAppId(application.getUuid())
                                        .withName("workflow-" + env.getName())
                                        .withDescription("Sample Workflow for " + env.getName() + " environment")
                                        .withEnvironment(env)
                                        .withGraph(graph)
                                        .build();
      orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
      assertThat(orchestration).isNotNull();
      assertThat(orchestration.getUuid()).isNotNull();
      envWorkflowMap.put(env.getUuid(), orchestration.getUuid());
    });
    return envWorkflowMap;
  }

  private void addAdminUser() {
    String userName = "admin@wings.software";
    String password = "admin";
    String basicAuthValue = "Basic " + encodeBase64String(format("%s:%s", userName, password).getBytes());
    WebTarget target = client.target(API_BASE + "/users/");
    RestResponse<User> response = target.request().post(
        Entity.entity(anUser().withName("Admin").withEmail(userName).withPassword(password).build(), APPLICATION_JSON),
        new GenericType<RestResponse<User>>() {});
    assertThat(response.getResource()).isInstanceOf(User.class);
    wingsPersistence.updateFields(User.class, response.getResource().getUuid(), ImmutableMap.of("emailVerified", true));
    response = client.target(API_BASE + "/users/login")
                   .request()
                   .header("Authorization", basicAuthValue)
                   .get(new GenericType<RestResponse<User>>() {});
    if (response.getResource() != null) {
      userToken = response.getResource().getToken();
    }
  }

  private Builder getRequestWithAuthHeader(WebTarget target) {
    return target.request().header("Authorization", "Bearer " + userToken);
  }

  private void addServiceInstances(List<Service> services, List<Environment> appEnvs) {
    // TODO: improve make http calls and use better generation scheme
    services.forEach(service -> {
      appEnvs.forEach(environment -> {
        String infraId =
            wingsPersistence.createQuery(Infra.class).field("envId").equal(environment.getUuid()).get().getUuid();
        List<Host> hosts = wingsPersistence.createQuery(Host.class)
                               .field("appId")
                               .equal(environment.getAppId())
                               .field("infraId")
                               .equal(infraId)
                               .asList();
        Release release = wingsPersistence.saveAndGet(Release.class, aRelease().withReleaseName("Rel1.1").build());
        Artifact artifact =
            wingsPersistence.saveAndGet(Artifact.class, anArtifact().withDisplayName("Build_02_16_10AM").build());
        ServiceTemplate template = wingsPersistence.createQuery(ServiceTemplate.class)
                                       .field("appId")
                                       .equal(environment.getAppId())
                                       .field("envId")
                                       .equal(environment.getUuid())
                                       .get();

        hosts.forEach(host
            -> wingsPersistence.save(aServiceInstance()
                                         .withAppId(host.getAppId())
                                         .withEnvId(environment.getUuid())
                                         .withHost(host)
                                         .withServiceTemplate(template)
                                         .withRelease(release)
                                         .withArtifact(artifact)
                                         .build()));
      });
    });
  }

  private void addActivitiesAndLogs(Application application, List<Service> services, List<Environment> appEnvs) {
    // TODO: improve make http calls and use better generation scheme
    appEnvs.forEach(environment -> {
      String infraId =
          wingsPersistence.createQuery(Infra.class).field("envId").equal(environment.getUuid()).get().getUuid();
      List<Host> hosts = wingsPersistence.createQuery(Host.class)
                             .field("appId")
                             .equal(environment.getAppId())
                             .field("infraId")
                             .equal(infraId)
                             .asList();
      ServiceTemplate template = wingsPersistence.query(ServiceTemplate.class, new PageRequest<>()).get(0);
      template.setService(services.get(0));
      Release release = wingsPersistence.query(Release.class, new PageRequest<>()).get(0);
      Artifact artifact = wingsPersistence.query(Artifact.class, new PageRequest<>()).get(0);

      shuffle(hosts);
      createDeployActivity(application, environment, template, hosts.get(0), release, artifact, Status.RUNNING);
      createStartActivity(application, environment, template, hosts.get(1), Status.COMPLETED);
      createStopActivity(application, environment, template, hosts.get(2), Status.FAILED);
      createDeployActivity(application, environment, template, hosts.get(0), release, artifact, Status.ABORTED);
    });
  }

  private void createStopActivity(
      Application application, Environment environment, ServiceTemplate template, Host host, Status status) {
    Activity activity = wingsPersistence.saveAndGet(Activity.class,
        anActivity()
            .withAppId(application.getUuid())
            .withCommandType("COMMAND")
            .withCommandName("STOP")
            .withEnvironmentId(environment.getUuid())
            .withHostName(host.getHostName())
            .withServiceId(template.getService().getUuid())
            .withServiceName(template.getService().getName())
            .withServiceTemplateId(template.getUuid())
            .withServiceTemplateName(template.getName())
            .withStatus(status)
            .build());

    addLogLine(application, template, host, activity,
        "------ deploying to " + host.getHostName() + ":" + template.getName() + " -------");
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO connecting to " + host.getHostName());
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO starting tomcat ./bin/startup.sh");
  }

  private void createStartActivity(
      Application application, Environment environment, ServiceTemplate template, Host host, Status status) {
    Activity activity = wingsPersistence.saveAndGet(Activity.class,
        anActivity()
            .withAppId(application.getUuid())
            .withCommandType("COMMAND")
            .withCommandName("START")
            .withEnvironmentId(environment.getUuid())
            .withHostName(host.getHostName())
            .withServiceId(template.getService().getUuid())
            .withServiceName(template.getService().getName())
            .withServiceTemplateId(template.getUuid())
            .withServiceTemplateName(template.getName())
            .withStatus(status)
            .build());

    addLogLine(application, template, host, activity,
        "------ deploying to " + host.getHostName() + ":" + template.getName() + " -------");
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO connecting to " + host.getHostName());
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO starting tomcat ./bin/startup.sh");
  }

  private void createDeployActivity(Application application, Environment environment, ServiceTemplate template,
      Host host, Release release, Artifact artifact, Status status) {
    Activity activity = wingsPersistence.saveAndGet(Activity.class,
        anActivity()
            .withAppId(application.getUuid())
            .withArtifactName(artifact.getDisplayName())
            .withCommandType("COMMAND")
            .withCommandName("DEPLOY")
            .withEnvironmentId(environment.getUuid())
            .withHostName(host.getHostName())
            .withReleaseName(release.getReleaseName())
            .withReleaseId(release.getUuid())
            .withServiceId(template.getService().getUuid())
            .withServiceName(template.getService().getName())
            .withServiceTemplateId(template.getUuid())
            .withServiceTemplateName(template.getName())
            .withStatus(status)
            .build());

    addLogLine(application, template, host, activity,
        "------ deploying to " + host.getHostName() + ":" + template.getName() + " -------");
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO connecting to " + host.getHostName());
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO stopping tomcat ./bin/shutdown.sh");
    addLogLine(application, template, host, activity,
        getTimeStamp() + "INFO copying artifact artifact.war to stating /home/staging");
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO untar artifact to /home/tomcat");
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO starting tomcat ./bin/startup.sh");
  }

  private void addLogLine(
      Application application, ServiceTemplate template, Host host, Activity activity, String logLine) {
    wingsPersistence.save(aLog()
                              .withAppId(application.getUuid())
                              .withActivityId(activity.getUuid())
                              .withHostName(host.getHostName())
                              .withLogLine(logLine)
                              .build());
  }

  private String getTimeStamp() {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    df.setTimeZone(tz);
    return df.format(new Date());
  }

  private void createGlobalSettings() {
    WebTarget target = client.target(API_BASE + "/settings/?appId=" + Base.GLOBAL_APP_ID);
    getRequestWithAuthHeader(target).post(
        Entity.entity(aSettingAttribute()
                          .withName("Wings Jenkins")
                          .withValue(aJenkinsConfig()
                                         .withJenkinsUrl("https://jenkins-wingssoftware.rhcloud.com")
                                         .withUsername("admin")
                                         .withPassword("W!ngs")
                                         .build())
                          .build(),
            APPLICATION_JSON),
        new GenericType<RestResponse<SettingAttribute>>() {});
    getRequestWithAuthHeader(target).post(Entity.entity(aSettingAttribute()
                                                            .withName("SMTP")
                                                            .withValue(aSmtpConfig()
                                                                           .withFromAddress("wings_test@wings.software")
                                                                           .withUsername("wings_test@wings.software")
                                                                           .withHost("smtp.gmail.com")
                                                                           .withPassword("@wes0me@pp")
                                                                           .withPort(465)
                                                                           .withUseSSL(true)
                                                                           .build())
                                                            .build(),
                                              APPLICATION_JSON),
        new GenericType<RestResponse<SettingAttribute>>() {});
  }

  private List<Application> createApplications() {
    List<Application> apps = new ArrayList<>();

    WebTarget target = client.target(API_BASE + "/apps/");

    for (int i = 0; i < NUM_APPS; i++) {
      String name = getName(appNames);
      RestResponse<Application> response = getRequestWithAuthHeader(target).post(
          Entity.entity(anApplication().withName(name).withDescription(name).build(), APPLICATION_JSON),
          new GenericType<RestResponse<Application>>() {});
      assertThat(response.getResource()).isInstanceOf(Application.class);
      apps.add(response.getResource());
    }
    return apps;
  }

  private List<Service> addServices(String appId, List<AppContainer> appContainers) throws IOException {
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
      serviceMap.put("appContainer", appContainers.get(randomInt(0, appContainers.size())));
      RestResponse<Base> response = getRequestWithAuthHeader(target).post(
          Entity.entity(serviceMap, APPLICATION_JSON), new GenericType<RestResponse<Base>>() { // FIXME
          });
      //      assertThat(response.getResource()).isInstanceOf(Service.class);
      String serviceId = response.getResource().getUuid();
      Service service = wingsPersistence.get(Service.class, serviceId);
      services.add(service);
      assertThat(service).isNotNull();

      configFileNames = new ArrayList<>(seedNames);
      addConfigFilesToEntity(service, DEFAULT_TEMPLATE_ID, NUM_CONFIG_FILE_PER_SERVICE);
    }
    return services;
  }

  private void addConfigFilesToEntity(Base entity, String templateId, int numConfigFilesToBeAdded) throws IOException {
    while (numConfigFilesToBeAdded > 0) {
      if (addOneConfigFileToEntity(entity.getAppId(), templateId, entity.getUuid())) {
        numConfigFilesToBeAdded--;
      }
    }
  }

  private boolean addOneConfigFileToEntity(String appId, String templateId, String entityId) throws IOException {
    WebTarget target =
        client.target(format(API_BASE + "/configs/?appId=%s&entityId=%s&templateId=%s", appId, entityId, templateId));
    File file = getTestFile(getName(configFileNames) + ".properties");
    FileDataBodyPart filePart = new FileDataBodyPart("file", file);
    FormDataMultiPart multiPart =
        new FormDataMultiPart().field("name", file.getName()).field("relativePath", "./configs/");
    multiPart.bodyPart(filePart);
    Response response = getRequestWithAuthHeader(target).post(Entity.entity(multiPart, multiPart.getMediaType()));
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
        getRequestWithAuthHeader(client.target(API_BASE + "/app-containers/?appId=" + appId))
            .get(new GenericType<RestResponse<PageResponse<AppContainer>>>() {});
    return response.getResource().getResponse();
  }

  private boolean addOneAppContainer(String appId) {
    WebTarget target = client.target(API_BASE + "/app-containers/?appId=" + appId);
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
      Response response = getRequestWithAuthHeader(target).post(Entity.entity(multiPart, multiPart.getMediaType()));
      return response.getStatus() == 200;
    } catch (IOException e) {
      log().info("Error occured in uploading app container" + e.getMessage());
    }
    return false;
  }

  private List<Environment> addEnvs(String appId) throws IOException {
    List<Environment> environments = new ArrayList<>();
    WebTarget target = client.target(API_BASE + "/environments?appId=" + appId);
    for (int i = 0; i < NUM_ENV_PER_APP; i++) {
      RestResponse<Environment> response = getRequestWithAuthHeader(target).post(
          Entity.entity(
              anEnvironment().withAppId(appId).withName(envNames.get(i)).withDescription(randomText(10)).build(),
              APPLICATION_JSON),
          new GenericType<RestResponse<Environment>>() {});
      assertThat(response.getResource()).isInstanceOf(Environment.class);
      environments.add(response.getResource());
      addHostsToEnv(response.getResource());
      //      createAndTagHosts(response.getResource());
    }
    return environments;
  }

  private void createAndTagHosts(Environment environment) {
    RestResponse<PageResponse<Tag>> response = getRequestWithAuthHeader(
        client.target(format(API_BASE + "/tag-types?appId=%s&envId=%s", environment.getAppId(), environment.getUuid())))
                                                   .get(new GenericType<RestResponse<PageResponse<Tag>>>() {});
    log().info(response.getResource().getResponse().toString());
  }

  private void addHostsToEnv(Environment env) throws IOException {
    if (envAttr == null) {
      envAttr = wingsPersistence.saveAndGet(SettingAttribute.class,
          aSettingAttribute().withAppId(env.getAppId()).withValue(new BastionConnectionAttributes()).build());
    }

    WebTarget target = client.target(format(API_BASE + "/hosts?appId=%s&envId=%s", env.getAppId(), env.getUuid()));

    List<SettingAttribute> connectionAttributes = wingsPersistence.createQuery(SettingAttribute.class)
                                                      .field("appId")
                                                      .equal(env.getAppId())
                                                      .field("value.type")
                                                      .equal(HOST_CONNECTION_ATTRIBUTES)
                                                      .asList();

    for (int i = 1; i <= NUM_HOSTS_PER_INFRA; i++) {
      Response response = getRequestWithAuthHeader(target).post(
          Entity.entity(ImmutableMap.of("hostNames", asList("host" + i + ".ec2.aws.com"), "hostConnAttr",
                            connectionAttributes.get(i % connectionAttributes.size()), "bastionConnAttr", envAttr),
              APPLICATION_JSON));
      assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
    }
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
    int nameIdx = randomInt(0, names.size());
    String name = names.get(nameIdx);
    names.remove(nameIdx);
    return name;
  }

  private String randomText(int length) { // TODO: choose words start to word end boundary
    int low = randomInt(50);
    int high = length + low > randomSeedString.length() ? randomSeedString.length() - low : length + low;
    return randomSeedString.substring(low, high);
  }
}
