package software.wings.service.impl;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Environment.EnvironmentType.OTHER;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.Orchestration.Builder.anOrchestration;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.Environment;
import software.wings.beans.Orchestration;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.ServiceTemplateService;
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
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

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

  /**
   * {@inheritDoc}
   */
  @Override
  public PageResponse<Environment> list(PageRequest<Environment> request) {
    return wingsPersistence.query(Environment.class, request);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, String> getData(String appId, String... params) {
    PageRequest<Environment> pageRequest = new PageRequest<>();
    pageRequest.addFilter("appId", appId, Operator.EQ);
    return list(pageRequest).stream().collect(toMap(Environment::getUuid, Environment::getName));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment save(Environment env) {
    env = wingsPersistence.saveAndGet(Environment.class, env);
    appService.addEnvironment(env);
    infraService.createDefaultInfraForEnvironment(env.getAppId(), env.getUuid()); // FIXME: stopgap for Alpha
    tagService.createDefaultRootTagForEnvironment(env);
    serviceTemplateService.createDefaultTemplatesByEnv(env);
    workflowService.createWorkflow(Orchestration.class,
        anOrchestration()
            .withName("Canary Deployment")
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withGraph(
                aGraph()
                    .addNodes(aNode().withId("n0").withName("ORIGIN").withType("ORIGIN").withX(50).withY(50).build(),
                        aNode()
                            .withId("n1")
                            .withName("Repeat by Services")
                            .withType(StateType.REPEAT.name())
                            .withX(250)
                            .withY(50)
                            .addProperty("executionStrategy", "PARALLEL")
                            .addProperty("repeatElementExpression", "${services}")
                            .build(),
                        aNode()
                            .withId("n2")
                            .withName("Repeat by Phases")
                            .withType(StateType.REPEAT.name())
                            .withX(450)
                            .withY(50)
                            .addProperty("executionStrategy", "SERIAL")
                            .addProperty(
                                "repeatElementExpression", "${phases.partitions(\"10%\",\"20%\",\"30%\",\"40%\")}")
                            .build(),
                        aNode()
                            .withId("n3")
                            .withName("Repeat by Instances")
                            .withType(StateType.REPEAT.name())
                            .withX(650)
                            .withY(50)
                            .addProperty("executionStrategy", "PARALLEL")
                            .addProperty("repeatElementExpression", "${instances}")
                            .build(),
                        aNode()
                            .withId("n4")
                            .withName("Stop Instance")
                            .withType(StateType.COMMAND.name())
                            .withX(850)
                            .withY(50)
                            .addProperty("commandName", "STOP")
                            .build(),
                        aNode()
                            .withId("n5")
                            .withName("Install on Instance")
                            .withType(StateType.COMMAND.name())
                            .withX(1050)
                            .withY(50)
                            .addProperty("commandName", "INSTALL")
                            .build(),
                        aNode()
                            .withId("n6")
                            .withName("Start Instance")
                            .withType(StateType.COMMAND.name())
                            .withX(1250)
                            .withY(50)
                            .addProperty("commandName", "START")
                            .build())
                    .addLinks(aLink()
                                  .withId("l0")
                                  .withType(TransitionType.SUCCESS.name())
                                  .withFrom("n0")
                                  .withTo("n1")
                                  .build(),
                        aLink().withId("l1").withType(TransitionType.REPEAT.name()).withFrom("n1").withTo("n2").build(),
                        aLink().withId("l2").withType(TransitionType.REPEAT.name()).withFrom("n2").withTo("n3").build(),
                        aLink().withId("l3").withType(TransitionType.REPEAT.name()).withFrom("n3").withTo("n4").build(),
                        aLink()
                            .withId("l4")
                            .withType(TransitionType.SUCCESS.name())
                            .withFrom("n4")
                            .withTo("n5")
                            .build(),
                        aLink()
                            .withId("l5")
                            .withType(TransitionType.SUCCESS.name())
                            .withFrom("n5")
                            .withTo("n6")
                            .build())
                    .build())
            .withEnvironment(env)
            .withAppId(env.getAppId())
            .build());
    return env;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Environment get(String appId, String envId) {
    return wingsPersistence.get(Environment.class, appId, envId);
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
    wingsPersistence.delete(
        wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).field(ID_KEY).equal(envId));
    executorService.submit(() -> {
      serviceTemplateService.deleteByEnv(appId, envId);
      tagService.deleteByEnv(appId, envId);
      infraService.deleteByEnv(appId, envId);
    });
  }

  @Override
  public void deleteByApp(String appId) {
    List<Environment> environments =
        wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
    environments.forEach(environment -> delete(appId, environment.getUuid()));
  }

  @Override
  public void createDefaultEnvironments(String appId) {
    save(anEnvironment().withAppId(appId).withName("PROD").withEnvironmentType(PROD).build());
    asList("DEV", "QA", "UAT")
        .forEach(name -> save(anEnvironment().withAppId(appId).withName(name).withEnvironmentType(OTHER).build()));
  }

  @Override
  public List<Environment> getEnvByApp(String appId) {
    return wingsPersistence.createQuery(Environment.class).field("appId").equal(appId).asList();
  }
}
