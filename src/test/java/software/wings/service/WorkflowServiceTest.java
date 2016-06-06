package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentBuilder;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Link;
import software.wings.beans.Graph.Node;
import software.wings.beans.Orchestration;
import software.wings.beans.Pipeline;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Listeners;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineTest;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;
import software.wings.sm.Transition;
import software.wings.sm.TransitionType;
import software.wings.sm.states.ForkState;
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyEventListener;

import java.beans.IntrospectionException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * The Class WorkflowServiceTest.
 *
 * @author Rishi
 */
@Listeners(NotifyEventListener.class)
public class WorkflowServiceTest extends WingsBaseTest {
  private static String appId = UUIDGenerator.getUuid();
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WorkflowService workflowService;
  @Inject private WingsPersistence wingsPersistence;
  private Environment env;

  public Environment getEnvironment() {
    if (env == null) {
      env = wingsPersistence.saveAndGet(Environment.class, EnvironmentBuilder.anEnvironment().withAppId(appId).build());
    }
    return env;
  }

  /**
   * Should save and read.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldSaveAndRead() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);
    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    String smId = sm.getUuid();
    sm = wingsPersistence.get(StateMachine.class, smId);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();
    System.out.println("All Done!");
  }

  /**
   * Should trigger.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTrigger() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);
    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();
    workflowService.trigger(appId, smId, executionUuid);
    Thread.sleep(5000);

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNotNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateB.getName()))
        .as("StateA executed before StateB")
        .isEqualTo(true);
    assertThat((long) StaticMap.getValue(stateB.getName()) < (long) StaticMap.getValue(stateC.getName()))
        .as("StateB executed before StateC")
        .isEqualTo(true);
  }

  /**
   * Should trigger failed transition.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerFailedTransition() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000), true);
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);
    StateMachineTest.StateSynch stateD = new StateMachineTest.StateSynch("stateD" + new Random().nextInt(10000));
    sm.addState(stateD);
    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.FAILURE)
                         .withToState(stateD)
                         .build());

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();
    workflowService.trigger(appId, smId, executionUuid);
    Thread.sleep(5000);

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNull();
    assertThat(StaticMap.getValue(stateD.getName())).isNotNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateB.getName()))
        .as("StateA executed before StateB")
        .isEqualTo(true);
    assertThat(StaticMap.getValue(stateC.getName())).isNull();
    assertThat((long) StaticMap.getValue(stateB.getName()) < (long) StaticMap.getValue(stateD.getName()))
        .as("StateB executed before StateD")
        .isEqualTo(true);
  }

  /**
   * Should trigger asynch.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerAsynch() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsynch("StateAB" + new Random().nextInt(10000), 2000);
    sm.addState(stateAB);
    State stateBC = new StateMachineTest.StateAsynch("StateBC" + new Random().nextInt(10000), 1000);
    sm.addState(stateBC);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateAB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateBC)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateBC)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();
    workflowService.trigger(appId, sm.getUuid(), executionUuid);

    Thread.sleep(8000);

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateAB.getName()))
        .as("StateA executed before StateAB")
        .isEqualTo(true);
    assertThat((long) StaticMap.getValue(stateAB.getName()) < (long) StaticMap.getValue(stateB.getName()))
        .as("StateAB executed before StateB")
        .isEqualTo(true);
    assertThat((long) StaticMap.getValue(stateB.getName()) < (long) StaticMap.getValue(stateBC.getName()))
        .as("StateB executed before StateBC")
        .isEqualTo(true);
    assertThat((long) StaticMap.getValue(stateBC.getName()) < (long) StaticMap.getValue(stateC.getName()))
        .as("StateBC executed before StateC")
        .isEqualTo(true);
  }

  /**
   * Should trigger failed asynch.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerFailedAsynch() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsynch("StateAB" + new Random().nextInt(10000), 2000, true);
    sm.addState(stateAB);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateAB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.FAILURE)
                         .withToState(stateC)
                         .build());

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();
    workflowService.trigger(appId, sm.getUuid(), executionUuid);

    Thread.sleep(5000);

    assertThat(StaticMap.getValue(stateA.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateAB.getName())).isNotNull();
    assertThat(StaticMap.getValue(stateB.getName())).isNull();
    assertThat(StaticMap.getValue(stateC.getName())).isNotNull();

    assertThat((long) StaticMap.getValue(stateA.getName()) < (long) StaticMap.getValue(stateAB.getName()))
        .as("StateA executed before StateAB")
        .isEqualTo(true);
    assertThat((long) StaticMap.getValue(stateAB.getName()) < (long) StaticMap.getValue(stateC.getName()))
        .as("StateAB executed before StateC")
        .isEqualTo(true);
  }

  private StateMachine createAsynchSM(WorkflowService svc) {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsynch("StateAB", 5000);
    sm.addState(stateAB);
    State stateBC = new StateMachineTest.StateAsynch("StateBC", 2000);
    sm.addState(stateBC);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateAB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateBC)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateBC)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());

    sm = svc.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();
    return sm;
  }

  /**
   * Should trigger simple fork.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerSimpleFork() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    ForkState fork1 = new ForkState("fork1");
    List<String> forkStates = new ArrayList<String>();
    forkStates.add(stateB.getName());
    forkStates.add(stateC.getName());
    fork1.setForkStateNames(forkStates);
    sm.addState(fork1);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(fork1)
                         .build());

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();
    workflowService.trigger(appId, sm.getUuid(), executionUuid);

    Thread.sleep(2000);
  }

  /**
   * Should trigger mixed fork.
   *
   * @throws InterruptedException the interrupted exception
   */
  @Test
  public void shouldTriggerMixedFork() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId(appId);
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);

    State stateAB = new StateMachineTest.StateAsynch("StateAB", 5000);
    sm.addState(stateAB);
    State stateBC = new StateMachineTest.StateAsynch("StateBC", 2000);
    sm.addState(stateBC);

    ForkState fork1 = new ForkState("fork1");
    List<String> forkStates = new ArrayList<String>();
    forkStates.add(stateB.getName());
    forkStates.add(stateBC.getName());
    fork1.setForkStateNames(forkStates);
    sm.addState(fork1);

    sm.setInitialStateName(stateA.getName());

    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateA)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateAB)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(stateAB)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(fork1)
                         .build());
    sm.addTransition(Transition.Builder.aTransition()
                         .withFromState(fork1)
                         .withTransitionType(TransitionType.SUCCESS)
                         .withToState(stateC)
                         .build());

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    String executionUuid = UUIDGenerator.getUuid();
    workflowService.trigger(appId, sm.getUuid(), executionUuid);

    Thread.sleep(10000);
  }

  /**
   * Should create pipeline with graph.
   */
  @Test
  public void shouldCreatePipelineWithGraph() {
    createPipeline();
  }

  /**
   * Should update pipeline with no graph.
   */
  @Test
  public void shouldUpdatePipelineWithNoGraph() {
    Pipeline pipeline = createPipelineNoGraph();
    Graph graph = createInitialGraph();
    pipeline.setGraph(graph);

    updatePipeline(pipeline, 1);
  }

  /**
   * Should update pipeline with graph.
   */
  @Test
  public void shouldUpdatePipelineWithGraph() {
    Pipeline pipeline = createPipeline();
    updatePipeline(pipeline, 2);
  }

  /**
   * Update pipeline.
   *
   * @param pipeline   the pipeline
   * @param graphCount the graph count
   */
  public void updatePipeline(Pipeline pipeline, int graphCount) {
    pipeline.setDescription("newDescription");
    pipeline.setName("pipeline2");
    List<String> newServices = Lists.newArrayList("123", "345");
    pipeline.setServices(newServices);

    Graph graph = pipeline.getGraph();
    Node node = new Node();
    node.setId("n5");
    node.setName("PROD");
    node.setX(350);
    node.setY(50);
    node.getProperties().put("envId", "34567");
    node.setType(StateType.ENV_STATE.name());
    graph.getNodes().add(node);

    Link link = new Link();
    link.setId("l3");
    link.setFrom("n4");
    link.setTo("n5");
    link.setType("success");
    graph.getLinks().add(link);

    Pipeline updatedPipeline = workflowService.updatePipeline(pipeline);
    assertThat(updatedPipeline).isNotNull();
    assertThat(updatedPipeline.getUuid()).isNotNull();
    assertThat(updatedPipeline.getUuid()).isEqualTo(pipeline.getUuid());
    assertThat(updatedPipeline.getName()).isEqualTo("pipeline2");
    assertThat(updatedPipeline.getDescription()).isEqualTo("newDescription");
    assertThat(updatedPipeline.getServices()).isEqualTo(newServices);

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(pipeline.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(graphCount);

    StateMachine sm = workflowService.readLatest(updatedPipeline.getUuid(), null);
    assertThat(sm.getGraph()).isNotNull();
    assertThat(sm.getGraph()).isEqualTo(graph);
  }

  private Pipeline createPipeline() {
    Pipeline pipeline = new Pipeline();
    pipeline.setAppId(appId);
    pipeline.setName("pipeline1");
    pipeline.setDescription("Sample Pipeline");

    pipeline.setServices(Lists.newArrayList("service1", "service2"));
    Graph graph = createInitialGraph();

    pipeline.setGraph(graph);

    pipeline = workflowService.createWorkflow(Pipeline.class, pipeline);
    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getUuid()).isNotNull();

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(pipeline.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(1);
    assertThat(res.get(0)).isNotNull();

    StateMachine sm = res.get(0);
    assertThat(sm.getGraph()).isNotNull();
    assertThat(sm.getGraph()).isEqualTo(graph);

    return pipeline;
  }

  /**
   * @return
   */
  private Graph createInitialGraph() {
    Graph graph = new Graph();
    List<Node> nodes = new ArrayList<>();

    Node node = new Node();
    node.setId("n0");
    node.setName("ORIGIN");
    node.setX(200);
    node.setY(50);
    node.setType(StateType.BUILD.name());
    nodes.add(node);

    node = new Node();
    node.setId("n1");
    node.setName("BUILD");
    node.setX(200);
    node.setY(50);
    node.setType(StateType.BUILD.name());
    nodes.add(node);

    node = new Node();
    node.setId("n2");
    node.setName("IT");
    node.setX(250);
    node.setY(50);
    node.getProperties().put("envId", "12345");
    node.setType(StateType.ENV_STATE.name());
    nodes.add(node);

    node = new Node();
    node.setId("n3");
    node.setName("QA");
    node.setX(300);
    node.setY(50);
    node.getProperties().put("envId", "23456");
    node.setType(StateType.ENV_STATE.name());
    nodes.add(node);

    node = new Node();
    node.setId("n4");
    node.setName("UAT");
    node.setX(350);
    node.setY(50);
    node.getProperties().put("envId", "34567");
    node.setType(StateType.ENV_STATE.name());
    nodes.add(node);

    graph.setNodes(nodes);

    List<Link> links = new ArrayList<>();

    Link link = new Link();
    link.setId("l0");
    link.setFrom("n0");
    link.setTo("n1");
    link.setType("success");
    links.add(link);

    link = new Link();
    link.setId("l1");
    link.setFrom("n1");
    link.setTo("n2");
    link.setType("success");
    links.add(link);

    link = new Link();
    link.setId("l2");
    link.setFrom("n2");
    link.setTo("n3");
    link.setType("success");
    links.add(link);

    link = new Link();
    link.setId("l3");
    link.setFrom("n3");
    link.setTo("n4");
    link.setType("success");
    links.add(link);

    graph.setLinks(links);
    return graph;
  }

  private Pipeline createPipelineNoGraph() {
    Pipeline pipeline = new Pipeline();
    pipeline.setAppId(appId);
    pipeline.setName("pipeline1");
    pipeline.setDescription("Sample Pipeline");

    pipeline.setServices(Lists.newArrayList("service1", "service2"));

    pipeline = workflowService.createWorkflow(Pipeline.class, pipeline);
    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getUuid()).isNotNull();

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(pipeline.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(0);

    return pipeline;
  }

  /**
   * Should create orchestration.
   */
  @Test
  public void shouldCreateOrchestration() {
    createOrchestration();
  }

  /**
   * Should update orchestration.
   */
  @Test
  public void shouldUpdateOrchestration() {
    Orchestration orchestration = createOrchestration();
    String uuid = orchestration.getUuid();
    orchestration = workflowService.readOrchestration(appId, orchestration.getEnvironment().getUuid(), uuid);

    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();
    assertThat(orchestration.getUuid()).isEqualTo(uuid);

    orchestration.setName("orchestration2");
    orchestration.setDescription(null);
    Graph graph = orchestration.getGraph();

    Node node = new Node();
    node.setId("n5");
    node.setName("http");
    node.setX(350);
    node.setY(50);
    node.setType(StateType.HTTP.name());
    graph.getNodes().add(node);

    Link link = new Link();
    link.setId("l3");
    link.setFrom("n3");
    link.setTo("n5");
    link.setType("success");
    graph.getLinks().add(link);

    Orchestration updatedOrchestration = workflowService.updateOrchestration(orchestration);
    assertThat(updatedOrchestration).isNotNull();
    assertThat(updatedOrchestration.getUuid()).isNotNull();
    assertThat(updatedOrchestration.getUuid()).isEqualTo(orchestration.getUuid());
    assertThat(updatedOrchestration.getName()).isEqualTo("orchestration2");
    assertThat(updatedOrchestration.getDescription()).isNull();
    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(orchestration.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(2);

    StateMachine sm = workflowService.readLatest(updatedOrchestration.getUuid(), null);
    assertThat(sm.getGraph()).isNotNull();
    assertThat(sm.getGraph()).isEqualTo(graph);
  }

  /**
   * Should delete orchestration.
   */
  @Test
  public void shouldDeleteOrchestration() {
    Orchestration orchestration = createOrchestration();
    String uuid = orchestration.getUuid();
    workflowService.deleteWorkflow(Orchestration.class, appId, uuid);
    orchestration = workflowService.readOrchestration(appId, orchestration.getEnvironment().getUuid(), uuid);
    assertThat(orchestration).isNull();
  }

  private Orchestration createOrchestration() {
    Orchestration orchestration = new Orchestration();
    orchestration.setAppId(appId);
    orchestration.setName("workflow1");
    orchestration.setDescription("Sample Workflow");
    orchestration.setEnvironment(getEnvironment());

    Graph graph = new Graph();
    List<Node> nodes = new ArrayList<>();

    Node node = new Node();
    node.setId("n0");
    node.setName("ORIGIN");
    node.setX(200);
    node.setY(50);
    nodes.add(node);

    node = new Node();
    node.setId("n1");
    node.setName("stop");
    node.setX(200);
    node.setY(50);
    node.setType(StateType.STOP.name());
    nodes.add(node);

    node = new Node();
    node.setId("n2");
    node.setName("wait");
    node.setX(250);
    node.setY(50);
    node.getProperties().put("duration", 5000l);
    node.setType(StateType.WAIT.name());
    nodes.add(node);

    node = new Node();
    node.setId("n3");
    node.setName("start");
    node.setX(300);
    node.setY(50);
    node.setType(StateType.START.name());
    nodes.add(node);

    graph.setNodes(nodes);

    List<Link> links = new ArrayList<>();

    Link link = new Link();
    link.setId("l0");
    link.setFrom("n0");
    link.setTo("n1");
    link.setType("success");
    links.add(link);

    link = new Link();
    link.setId("l1");
    link.setFrom("n1");
    link.setTo("n2");
    link.setType("success");
    links.add(link);

    link = new Link();
    link.setId("l2");
    link.setFrom("n2");
    link.setTo("n3");
    link.setType("success");
    links.add(link);

    graph.setLinks(links);

    orchestration.setGraph(graph);

    orchestration = workflowService.createWorkflow(Orchestration.class, orchestration);
    assertThat(orchestration).isNotNull();
    assertThat(orchestration.getUuid()).isNotNull();

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValues(orchestration.getUuid());
    filter.setOp(Operator.EQ);
    req.addFilter(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(1);
    assertThat(res.get(0)).isNotNull();

    StateMachine sm = res.get(0);
    assertThat(sm.getGraph()).isNotNull();
    assertThat(sm.getGraph()).isEqualTo(graph);

    return orchestration;
  }

  /**
   * Should trigger orchestration.
   */
  @Test
  public void shouldTriggerOrchestration() {
    Orchestration orchestration = createOrchestration();
    ExecutionArgs executionArgs = new ExecutionArgs();
    WorkflowExecution execution =
        workflowService.triggerOrchestrationExecution(appId, orchestration.getUuid(), executionArgs);
    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Orchestration executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    Misc.quietSleep(2000);
    execution = workflowService.getExecutionDetails(appId, executionId);
    assertThat(execution).isNotNull();
    assertThat(execution.getUuid()).isEqualTo(executionId);
    assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  /**
   * Should list orchestration.
   */
  @Test
  public void shouldListOrchestration() {
    shouldTriggerOrchestration();

    // 2nd orchestration
    Orchestration orchestration = createOrchestration();
    PageRequest<Orchestration> pageRequest = new PageRequest<>();
    PageResponse<Orchestration> res = workflowService.listOrchestration(pageRequest);

    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(2);
  }

  /**
   * Trigger pipeline.
   */
  @Test
  public void triggerPipeline() {
    Pipeline pipeline = createPipeline();
    WorkflowExecution execution = workflowService.triggerPipelineExecution(appId, pipeline.getUuid());
    assertThat(execution).isNotNull();
    String executionId = execution.getUuid();
    logger.debug("Pipeline executionId: {}", executionId);
    assertThat(executionId).isNotNull();
    Misc.quietSleep(2000);
    execution = workflowService.getExecutionDetails(appId, executionId);
    assertThat(execution).isNotNull();
    assertThat(execution.getUuid()).isEqualTo(executionId);
    assertThat(execution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  /**
   * Stencils.
   *
   * @throws IllegalAccessException    the illegal access exception
   * @throws IllegalArgumentException  the illegal argument exception
   * @throws InvocationTargetException the invocation target exception
   * @throws IntrospectionException    the introspection exception
   */
  @Test
  public void stencils()
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
    Map<StateTypeScope, List<StateTypeDescriptor>> stencils = workflowService.stencils();
    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(2).containsKeys(
        StateTypeScope.ORCHESTRATION_STENCILS, StateTypeScope.PIPELINE_STENCILS);
    assertThat(stencils.get(StateTypeScope.ORCHESTRATION_STENCILS))
        .extracting(StateTypeDescriptor::getType)
        .contains(/*"DEPLOY",*/ "START", "STOP");
  }

  /**
   * Stencils for pipeline.
   *
   * @throws IllegalAccessException    the illegal access exception
   * @throws IllegalArgumentException  the illegal argument exception
   * @throws InvocationTargetException the invocation target exception
   * @throws IntrospectionException    the introspection exception
   */
  @Test
  public void stencilsForPipeline()
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
    Map<StateTypeScope, List<StateTypeDescriptor>> stencils =
        workflowService.stencils(StateTypeScope.PIPELINE_STENCILS);
    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(1).containsKeys(StateTypeScope.PIPELINE_STENCILS);
    assertThat(stencils.get(StateTypeScope.PIPELINE_STENCILS))
        .extracting(StateTypeDescriptor::getType)
        .contains("BUILD", "ENV_STATE")
        .doesNotContain("START", "STOP");
  }

  /**
   * Stencils for orchestration.
   *
   * @throws IllegalAccessException    the illegal access exception
   * @throws IllegalArgumentException  the illegal argument exception
   * @throws InvocationTargetException the invocation target exception
   * @throws IntrospectionException    the introspection exception
   */
  @Test
  public void stencilsForOrchestration()
      throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, IntrospectionException {
    Map<StateTypeScope, List<StateTypeDescriptor>> stencils =
        workflowService.stencils(StateTypeScope.ORCHESTRATION_STENCILS);
    logger.debug(JsonUtils.asJson(stencils));
    assertThat(stencils).isNotNull().hasSize(1).containsKeys(StateTypeScope.ORCHESTRATION_STENCILS);
    assertThat(stencils.get(StateTypeScope.ORCHESTRATION_STENCILS))
        .extracting(StateTypeDescriptor::getType)
        .doesNotContain("BUILD", "ENV_STATE")
        .contains("START", "STOP" /*,"DEPLOY"*/);
  }
}
