package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.WingsBaseTest;
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
import software.wings.sm.Transition;
import software.wings.sm.TransitionType;
import software.wings.sm.states.ForkState;
import software.wings.utils.Misc;
import software.wings.waitnotify.NotifyEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;

/**
 * @author Rishi
 */
@Listeners(NotifyEventListener.class)
public class WorkflowServiceTest extends WingsBaseTest {
  @Inject private WorkflowService workflowService;

  @Inject private WingsPersistence wingsPersistence;

  private String appId = UUIDGenerator.getUuid();

  @Test
  public void shouldSaveAndRead() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId("APP_ID");
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);
    sm.setInitialStateName(stateA.getName());

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, stateB));
    sm.addTransition(new Transition(stateB, TransitionType.SUCCESS, stateC));

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    String smId = sm.getUuid();
    sm = wingsPersistence.get(StateMachine.class, smId);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();
    System.out.println("All Done!");
  }

  @Test
  public void shouldTrigger() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId("APP_ID");
    State stateA = new StateMachineTest.StateSynch("stateA" + new Random().nextInt(10000));
    sm.addState(stateA);
    StateMachineTest.StateSynch stateB = new StateMachineTest.StateSynch("stateB" + new Random().nextInt(10000));
    sm.addState(stateB);
    StateMachineTest.StateSynch stateC = new StateMachineTest.StateSynch("stateC" + new Random().nextInt(10000));
    sm.addState(stateC);
    sm.setInitialStateName(stateA.getName());

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, stateB));
    sm.addTransition(new Transition(stateB, TransitionType.SUCCESS, stateC));

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    workflowService.trigger(smId);
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

  @Test
  public void shouldTriggerAsynch() throws InterruptedException {
    StateMachine sm = createAsynchSM(workflowService);
    sm.setAppId("APP_ID");
    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    workflowService.trigger(smId);

    Thread.sleep(10000);
  }

  private StateMachine createAsynchSM(WorkflowService svc) {
    StateMachine sm = new StateMachine();
    sm.setAppId("APP_ID");
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

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, stateAB));
    sm.addTransition(new Transition(stateAB, TransitionType.SUCCESS, stateB));
    sm.addTransition(new Transition(stateB, TransitionType.SUCCESS, stateBC));
    sm.addTransition(new Transition(stateBC, TransitionType.SUCCESS, stateC));

    sm = svc.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();
    return sm;
  }

  @Test
  public void shouldTriggerSimpleFork() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId("APP_ID");
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

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, fork1));

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    workflowService.trigger(smId);

    Thread.sleep(5000);
  }

  @Test
  public void shouldTriggerMixedFork() throws InterruptedException {
    StateMachine sm = new StateMachine();
    sm.setAppId("APP_ID");
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

    sm.addTransition(new Transition(stateA, TransitionType.SUCCESS, stateAB));
    sm.addTransition(new Transition(stateAB, TransitionType.SUCCESS, fork1));
    sm.addTransition(new Transition(fork1, TransitionType.SUCCESS, stateC));

    sm = workflowService.create(sm);
    assertThat(sm).isNotNull();
    assertThat(sm.getUuid()).isNotNull();

    String smId = sm.getUuid();
    System.out.println("Going to trigger state machine");
    workflowService.trigger(smId);

    Thread.sleep(10000);
  }

  @Test
  public void shouldCreatePipelineWithGraph() {
    createPipeline();
  }

  @Test
  public void shouldUpdatePipelineWithGraph() {
    Pipeline pipeline = createPipeline();
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

    Pipeline updatedPipeline = workflowService.updateWorkflow(Pipeline.class, pipeline);
    assertThat(updatedPipeline).isNotNull();
    assertThat(updatedPipeline.getUuid()).isNotNull();
    assertThat(updatedPipeline.getUuid()).isEqualTo(pipeline.getUuid());

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValue(pipeline.getUuid());
    filter.setOp(Operator.EQ);
    req.getFilters().add(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(2);

    StateMachine sm = workflowService.readLatest(updatedPipeline.getUuid(), null);
    assertThat(sm.getGraph()).isNotNull();
    assertThat(sm.getGraph()).isEqualTo(graph);
  }

  private Pipeline createPipeline() {
    Pipeline pipeline = new Pipeline();
    pipeline.setName("pipeline1");
    pipeline.setDescription("Sample Pipeline");

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

    pipeline.setGraph(graph);

    pipeline = workflowService.createWorkflow(Pipeline.class, pipeline);
    assertThat(pipeline).isNotNull();
    assertThat(pipeline.getUuid()).isNotNull();

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValue(pipeline.getUuid());
    filter.setOp(Operator.EQ);
    req.getFilters().add(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(1);
    assertThat(res.get(0)).isNotNull();

    StateMachine sm = res.get(0);
    assertThat(sm.getGraph()).isNotNull();
    assertThat(sm.getGraph()).isEqualTo(graph);

    return pipeline;
  }

  @Test
  public void shouldCreateOrchestration() {
    createOrchestration();
  }

  @Test
  public void shouldUpdateOrchestration() {
    Orchestration orchestration = createOrchestration();
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

    Orchestration updatedOrchestration = workflowService.updateWorkflow(Orchestration.class, orchestration);
    assertThat(updatedOrchestration).isNotNull();
    assertThat(updatedOrchestration.getUuid()).isNotNull();
    assertThat(updatedOrchestration.getUuid()).isEqualTo(orchestration.getUuid());

    PageRequest<StateMachine> req = new PageRequest<>();
    SearchFilter filter = new SearchFilter();
    filter.setFieldName("originId");
    filter.setFieldValue(orchestration.getUuid());
    filter.setOp(Operator.EQ);
    req.getFilters().add(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(2);

    StateMachine sm = workflowService.readLatest(updatedOrchestration.getUuid(), null);
    assertThat(sm.getGraph()).isNotNull();
    assertThat(sm.getGraph()).isEqualTo(graph);
  }

  private Orchestration createOrchestration() {
    Orchestration orchestration = new Orchestration();
    orchestration.setAppId(appId);
    orchestration.setName("workflow1");
    orchestration.setDescription("Sample Workflow");

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
    filter.setFieldValue(orchestration.getUuid());
    filter.setOp(Operator.EQ);
    req.getFilters().add(filter);
    PageResponse<StateMachine> res = workflowService.list(req);

    assertThat(res).isNotNull();
    assertThat(res.size()).isEqualTo(1);
    assertThat(res.get(0)).isNotNull();

    StateMachine sm = res.get(0);
    assertThat(sm.getGraph()).isNotNull();
    assertThat(sm.getGraph()).isEqualTo(graph);

    return orchestration;
  }

  @Test
  public void triggerOrchestration() {
    Orchestration orchestration = createOrchestration();
    WorkflowExecution execution = workflowService.triggerOrchestrationExecution(appId, orchestration.getUuid(), null);
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

  private final Logger logger = LoggerFactory.getLogger(getClass());
}
