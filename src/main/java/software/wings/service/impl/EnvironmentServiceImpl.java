package software.wings.service.impl;

import static java.util.stream.Collectors.toMap;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.DEV;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.Environment.EnvironmentType.QA;
import static software.wings.beans.Environment.EnvironmentType.UAT;
import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.History.Builder.aHistory;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.Orchestration.OrchestrationBuilder.anOrchestration;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;

import com.google.common.collect.ImmutableMap;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.EventType;
import software.wings.beans.Orchestration;
import software.wings.beans.SearchFilter;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.common.NotificationMessageResolver;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HistoryService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.TagService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;
import software.wings.sm.TransitionType;
import software.wings.stencils.DataProvider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 4/1/16.
 */
@ValidateOnExecution
@Singleton
public class EnvironmentServiceImpl implements EnvironmentService, DataProvider {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private TagService tagService;
  @Inject private ExecutorService executorService;
  @Inject private WorkflowService workflowService;
  @Inject private SetupService setupService;
  @Inject private NotificationService notificationService;
  @Inject private HistoryService historyService;
  @Inject private HostService hostService;
  @Inject private ActivityService activityService;

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Environment> list(PageRequest<Environment> request, boolean withSummary) {
    PageResponse<Environment> pageResponse = wingsPersistence.query(Environment.class, request);
    if (withSummary) {
      pageResponse.getResponse().forEach(environment -> { addServiceTemplates(environment); });
    }
    return pageResponse;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment get(String appId, String envId, boolean withSummary) {
    Environment environment = wingsPersistence.get(Environment.class, appId, envId);
    if (environment == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Environment doesn't exist");
    }
    if (withSummary) {
      addServiceTemplates(environment);
    }
    return environment;
  }

  @Override
  public Environment get(@NotEmpty String appId, @NotEmpty String envId, @NotNull SetupStatus status) {
    Environment environment = get(appId, envId, true);
    if (status == SetupStatus.INCOMPLETE) {
      environment.setSetup(setupService.getEnvironmentSetupStatus(environment));
    }
    return environment;
  }

  @Override
  public boolean exist(@NotEmpty String appId, @NotEmpty String envId) {
    return wingsPersistence.createQuery(Environment.class)
               .field("appId")
               .equal(appId)
               .field(ID_KEY)
               .equal(envId)
               .getKey()
        != null;
  }

  private void addServiceTemplates(Environment environment) {
    PageRequest<ServiceTemplate> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", environment.getAppId(), SearchFilter.Operator.EQ);
    pageRequest.addFilter("envId", environment.getUuid(), EQ);
    environment.setServiceTemplates(serviceTemplateService.list(pageRequest, true).getResponse());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getData(String appId, String... params) {
    PageRequest<Environment> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", appId, EQ);
    return list(pageRequest, false).stream().collect(toMap(Environment::getUuid, Environment::getName));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment save(Environment environment) {
    environment = wingsPersistence.saveAndGet(Environment.class, environment);
    tagService.createDefaultRootTagForEnvironment(environment);
    serviceTemplateService.createDefaultTemplatesByEnv(environment);
    workflowService.createWorkflow(Orchestration.class, getDefaultCanaryDeploymentObject(environment));
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(environment.getAppId())
            .withDisplayText(getDecoratedNotificationMessage(NotificationMessageResolver.ENTITY_CREATE_NOTIFICATION,
                ImmutableMap.of("ENTITY_TYPE", "Environment", "ENTITY_NAME", environment.getName())))
            .build());
    historyService.createAsync(aHistory()
                                   .withEventType(EventType.CREATED)
                                   .withAppId(environment.getAppId())
                                   .withEntityType(EntityType.ENVIRONMENT)
                                   .withEntityId(environment.getUuid())
                                   .withEntityName(environment.getName())
                                   .withEntityNewValue(environment)
                                   .withShortDescription("Environment " + environment.getName() + " created")
                                   .withTitle("Environment " + environment.getName() + " created")
                                   .build());
    return environment;
  }

  private Orchestration getDefaultCanaryDeploymentObject(Environment env) {
    return anOrchestration()
        .withName("Canary Deployment")
        .withWorkflowType(WorkflowType.ORCHESTRATION)
        .withGraph(
            aGraph()
                .addNodes(aNode()
                              .withId("n1")
                              .withOrigin(true)
                              .withName("Services: All")
                              .withType(StateType.REPEAT.name())
                              .withX(50)
                              .withY(50)
                              .addProperty("executionStrategy", "SERIAL")
                              .addProperty("repeatElementExpression", "${services}")
                              .build(),
                    aNode()
                        .withId("n2")
                        .withName("Phases: 10%,20%,30%,40%")
                        .withType(StateType.REPEAT.name())
                        .withX(180)
                        .withY(50)
                        .addProperty("executionStrategy", "SERIAL")
                        .addProperty(
                            "repeatElementExpression", "${phases.withPercentages(\"10%\",\"20%\",\"30%\",\"40%\")}")
                        .build(),
                    aNode()
                        .withId("n3")
                        .withName("Instances: All")
                        .withType(StateType.REPEAT.name())
                        .withX(310)
                        .withY(50)
                        .addProperty("executionStrategy", "PARALLEL")
                        .addProperty("repeatElementExpression", "${instances}")
                        .build(),
                    aNode()
                        .withId("n4")
                        .withName("Install on Instance")
                        .withType(StateType.COMMAND.name())
                        .withX(440)
                        .withY(50)
                        .addProperty("commandName", "Install")
                        .build(),
                    aNode()
                        .withId("n5")
                        .withName("Http Verification")
                        .withType(StateType.HTTP.name())
                        .withX(590)
                        .withY(50)
                        .addProperty("url", "http://${host.name}:8080")
                        .addProperty("method", "GET")
                        .addProperty("assertion", "${httpResponseCode}==200")
                        .build(),
                    aNode()
                        .withId("n6")
                        .withName("Wait")
                        .withType(StateType.WAIT.name())
                        .withX(740)
                        .withY(50)
                        .addProperty("duration", "30")
                        .build(),
                    aNode()
                        .withId("n7")
                        .withName("Verify")
                        .withType(StateType.FORK.name())
                        .withX(890)
                        .withY(50)
                        .build(),
                    aNode()
                        .withId("n8")
                        .withName("Splunk")
                        .withType(StateType.SPLUNK.name())
                        .withX(1020)
                        .withY(50)
                        .addProperty("query",
                            "host=\"${host.name}\" sourcetype=catalina log_level=ERROR earliest=-15m | stats count")
                        .addProperty("assertion", "${xpath('//field[@k=\"count\"]/value/text/text()')}.equals(\"0\")")
                        .build(),
                    aNode()
                        .withId("n9")
                        .withName("AppDynamics")
                        .withType(StateType.APP_DYNAMICS.name())
                        .withX(1050)
                        .withY(150)
                        .addProperty("applicationName", "MyApp")
                        .addProperty("metricPath",
                            "Overall Application Performance|MyTier|Individual Nodes|Tomcat@${host.hostName}|Average Response Time (ms)")
                        .addProperty("assertion", "${xpath('//metric-value/value/text()')} < 100")
                        .build())
                .addLinks(
                    aLink().withId("l1").withType(TransitionType.REPEAT.name()).withFrom("n1").withTo("n2").build(),
                    aLink().withId("l2").withType(TransitionType.REPEAT.name()).withFrom("n2").withTo("n3").build(),
                    aLink().withId("l3").withType(TransitionType.REPEAT.name()).withFrom("n3").withTo("n4").build(),
                    aLink().withId("l4").withType(TransitionType.SUCCESS.name()).withFrom("n4").withTo("n5").build(),
                    aLink().withId("l5").withType(TransitionType.SUCCESS.name()).withFrom("n5").withTo("n6").build(),
                    aLink().withId("l6").withType(TransitionType.SUCCESS.name()).withFrom("n6").withTo("n7").build(),
                    aLink().withId("l7").withType(TransitionType.FORK.name()).withFrom("n7").withTo("n8").build(),
                    aLink().withId("l8").withType(TransitionType.FORK.name()).withFrom("n7").withTo("n9").build())
                .build())
        .withAppId(env.getAppId())
        .build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment update(Environment environment) {
    wingsPersistence.updateFields(Environment.class, environment.getUuid(),
        ImmutableMap.of("name", environment.getName(), "description", environment.getDescription(), "environmentType",
            environment.getEnvironmentType()));
    return wingsPersistence.get(Environment.class, environment.getAppId(), environment.getUuid());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void delete(String appId, String envId) {
    Environment environment = wingsPersistence.get(Environment.class, appId, envId);
    if (environment == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Environment doesn't exist");
    }
    boolean deleted = wingsPersistence.delete(
        wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).field(ID_KEY).equal(envId));

    if (deleted) {
      executorService.submit(() -> {
        serviceTemplateService.deleteByEnv(appId, envId);
        tagService.deleteByEnv(appId, envId);
        hostService.deleteByEnvironment(appId, envId);
        activityService.deleteByEnvironment(appId, envId);
        notificationService.sendNotificationAsync(
            anInformationNotification()
                .withAppId(environment.getAppId())
                .withDisplayText(getDecoratedNotificationMessage(NotificationMessageResolver.ENTITY_DELETE_NOTIFICATION,
                    ImmutableMap.of("ENTITY_TYPE", "Environment", "ENTITY_NAME", environment.getName())))
                .build());
        historyService.createAsync(aHistory()
                                       .withEventType(EventType.DELETED)
                                       .withAppId(environment.getAppId())
                                       .withEntityType(EntityType.ENVIRONMENT)
                                       .withEntityId(environment.getUuid())
                                       .withEntityName(environment.getName())
                                       .withEntityNewValue(environment)
                                       .withShortDescription("Environment " + environment.getName() + " created")
                                       .withTitle("Environment " + environment.getName() + " created")
                                       .build());

      });
    }
  }

  @Override
  public void deleteByApp(String appId) {
    List<Environment> environments =
        wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
    environments.forEach(environment -> delete(appId, environment.getUuid()));
  }

  @Override
  public void createDefaultEnvironments(String appId) {
    save(anEnvironment().withAppId(appId).withName(Constants.DEV_ENV).withEnvironmentType(DEV).build());
    save(anEnvironment().withAppId(appId).withName(Constants.QA_ENV).withEnvironmentType(QA).build());
    save(anEnvironment().withAppId(appId).withName(Constants.UAT_ENV).withEnvironmentType(UAT).build());
    save(anEnvironment().withAppId(appId).withName(Constants.PROD_ENV).withEnvironmentType(PROD).build());
  }

  @Override
  public List<Environment> getEnvByApp(String appId) {
    return wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
  }
}
