package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.History.Builder.aHistory;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.Orchestration.OrchestrationBuilder.anOrchestration;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.Setup.SetupStatus.INCOMPLETE;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
import static software.wings.common.NotificationMessageResolver.ENTITY_DELETE_NOTIFICATION;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;

import com.codahale.metrics.annotation.Metered;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.EventType;
import software.wings.beans.Notification;
import software.wings.beans.Orchestration;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Setup.SetupStatus;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowType;
import software.wings.beans.stats.AppKeyStatistics;
import software.wings.common.NotificationMessageResolver;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HistoryService;
import software.wings.service.intfc.InfrastructureService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.SetupService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;
import software.wings.sm.TransitionType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Application Service Implementation class.
 *
 * @author Rishi
 */
@ValidateOnExecution
@Singleton
public class AppServiceImpl implements AppService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private SettingsService settingsService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private AppContainerService appContainerService;
  @Inject private ExecutorService executorService;
  @Inject private SetupService setupService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private NotificationService notificationService;
  @Inject private HistoryService historyService;
  @Inject private InfrastructureService infrastructureService;
  @Inject private WorkflowService workflowService;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactService artifactService;
  @Inject private StatisticsService statisticsService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#save(software.wings.beans.Application)
   */
  @Override
  @Metered
  public Application save(Application app) {
    Application application = wingsPersistence.saveAndGet(Application.class, app);
    settingsService.createDefaultSettings(application.getUuid());
    infrastructureService.createDefaultInfrastructure(app.getUuid());
    environmentService.createDefaultEnvironments(application.getUuid());
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(application.getUuid())
            .withDisplayText(getDecoratedNotificationMessage(NotificationMessageResolver.ENTITY_CREATE_NOTIFICATION,
                ImmutableMap.of("ENTITY_TYPE", "Application", "ENTITY_NAME", application.getName())))
            .build());
    workflowService.createWorkflow(Orchestration.class, getDefaultCanaryDeploymentObject(application.getUuid()));

    historyService.createAsync(aHistory()
                                   .withEventType(EventType.CREATED)
                                   .withAppId(app.getUuid())
                                   .withEntityType(EntityType.APPLICATION)
                                   .withEntityId(app.getUuid())
                                   .withEntityName(application.getName())
                                   .withEntityNewValue(application)
                                   .withShortDescription("Application " + application.getName() + " created")
                                   .withTitle("Application " + application.getName() + " created")
                                   .build());
    return get(application.getUuid(), INCOMPLETE, true, 0);
  }

  private Orchestration getDefaultCanaryDeploymentObject(String appId) {
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
        .withAppId(appId)
        .withTargetToAllEnv(true)
        .build();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Application> list(
      PageRequest<Application> req, boolean overview, int numberOfExecutions, int overviewDays) {
    PageResponse<Application> response = wingsPersistence.query(Application.class, req);

    if (overview) { // TODO: merge both overview block make service/env population part of overview option
      Map<String, AppKeyStatistics> applicationKeyStats = statisticsService.getApplicationKeyStats(
          response.stream().map(Application::getUuid).collect(Collectors.toList()), overviewDays);
      response.forEach(application
          -> application.setAppKeyStatistics(
              applicationKeyStats.computeIfAbsent(application.getUuid(), s -> new AppKeyStatistics())));
    }
    response.getResponse().parallelStream().forEach(application -> {
      application.setEnvironments(environmentService.getEnvByApp(application.getUuid()));
      application.setServices(serviceResourceService.findServicesByApp(application.getUuid()));
      if (overview) {
        application.setRecentExecutions(
            workflowExecutionService
                .listExecutions(
                    aPageRequest()
                        .withLimit(Integer.toString(numberOfExecutions))
                        .addFilter(aSearchFilter().withField("appId", Operator.EQ, application.getUuid()).build())
                        .addOrder(aSortOrder().withField("createdAt", OrderType.DESC).build())
                        .build(),
                    false)
                .getResponse());
        application.setNotifications(getIncompleteActionableApplicationNotifications(application.getUuid()));
      }
    });
    return response;
  }

  @Override
  public boolean exist(String appId) {
    return wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(appId).getKey() != null;
  }

  private List<Notification> getIncompleteActionableApplicationNotifications(String appId) {
    return notificationService
        .list(aPageRequest()
                  .addFilter(aSearchFilter().withField("appId", Operator.EQ, appId).build())
                  .addFilter(aSearchFilter().withField("complete", Operator.EQ, false).build())
                  .addFilter(aSearchFilter().withField("actionable", Operator.EQ, true).build())
                  .build())
        .getResponse();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#get(java.lang.String)
   */
  @Override
  public Application get(String uuid) {
    Application application = wingsPersistence.get(Application.class, uuid);
    if (application == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Application doesn't exist");
    }
    return application;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#update(software.wings.beans.Application)
   */
  @Override
  public Application update(Application app) {
    Query<Application> query = wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(app.getUuid());
    UpdateOperations<Application> operations = wingsPersistence.createUpdateOperations(Application.class)
                                                   .set("name", app.getName())
                                                   .set("description", app.getDescription());
    wingsPersistence.update(query, operations);
    return wingsPersistence.get(Application.class, app.getUuid());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.AppService#delete(java.lang.String)
   */
  @Override
  public void delete(String appId) {
    Application application = wingsPersistence.get(Application.class, appId);
    if (application == null) {
      throw new WingsException(INVALID_ARGUMENT, "args", "Application doesn't exist");
    }

    boolean deleted = wingsPersistence.delete(Application.class, appId);
    if (deleted) {
      executorService.submit(() -> {
        notificationService.deleteByApplication(appId);
        notificationService.sendNotificationAsync(
            anInformationNotification()
                .withAppId(application.getUuid())
                .withDisplayText(getDecoratedNotificationMessage(ENTITY_DELETE_NOTIFICATION,
                    ImmutableMap.of("ENTITY_TYPE", "Application", "ENTITY_NAME", application.getName())))
                .build());

        serviceResourceService.deleteByApp(appId);
        environmentService.deleteByApp(appId);
        artifactStreamService.deleteByApplication(appId);
        artifactService.deleteByApplication(appId);
        workflowService.deleteWorkflowByApplication(appId);
        workflowService.deleteStateMachinesByApplication(appId);
        appContainerService.deleteByAppId(appId);
        historyService.deleteByApplication(appId);
      });
      notificationService.sendNotificationAsync(
          anInformationNotification()
              .withAppId(application.getUuid())
              .withDisplayText(getDecoratedNotificationMessage(ENTITY_DELETE_NOTIFICATION,
                  ImmutableMap.of("ENTITY_TYPE", "Application", "ENTITY_NAME", application.getName())))
              .build());
    }

    historyService.createAsync(aHistory()
                                   .withEventType(EventType.DELETED)
                                   .withAppId(application.getUuid())
                                   .withEntityType(EntityType.APPLICATION)
                                   .withEntityId(application.getUuid())
                                   .withEntityName(application.getName())
                                   .withEntityNewValue(application)
                                   .withShortDescription("Application " + application.getName() + " created")
                                   .withTitle("Application " + application.getName() + " created")
                                   .build());
  }

  @Override
  public Application get(String appId, SetupStatus status, boolean overview, int overviewDays) {
    Application application = get(appId);
    application.setEnvironments(environmentService.getEnvByApp(application.getUuid()));
    application.setServices(serviceResourceService.findServicesByApp(application.getUuid()));

    if (overview) {
      application.setNotifications(getIncompleteActionableApplicationNotifications(appId));
      application.setAppKeyStatistics(
          statisticsService.getApplicationKeyStats(Arrays.asList(appId), overviewDays).get(appId));
    }

    if (status == INCOMPLETE) {
      application.setSetup(setupService.getApplicationSetupStatus(application));
    }
    return application;
  }
}
