package software.wings.service.impl;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.OTHER;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.ErrorCodes.INVALID_ARGUMENT;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.InformationNotification.Builder.anInformationNotification;
import static software.wings.beans.Orchestration.Builder.anOrchestration;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.common.NotificationMessageResolver.getDecoratedNotificationMessage;

import com.google.common.collect.ImmutableMap;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.Environment;
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
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfraService;
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
  @Inject private InfraService infraService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private TagService tagService;
  @Inject private ExecutorService executorService;
  @Inject private AppService appService;
  @Inject private WorkflowService workflowService;
  @Inject private SetupService setupService;
  @Inject private NotificationService notificationService;

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Environment> list(PageRequest<Environment> request, boolean withSummary) {
    PageResponse<Environment> pageResponse = wingsPersistence.query(Environment.class, request);
    if (withSummary) {
      pageResponse.getResponse().forEach(environment -> {
        addServiceTemplates(environment);
        addWorkflows(environment);
      });
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
      addWorkflows(environment);
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

  private void addWorkflows(Environment environment) {
    PageRequest<Orchestration> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", environment.getAppId(), SearchFilter.Operator.EQ);
    pageRequest.addFilter("environment", environment, SearchFilter.Operator.EQ);
    environment.setOrchestrations(workflowService.listOrchestration(pageRequest).getResponse());
  }

  private void addServiceTemplates(Environment environment) {
    PageRequest<ServiceTemplate> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", environment.getAppId(), SearchFilter.Operator.EQ);
    pageRequest.addFilter("envId", environment.getUuid(), EQ);
    environment.setServiceTemplates(serviceTemplateService.list(pageRequest).getResponse());
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
    appService.addEnvironment(environment);
    infraService.createDefaultInfraForEnvironment(
        environment.getAppId(), environment.getUuid()); // FIXME: stopgap for Alpha
    tagService.createDefaultRootTagForEnvironment(environment);
    serviceTemplateService.createDefaultTemplatesByEnv(environment);
    workflowService.createWorkflow(Orchestration.class, getDefaultCanaryDeploymentObject(environment));
    notificationService.sendNotificationAsync(
        anInformationNotification()
            .withAppId(environment.getAppId())
            .withDisplayText(getDecoratedNotificationMessage(NotificationMessageResolver.ENTITY_CREATE_NOTIFICATION,
                ImmutableMap.of("ENTITY_TYPE", "Environment", "ENTITY_NAME", environment.getName())))
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
                              .withName("Services:All")
                              .withType(StateType.REPEAT.name())
                              .withX(50)
                              .withY(50)
                              .addProperty("executionStrategy", "SERIAL")
                              .addProperty("repeatElementExpression", "${services}")
                              .build(),
                    aNode()
                        .withId("n2")
                        .withName("Phases:10%,20%,30%,40%")
                        .withType(StateType.REPEAT.name())
                        .withX(200)
                        .withY(50)
                        .addProperty("executionStrategy", "SERIAL")
                        .addProperty(
                            "repeatElementExpression", "${phases.withPercentages(\"10%\",\"20%\",\"30%\",\"40%\")}")
                        .build(),
                    aNode()
                        .withId("n3")
                        .withName("Instances:All")
                        .withType(StateType.REPEAT.name())
                        .withX(350)
                        .withY(50)
                        .addProperty("executionStrategy", "PARALLEL")
                        .addProperty("repeatElementExpression", "${instances}")
                        .build(),
                    aNode()
                        .withId("n4")
                        .withName("Stop Instance")
                        .withType(StateType.COMMAND.name())
                        .withX(500)
                        .withY(50)
                        .addProperty("commandName", "STOP")
                        .build(),
                    aNode()
                        .withId("n5")
                        .withName("Install on Instance")
                        .withType(StateType.COMMAND.name())
                        .withX(650)
                        .withY(50)
                        .addProperty("commandName", "INSTALL")
                        .build(),
                    aNode()
                        .withId("n6")
                        .withName("Start Instance")
                        .withType(StateType.COMMAND.name())
                        .withX(800)
                        .withY(50)
                        .addProperty("commandName", "START")
                        .build())
                .addLinks(
                    aLink().withId("l1").withType(TransitionType.REPEAT.name()).withFrom("n1").withTo("n2").build(),
                    aLink().withId("l2").withType(TransitionType.REPEAT.name()).withFrom("n2").withTo("n3").build(),
                    aLink().withId("l3").withType(TransitionType.REPEAT.name()).withFrom("n3").withTo("n4").build(),
                    aLink().withId("l4").withType(TransitionType.SUCCESS.name()).withFrom("n4").withTo("n5").build(),
                    aLink().withId("l5").withType(TransitionType.SUCCESS.name()).withFrom("n5").withTo("n6").build())
                .build())
        .withEnvironment(env)
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
        notificationService.sendNotificationAsync(
            anInformationNotification()
                .withAppId(environment.getAppId())
                .withDisplayText(getDecoratedNotificationMessage(NotificationMessageResolver.ENTITY_DELETE_NOTIFICATION,
                    ImmutableMap.of("ENTITY_TYPE", "Environment", "ENTITY_NAME", environment.getName())))
                .build());
        serviceTemplateService.deleteByEnv(appId, envId);
        tagService.deleteByEnv(appId, envId);
        infraService.deleteByEnv(appId, envId);
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
    save(anEnvironment().withAppId(appId).withName(Constants.PROD_ENV).withEnvironmentType(PROD).build());
    asList(Constants.UAT_ENV, Constants.QA_ENV, Constants.DEV_ENV)
        .forEach(name -> save(anEnvironment().withAppId(appId).withName(name).withEnvironmentType(OTHER).build()));
  }

  @Override
  public List<Environment> getEnvByApp(String appId) {
    return wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
  }
}
