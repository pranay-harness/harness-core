package software.wings.integration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ApprovalNotification.Builder.anApprovalNotification;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.ChangeNotification.Builder.aChangeNotification;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.ElasticLoadBalancerConfig.Builder.anElasticLoadBalancerConfig;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.DEV;
import static software.wings.beans.FailureNotification.Builder.aFailureNotification;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.HostConnectionAttributes.AccessType.KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.ConnectionType.SSH;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Orchestration.Builder.anOrchestration;
import static software.wings.beans.Permission.Builder.aPermission;
import static software.wings.beans.Pipeline.Builder.aPipeline;
import static software.wings.beans.Role.Builder.aRole;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.ServiceVariable.Builder.aServiceVariable;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.beans.SplunkConfig.Builder.aSplunkConfig;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.beans.artifact.JenkinsConfig.Builder.aJenkinsConfig;
import static software.wings.beans.infrastructure.AwsInfrastructureProviderConfig.Builder.anAwsInfrastructureProviderConfig;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.helpers.ext.mail.SmtpConfig.Builder.aSmtpConfig;
import static software.wings.integration.IntegrationTestUtil.randomInt;
import static software.wings.integration.SeedData.containerNames;
import static software.wings.integration.SeedData.envNames;
import static software.wings.integration.SeedData.randomSeedString;
import static software.wings.integration.SeedData.seedNames;
import static software.wings.security.PermissionAttribute.Action.ALL;
import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.PermissionScope.APP;
import static software.wings.security.PermissionAttribute.PermissionScope.ENV;
import static software.wings.security.PermissionAttribute.ResourceType.ANY;
import static software.wings.utils.ArtifactType.WAR;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
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
import software.wings.beans.AppContainer;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Graph;
import software.wings.beans.Orchestration;
import software.wings.beans.Pipeline;
import software.wings.beans.RestResponse;
import software.wings.beans.Role;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.utils.JsonSubtypeResolver;
import software.wings.utils.Misc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 * Created by anubhaw on 5/6/16.
 */
@Integration
public class DataGenUtil extends WingsBaseTest {
  private static final int NUM_APPS = 1; /* Max 1000 */
  private static final int NUM_APP_CONTAINER_PER_APP = 2; /* Max 1000 */
  private static final int NUM_SERVICES_PER_APP = 1; /* Max 1000 */
  private static final int NUM_CONFIG_FILE_PER_SERVICE = 2; /* Max 100  */
  private static final int NUM_ENV_PER_APP = 0; /* Max 6. 4 are created by default */
  private static final int NUM_HOSTS_PER_INFRA = 5; /* No limit */
  private static final int NUM_TAG_GROUPS_PER_ENV = 3; /* Max 10   */
  private static final int TAG_HIERARCHY_DEPTH = 3; /* Max 10   */
  private static final String API_BASE = "https://localhost:9090/api";

  private static final String userName = "admin@wings.software";
  private static final String password = "admin";

  private static String userToken = "INVALID_TOKEN";
  private final Logger logger = LoggerFactory.getLogger(getClass());
  /**
   * The Test folder.
   */
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();
  private Client client;
  @Inject private WingsPersistence wingsPersistence;
  private List<String> appNames = new ArrayList<>(seedNames);
  private List<String> serviceNames;
  private List<String> configFileNames;
  private SettingAttribute envAttr = null;
  @Inject private WorkflowService workflowService;
  @Inject private WorkflowExecutionService workflowExecutionService;

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
    assertThat(NUM_ENV_PER_APP).isBetween(0, 10);
    assertThat(NUM_TAG_GROUPS_PER_ENV).isBetween(1, 10);
    assertThat(TAG_HIERARCHY_DEPTH).isBetween(1, 10);

    dropDBAndEnsureIndexes();

    ClientConfig config = new ClientConfig(new JacksonJsonProvider().configure(FAIL_ON_UNKNOWN_PROPERTIES, false));
    config.register(MultiPartWriter.class);

    SSLContext sslcontext = SSLContext.getInstance("TLS");
    sslcontext.init(null, new TrustManager[] {new X509TrustManager() {
      public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
      }

      public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
      }

      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
  }
}
}, new java.security.SecureRandom());

ObjectMapper objectMapper = new ObjectMapper();
objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
objectMapper.setSubtypeResolver(new JsonSubtypeResolver(objectMapper.getSubtypeResolver()));
JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
jacksonProvider.setMapper(objectMapper);

client = ClientBuilder.newBuilder()
             .sslContext(sslcontext)
             .hostnameVerifier((s1, s2) -> true)
             .register(MultiPartFeature.class)
             .register(jacksonProvider)
             .build();
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
  addDefaultRoleAndUsers();
  createGlobalSettings();

  List<Application> apps = createApplications();
  Map<String, List<AppContainer>> containers = new HashMap<>();
  Map<String, List<Service>> services = new HashMap<>();
  Map<String, List<Environment>> appEnvs = new HashMap<>();

  containers.put(GLOBAL_APP_ID, addAppContainers(GLOBAL_APP_ID));

  for (Application application : apps) {
    appEnvs.put(application.getUuid(), addEnvs(application.getUuid()));
    containers.put(application.getUuid(), addAppContainers(application.getUuid()));
    services.put(application.getUuid(), addServices(application.getUuid(), containers.get(GLOBAL_APP_ID)));
    //      addServiceInstances(services.get(application.getUuid()), appEnvs.get(application.getUuid()));
    //      addActivitiesAndLogs(application, services.get(application.getUuid()), appEnvs.get(application.getUuid()));
    //      addOrchestrationAndPipeline(services, appEnvs, application);
    //      addNotifications(application);
    createAppSettings(application.getUuid());
  }
}

private void addDefaultRoleAndUsers() {
  Role administrator = aRole()
                           .withAppId(Base.GLOBAL_APP_ID)
                           .withName("Administrator")
                           .withPermissions(asList(aPermission()
                                                       .withPermissionScope(APP)
                                                       .withAppId(GLOBAL_APP_ID)
                                                       .withResourceType(ANY)
                                                       .withAction(ALL)
                                                       .build(),
                               aPermission()
                                   .withPermissionScope(ENV)
                                   .withAppId(GLOBAL_APP_ID)
                                   .withEnvironmentType(EnvironmentType.ALL)
                                   .withResourceType(ANY)
                                   .withAction(ALL)
                                   .build()))
                           .withAdminRole(true)
                           .build();

  Role engineers = aRole()
                       .withAppId(Base.GLOBAL_APP_ID)
                       .withName("Software Engineering")
                       .withPermissions(asList(aPermission()
                                                   .withPermissionScope(APP)
                                                   .withAppId(GLOBAL_APP_ID)
                                                   .withResourceType(ANY)
                                                   .withAction(ALL)
                                                   .build(),
                           aPermission()
                               .withPermissionScope(ENV)
                               .withAppId(GLOBAL_APP_ID)
                               .withEnvironmentType(DEV)
                               .withResourceType(ANY)
                               .withAction(ALL)
                               .build()))
                       .build();

  Role operation = aRole()
                       .withAppId(Base.GLOBAL_APP_ID)
                       .withName("Operation Engineering")
                       .withPermissions(asList(aPermission()
                                                   .withPermissionScope(APP)
                                                   .withAppId(GLOBAL_APP_ID)
                                                   .withResourceType(ANY)
                                                   .withAction(READ)
                                                   .build(),
                           aPermission()
                               .withPermissionScope(ENV)
                               .withAppId(GLOBAL_APP_ID)
                               .withEnvironmentType(EnvironmentType.ALL)
                               .withResourceType(ANY)
                               .withAction(ALL)
                               .build()))
                       .build();

  Role quality = aRole()
                     .withAppId(Base.GLOBAL_APP_ID)
                     .withName("Quality Engineering")
                     .withPermissions(asList(aPermission()
                                                 .withPermissionScope(APP)
                                                 .withAppId(GLOBAL_APP_ID)
                                                 .withResourceType(ANY)
                                                 .withAction(READ)
                                                 .build(),
                         aPermission()
                             .withPermissionScope(ENV)
                             .withAppId(GLOBAL_APP_ID)
                             .withEnvironmentType(EnvironmentType.QA)
                             .withResourceType(ANY)
                             .withAction(ALL)
                             .build()))
                     .build();

  wingsPersistence.save(asList(administrator, engineers, operation, quality));

  addAdminUser();
}

private void addAdminUser() {
  WebTarget target = client.target(API_BASE + "/users/");
  RestResponse<User> response = target.request().post(
      Entity.entity(
          anUser()
              .withName("Admin")
              .withEmail(userName)
              .withPassword(password)
              .withRoles(wingsPersistence
                             .query(Role.class,
                                 aPageRequest()
                                     .addFilter(aSearchFilter().withField("adminRole", Operator.EQ, true).build())
                                     .build())
                             .getResponse())
              .build(),
          APPLICATION_JSON),
      new GenericType<RestResponse<User>>() {});
  assertThat(response.getResource()).isInstanceOf(User.class);
  wingsPersistence.updateFields(User.class, response.getResource().getUuid(), ImmutableMap.of("emailVerified", true));
  loginAdminUser();
}

private void loginAdminUser() {
  String basicAuthValue = "Basic " + encodeBase64String(format("%s:%s", userName, password).getBytes());
  RestResponse<User> response;
  response = client.target(API_BASE + "/users/login")
                 .request()
                 .header("Authorization", basicAuthValue)
                 .get(new GenericType<RestResponse<User>>() {});
  if (response.getResource() != null) {
    userToken = response.getResource().getToken();
  }
}

private void addNotifications(Application application) {
  String appId = application.getUuid();
  String envId = application.getEnvironments().get(0).getUuid();

  wingsPersistence.save(aChangeNotification()
                            .withAppId(appId)
                            .withEnvironmentId(envId)
                            .withEntityId("pipelineId")
                            .withEntityType(EntityType.PIPELINE)
                            .withScheduledOn(currentTimeMillis())
                            .build());
  wingsPersistence.save(anApprovalNotification()
                            .withAppId(appId)
                            .withEnvironmentId(envId)
                            .withEntityId("artifactId")
                            .withEntityType(EntityType.ARTIFACT)
                            .withEntityName("Final_Build_05_02_08_16_9_15pm")
                            .withReleaseId("releaseId")
                            .build());
  wingsPersistence.save(aFailureNotification()
                            .withAppId(appId)
                            .withEntityId("executionId")
                            .withEntityType(EntityType.WORKFLOW)
                            .withEnvironmentId(envId)
                            .withEntityName("workflow_ui_svr_2:04_12_2016")
                            .withExecutionId("executionId")
                            .build());
}

private void addOrchestrationAndPipeline(
    Map<String, List<Service>> services, Map<String, List<Environment>> appEnvs, Application application) {
  Map<String, String> envWorkflowMap =
      addOrchestrationAndPipeline(application, services.get(application.getUuid()), appEnvs.get(application.getUuid()));
  Pipeline pipeline =
      addPipeline(application, services.get(application.getUuid()), appEnvs.get(application.getUuid()), envWorkflowMap);
  addPipelineExecution(application, pipeline);
  addPipelineExecution(application, pipeline);
  addPipelineExecution(application, pipeline);
  addPipelineExecution(application, pipeline);
  addPipelineExecution(application, pipeline);
}

private void addPipelineExecution(Application application, Pipeline pipeline) {
  WorkflowExecution execution =
      workflowExecutionService.triggerPipelineExecution(application.getUuid(), pipeline.getUuid());
  assertThat(execution).isNotNull();
  String executionId = execution.getUuid();
  logger.debug("Pipeline executionId: {}", executionId);
  assertThat(executionId).isNotNull();
  Misc.quietSleep(2000);
  execution = workflowExecutionService.getExecutionDetails(application.getUuid(), executionId);
  assertThat(execution)
      .isNotNull()
      .extracting(WorkflowExecution::getUuid, WorkflowExecution::getStatus)
      .containsExactly(executionId, ExecutionStatus.SUCCESS);
}

private Pipeline addPipeline(Application application, List<Service> services, List<Environment> environments,
    Map<String, String> envWorkflowMap) {
  int x = 80;
  software.wings.beans.Graph.Builder graphBuilder = aGraph();

  x += 200;
  graphBuilder.addNodes(aNode()
                            .withId("n1")
                            .withOrigin(true)
                            .withName("build")
                            .withX(x)
                            .withY(80)
                            .withType(StateType.BUILD.name())
                            .build());

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
  Pipeline pipeline = aPipeline()
                          .withAppId(application.getUuid())
                          .withName("pipeline1")
                          .withDescription("Sample Pipeline")
                          .addServices(services.stream().map(Service::getUuid).collect(toList()).toArray(new String[0]))
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
          .addNodes(aNode()
                        .withId("n1")
                        .withOrigin(true)
                        .withName("stop")
                        .withX(200)
                        .withY(80)
                        .withType(StateType.ENV_STATE.name())
                        .build())
          .addNodes(aNode()
                        .withId("n2")
                        .withName("wait")
                        .withX(360)
                        .withY(80)
                        .withType(StateType.WAIT.name())
                        .addProperty("duration", 5000l)
                        .build())
          .addNodes(
              aNode().withId("n3").withName("start").withX(530).withY(80).withType(StateType.ENV_STATE.name()).build())
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

private Builder getRequestWithAuthHeader(WebTarget target) {
  return target.request().header("Authorization", "Bearer " + userToken);
}

private void addServiceInstances(List<Service> services, List<Environment> appEnvs) {
  // TODO: improve make http calls and use better generation scheme

  services.forEach(service -> {
    appEnvs.forEach(environment -> {
      String infraId =
          wingsPersistence.createQuery(Infrastructure.class).field("appId").equal(Base.GLOBAL_APP_ID).get().getUuid();
      List<Host> hosts = wingsPersistence.createQuery(Host.class)
                             .field("appId")
                             .equal(environment.getAppId())
                             .field("infraId")
                             .equal(infraId)
                             .asList();
      ServiceTemplate template = wingsPersistence.createQuery(ServiceTemplate.class)
                                     .field("appId")
                                     .equal(environment.getAppId())
                                     .field("envId")
                                     .equal(environment.getUuid())
                                     .get();

      List<String> hostIds = hosts.stream().map(Host::getUuid).collect(toList());

      WebTarget target = client.target(format("%s/service-templates/%s/map-hosts?appId=%s&envId=%s", API_BASE,
          template.getUuid(), template.getAppId(), template.getEnvId()));
      getRequestWithAuthHeader(target).put(Entity.entity(hostIds, APPLICATION_JSON));
    });
  });
}

private void createStopActivity(
    Application application, Environment environment, ServiceTemplate template, Host host, ExecutionStatus status) {
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
    Application application, Environment environment, ServiceTemplate template, Host host, ExecutionStatus status) {
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
  WebTarget target = client.target(API_BASE + "/settings/?appId=" + GLOBAL_APP_ID);
  getRequestWithAuthHeader(target).post(
      Entity.entity(aSettingAttribute()
                        .withName("Wings Jenkins")
                        .withValue(aJenkinsConfig()
                                       .withJenkinsUrl("http://ec2-54-174-51-35.compute-1.amazonaws.com/")
                                       .withUsername("wingsbuild")
                                       .withPassword("0db28aa0f4fc0685df9a216fc7af0ca96254b7c2")
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
  getRequestWithAuthHeader(target).post(
      Entity.entity(aSettingAttribute()
                        .withName("Splunk")
                        .withValue(aSplunkConfig()
                                       .withHost("ec2-52-54-103-49.compute-1.amazonaws.com")
                                       .withPort(8089)
                                       .withPassword("W!ngs@Splunk")
                                       .withUsername("admin")
                                       .build())
                        .build(),
          APPLICATION_JSON),
      new GenericType<RestResponse<SettingAttribute>>() {});

  getRequestWithAuthHeader(target).post(
      Entity.entity(aSettingAttribute()
                        .withName("AppDynamics")
                        .withValue(AppDynamicsConfig.Builder.anAppDynamicsConfig()
                                       .withControllerUrl("https://na774.saas.appdynamics.com/controller")
                                       .withUsername("testuser")
                                       .withAccountname("na774")
                                       .withPassword("testuser123")
                                       .build())
                        .build(),
          APPLICATION_JSON),
      new GenericType<RestResponse<SettingAttribute>>() {});

  getRequestWithAuthHeader(target).post(
      Entity.entity(aSettingAttribute()
                        .withName("AWS_CREDENTIALS")
                        .withValue(anAwsInfrastructureProviderConfig()
                                       .withSecretKey("AKIAI6IK4KYQQQEEWEVA")
                                       .withSecretKey("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")
                                       .build())
                        .build(),
          APPLICATION_JSON),
      new GenericType<RestResponse<SettingAttribute>>() {});
}

private void createAppSettings(String appId) {
  WebTarget target = client.target(API_BASE + "/settings/?appId=" + appId);
  getRequestWithAuthHeader(target).post(
      Entity.entity(
          aSettingAttribute()
              .withAppId(appId)
              .withEnvId(GLOBAL_ENV_ID)
              .withName("Wings Key")
              .withValue(aHostConnectionAttributes()
                             .withConnectionType(SSH)
                             .withType(HOST_CONNECTION_ATTRIBUTES)
                             .withAccessType(KEY)
                             .withUserName("ubuntu")
                             .withKey("-----BEGIN RSA PRIVATE KEY-----\n"
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
                             .build())
              .build(),
          APPLICATION_JSON),
      new GenericType<RestResponse<SettingAttribute>>() {});
}

private String createPort(String appId, String serviceId) {
  WebTarget target = client.target(API_BASE + "/service-variables/?appId=" + appId);
  RestResponse<ServiceVariable> restResponse =
      getRequestWithAuthHeader(target).post(Entity.entity(aServiceVariable()
                                                              .withName("PORT")
                                                              .withType(Type.TEXT)
                                                              .withEntityId(serviceId)
                                                              .withEntityType(EntityType.SERVICE)
                                                              .withValue("7070")
                                                              .build(),
                                                APPLICATION_JSON),
          new GenericType<RestResponse<ServiceVariable>>() {});

  return restResponse.getResource().getUuid();
}

private String createLoadBalancer(String appId, String serviceId, String value) {
  WebTarget target = client.target(API_BASE + "/service-variables/?appId=" + appId);
  RestResponse<ServiceVariable> restResponse =
      getRequestWithAuthHeader(target).post(Entity.entity(aServiceVariable()
                                                              .withName("PORT")
                                                              .withType(Type.LB)
                                                              .withEntityId(serviceId)
                                                              .withEntityType(EntityType.SERVICE)
                                                              .withValue(value)
                                                              .build(),
                                                APPLICATION_JSON),
          new GenericType<RestResponse<ServiceVariable>>() {});

  return restResponse.getResource().getUuid();
}

private String createLoadBalancerConfig(String appId, String envId, String lbName) {
  WebTarget target = client.target(API_BASE + "/settings/?appId=" + appId);
  RestResponse<SettingAttribute> restResponse = getRequestWithAuthHeader(target).post(
      Entity.entity(aSettingAttribute()
                        .withAppId(appId)
                        .withEnvId(envId)
                        .withName("Elastic Load Balancer")
                        .withValue(anElasticLoadBalancerConfig()
                                       .withRegion(Regions.US_EAST_1)
                                       .withAccessKey("AKIAIJ5H5UG5TUB3L2QQ")
                                       .withSecretKey("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")
                                       .withLoadBalancerName(lbName)
                                       .withType(SettingVariableTypes.ELB)
                                       .build())
                        .build(),
          APPLICATION_JSON),
      new GenericType<RestResponse<SettingAttribute>>() {});

  return restResponse.getResource().getUuid();
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
    RestResponse<Service> response = getRequestWithAuthHeader(target).post(
        Entity.entity(serviceMap, APPLICATION_JSON), new GenericType<RestResponse<Service>>() { // FIXME
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

private void addConfigFilesToEntity(Base entity, String templateId, int numConfigFilesToBeAdded, EntityType entityType)
    throws IOException {
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
  FormDataMultiPart multiPart =
      new FormDataMultiPart().field("name", file.getName()).field("relativeFilePath", "configs/" + file.getName());
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
  List<Environment> environments = wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
  WebTarget target = client.target(API_BASE + "/environments?appId=" + appId);

  for (int i = 0; i < NUM_ENV_PER_APP; i++) {
    RestResponse<Environment> response = getRequestWithAuthHeader(target).post(
        Entity.entity(
            anEnvironment().withAppId(appId).withName(envNames.get(i)).withDescription(randomText(10)).build(),
            APPLICATION_JSON),
        new GenericType<RestResponse<Environment>>() {});
    assertThat(response.getResource()).isInstanceOf(Environment.class);
    environments.add(response.getResource());
  }
  environments.forEach(this ::addHostsToEnv);
  return environments;
}

private void addHostsToEnv(Environment env) {
  if (envAttr == null) {
    envAttr = wingsPersistence.saveAndGet(SettingAttribute.class,
        aSettingAttribute()
            .withAppId(env.getAppId())
            .withName("JumpBox")
            .withValue(new BastionConnectionAttributes())
            .build());
  }

  Infrastructure infrastructure = wingsPersistence.createQuery(Infrastructure.class).get();
  WebTarget target = client.target(format(
      API_BASE + "/hosts?infraId=%s&appId=%s&envId=%s", infrastructure.getUuid(), env.getAppId(), env.getUuid()));

  List<SettingAttribute> connectionAttributes = wingsPersistence.createQuery(SettingAttribute.class)
                                                    .field("appId")
                                                    .equal(env.getAppId())
                                                    .field("value.type")
                                                    .equal(HOST_CONNECTION_ATTRIBUTES)
                                                    .asList();

  for (int i = 1; i <= NUM_HOSTS_PER_INFRA; i++) {
    Response response = getRequestWithAuthHeader(target).post(Entity.entity(
        ImmutableMap.of("hostNames", asList("host" + i + ".ec2.aws.com"), "hostConnAttr",
            connectionAttributes.get(i % connectionAttributes.size()).getUuid(), "bastionConnAttr", envAttr.getUuid()),
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
