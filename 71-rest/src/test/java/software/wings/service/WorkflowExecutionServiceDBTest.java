package software.wings.service;

import static io.harness.beans.ExecutionStatus.PAUSED;
import static io.harness.beans.ExecutionStatus.RUNNING;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.beans.ExecutionStatus.WAITING;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.HARSH;
import static io.harness.rule.OwnerRule.RAGHU;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.api.InstanceElement.Builder.anInstanceElement;
import static software.wings.beans.CountsByStatuses.Builder.aCountsByStatuses;
import static software.wings.beans.ElementExecutionSummary.ElementExecutionSummaryBuilder.anElementExecutionSummary;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.service.impl.workflow.WorkflowServiceTestHelper.constructPipeline;
import static software.wings.sm.InstanceStatusSummary.InstanceStatusSummaryBuilder.anInstanceStatusSummary;
import static software.wings.sm.StateMachine.StateMachineBuilder.aStateMachine;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.STATE_MACHINE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Instance;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.HostElement;
import software.wings.api.InstanceElement;
import software.wings.api.jira.JiraExecutionData;
import software.wings.app.GeneralNotifyEventListener;
import software.wings.beans.ArtifactVariable;
import software.wings.beans.CountsByStatuses;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.PipelineExecution;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.WingsPersistence;
import software.wings.events.TestUtils;
import software.wings.resources.WorkflowResource;
import software.wings.rules.Listeners;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateMachine;
import software.wings.sm.StateMachineExecutionSimulator;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.BarrierState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class workflowExecutionServiceTest.
 *
 * @author Rishi
 */
@Listeners(GeneralNotifyEventListener.class)
public class WorkflowExecutionServiceDBTest extends WingsBaseTest {
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Inject private WingsPersistence wingsPersistence;
  @Inject private StateMachineExecutionSimulator stateMachineExecutionSimulator;
  @Inject private WorkflowResource workflowResource;
  @Inject private HostService hostService;
  @Mock private UserGroupService userGroupService;
  @Mock private AuthHandler authHandler;
  @Mock private AppService appService;
  @Inject private TestUtils eventTestHelper;
  @Mock private ArtifactService artifactService;
  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private User user;

  @Before
  public void init() {
    wingsPersistence.save(aStateMachine()
                              .withAppId(APP_ID)
                              .withUuid(STATE_MACHINE_ID)
                              .withInitialStateName("foo")
                              .addState(new BarrierState("foo"))
                              .build());

    when(artifactService.listByIds(any(), any())).thenReturn(Collections.emptyList());
    when(serviceInstanceService.fetchServiceInstances(any(), any())).thenReturn(Collections.emptyList());
    when(appService.getAccountIdByAppId(any())).thenReturn(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void shouldListExecutions() {
    CountsByStatuses countsByStatuses = aCountsByStatuses().build();

    final WorkflowExecutionBuilder workflowExecutionBuilder = WorkflowExecution.builder().appId(APP_ID).envId(ENV_ID);

    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(SUCCESS).build());
    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid())
                              .status(ExecutionStatus.ERROR)
                              .breakdown(countsByStatuses)
                              .build());
    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid())
                              .status(ExecutionStatus.FAILED)
                              .breakdown(countsByStatuses)
                              .build());
    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(ExecutionStatus.ABORTED).build());

    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(RUNNING).build());

    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(PAUSED).build());
    wingsPersistence.save(workflowExecutionBuilder.uuid(generateUuid()).status(WAITING).build());

    PageResponse<WorkflowExecution> pageResponse = workflowExecutionService.listExecutions(
        aPageRequest().addFilter(WorkflowExecutionKeys.appId, Operator.EQ, APP_ID).build(), false, true, false, true);
    assertThat(pageResponse).isNotNull();
    assertThat(pageResponse.size()).isEqualTo(7);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldListExecutionsForPipeline() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.PIPELINE);
    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.PIPELINE, SUCCESS);

    workflowExecution.setPipelineExecution(createAndFetchPipelineExecution(SUCCESS));
    wingsPersistence.save(workflowExecution);

    PageResponse<WorkflowExecution> pageResponse = workflowExecutionService.listExecutions(
        aPageRequest().addFilter(WorkflowExecutionKeys.appId, Operator.EQ, APP_ID).build(), false, true, false, true);
    assertThat(pageResponse).isNotNull();
    assertThat(pageResponse.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldFetchExecutionWithoutSummary() {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(WorkflowType.ORCHESTRATION);
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(generateUuid());

    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.ORCHESTRATION, PAUSED);

    workflowExecution.setWorkflowType(WorkflowType.PIPELINE);
    workflowExecution.setPipelineExecution(createAndFetchPipelineExecution(PAUSED));

    wingsPersistence.save(workflowExecution);

    WorkflowExecution retrievedWorkflowExecution =
        workflowExecutionService.getExecutionWithoutSummary(APP_ID, workflowExecution.getUuid());

    assertThat(retrievedWorkflowExecution.getUuid()).isEqualTo(workflowExecution.getUuid());

    Map<String, String> serviceInstances = new HashMap<>();
    serviceInstances.put("foo", "bar");

    executionArgs.setServiceInstanceIdNames(serviceInstances);

    Map<String, String> artifactIdNames = new HashMap<>();
    serviceInstances.put("foo", "bar");

    executionArgs.setArtifactIdNames(artifactIdNames);

    WorkflowExecution workflowExecution1 = createWorkflowExecution(executionArgs, WorkflowType.ORCHESTRATION, PAUSED);

    wingsPersistence.save(workflowExecution1);

    WorkflowExecution retrievedWorkflowExecution1 =
        workflowExecutionService.getExecutionWithoutSummary(APP_ID, workflowExecution1.getUuid());

    assertThat(retrievedWorkflowExecution1.getUuid()).isEqualTo(workflowExecution1.getUuid());
    assertThat(retrievedWorkflowExecution1.getWorkflowType()).isEqualTo(workflowExecution1.getWorkflowType());
    assertThat(retrievedWorkflowExecution1.getStatus()).isEqualTo(workflowExecution1.getStatus());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldTestExecutionDetails() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.PIPELINE);
    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.PIPELINE, PAUSED);

    workflowExecution.setPipelineExecution(createAndFetchPipelineExecution(PAUSED));
    wingsPersistence.save(workflowExecution);

    WorkflowExecution retrievedWorkflowExecution =
        workflowExecutionService.getExecutionDetailsWithoutGraph(APP_ID, workflowExecution.getUuid());

    assertThat(retrievedWorkflowExecution.getUuid()).isEqualTo(workflowExecution.getUuid());
    assertThat(retrievedWorkflowExecution.getStatus()).isEqualTo(workflowExecution.getStatus());
    assertThat(retrievedWorkflowExecution.getPipelineExecution().getPipelineStageExecutions().get(0).getStateName())
        .isEqualTo("STAGE");
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldTestExecutionDetailsWithFinalStage() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.PIPELINE);
    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.PIPELINE, SUCCESS);

    workflowExecution.setPipelineExecution(createAndFetchPipelineExecution(SUCCESS));
    wingsPersistence.save(workflowExecution);

    WorkflowExecution retrievedWorkflowExecution =
        workflowExecutionService.getExecutionDetailsWithoutGraph(APP_ID, workflowExecution.getUuid());

    assertThat(retrievedWorkflowExecution.getUuid()).isEqualTo(workflowExecution.getUuid());
    assertThat(retrievedWorkflowExecution.getStatus()).isEqualTo(workflowExecution.getStatus());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldTestExecutionDetailsWithFinalStage1() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.PIPELINE);
    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.PIPELINE, SUCCESS);

    workflowExecution.setPipelineExecution(createAndFetchPipelineExecution(SUCCESS));
    wingsPersistence.save(workflowExecution);

    WorkflowExecution retrievedWorkflowExecution =
        workflowExecutionService.getExecutionDetailsWithoutGraph(APP_ID, workflowExecution.getUuid());

    assertThat(retrievedWorkflowExecution.getUuid()).isEqualTo(workflowExecution.getUuid());
    assertThat(retrievedWorkflowExecution.getStatus()).isEqualTo(workflowExecution.getStatus());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldUpdateNotes() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.PIPELINE);
    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.PIPELINE, SUCCESS);

    workflowExecution.setPipelineExecution(createAndFetchPipelineExecution(SUCCESS));
    wingsPersistence.save(workflowExecution);

    executionArgs.setNotes("Notes");
    assertThat(
        workflowExecutionService.updateNotes(workflowExecution.getAppId(), workflowExecution.getUuid(), executionArgs))
        .isTrue();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldAppendInfraMappingIds() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.PIPELINE);
    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.PIPELINE, SUCCESS);

    workflowExecution.setPipelineExecution(createAndFetchPipelineExecution(SUCCESS));
    wingsPersistence.save(workflowExecution);
    assertThat(workflowExecutionService.appendInfraMappingId(
                   workflowExecution.getAppId(), workflowExecution.getUuid(), generateUuid()))
        .isTrue();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldUpdateInfraMappingIds() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.PIPELINE);
    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.PIPELINE, SUCCESS);

    workflowExecution.setInfraMappingIds(asList(generateUuid()));
    workflowExecution.setPipelineExecution(createAndFetchPipelineExecution(SUCCESS));
    wingsPersistence.save(workflowExecution);
    assertThat(workflowExecutionService.appendInfraMappingId(
                   workflowExecution.getAppId(), workflowExecution.getUuid(), generateUuid()))
        .isTrue();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldGetApprovalAuthorization() {
    user = eventTestHelper.createUser(null);
    UserThreadLocal.set(user);
    String workflowId = generateUuid();
    when(authHandler.authorize(any(), any(), any())).thenReturn(true);
    assertThat(workflowExecutionService.verifyAuthorizedToAcceptOrReject(null, asList(APP_ID), workflowId)).isTrue();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldObtainLastGoodDeployedVariables() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.ORCHESTRATION);
    String serviceId = generateUuid();
    String workflowId = generateUuid();
    ArtifactVariable artifactVariable =
        ArtifactVariable.builder().name("ArtifactVariable").entityType(SERVICE).entityId(serviceId).build();
    executionArgs.setArtifactVariables(asList(artifactVariable));

    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.ORCHESTRATION, SUCCESS);
    workflowExecution.setWorkflowId(workflowId);

    wingsPersistence.save(workflowExecution);
    assertThat(
        workflowExecutionService.obtainLastGoodDeployedArtifactsVariables(workflowExecution.getAppId(), workflowId))
        .isNotEmpty();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldObtainLastGoodDeployedArtifacts() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.ORCHESTRATION);
    String workflowId = generateUuid();

    Artifact artifact = Artifact.Builder.anArtifact()
                            .withAppId("APP_ID")
                            .withArtifactStreamId(generateUuid())
                            .withMetadata(ImmutableMap.of(ArtifactMetadataKeys.buildNo, "1.2"))
                            .withDisplayName("Some artifact")
                            .build();

    executionArgs.setArtifacts(asList(artifact));

    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.ORCHESTRATION, SUCCESS);
    workflowExecution.setWorkflowId(workflowId);

    wingsPersistence.save(workflowExecution);
    assertThat(workflowExecutionService.obtainLastGoodDeployedArtifacts(workflowExecution.getAppId(), workflowId))
        .isNotEmpty();
  }
  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldRefreshCollectedArtifacts() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.ORCHESTRATION);
    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.ORCHESTRATION, SUCCESS);
    WorkflowExecution workflowExecution1 = createWorkflowExecution(executionArgs, WorkflowType.ORCHESTRATION, SUCCESS);

    String envId = generateUuid();
    String serviceId = generateUuid();
    String workflowId = generateUuid();

    workflowExecution.setEnvIds(asList(envId));
    workflowExecution.setServiceIds(asList(serviceId));
    workflowExecution.setWorkflowId(workflowId);

    Artifact artifact = Artifact.Builder.anArtifact()
                            .withAppId(workflowExecution.getAppId())
                            .withArtifactStreamId(generateUuid())
                            .withMetadata(ImmutableMap.of(ArtifactMetadataKeys.buildNo, "1.2"))
                            .withDisplayName("Some artifact")
                            .build();

    Artifact artifact1 = Artifact.Builder.anArtifact()
                             .withAppId(workflowExecution.getAppId())
                             .withArtifactStreamId(generateUuid())
                             .withMetadata(ImmutableMap.of(ArtifactMetadataKeys.buildNo, "1.2"))
                             .withDisplayName("Some artifact")
                             .build();

    workflowExecution.setArtifacts(asList(artifact));
    workflowExecution1.setArtifacts(asList(artifact, artifact1));

    workflowExecution.setPipelineExecution(createAndFetchPipelineExecution(SUCCESS));
    wingsPersistence.save(workflowExecution);
    wingsPersistence.save(workflowExecution1);
    workflowExecutionService.refreshCollectedArtifacts(
        workflowExecution.getAppId(), workflowExecution.getUuid(), workflowExecution1.getUuid());

    WorkflowExecution retrievedWorkflowExecution =
        workflowExecutionService.fetchLastWorkflowExecution(workflowExecution.getAppId(), workflowId, serviceId, envId);

    assertThat(retrievedWorkflowExecution.getUuid()).isEqualTo(workflowExecution.getUuid());
    assertThat(retrievedWorkflowExecution.getArtifacts()).isEqualTo(asList(artifact, artifact1));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldFetchLatestExecutionForServiceIds() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.ORCHESTRATION);
    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.ORCHESTRATION, SUCCESS);

    String envId = generateUuid();
    String serviceId = generateUuid();
    String workflowId = generateUuid();

    workflowExecution.setEnvIds(asList(envId));
    workflowExecution.setServiceIds(asList(serviceId));
    workflowExecution.setWorkflowId(workflowId);

    workflowExecution.setPipelineExecution(createAndFetchPipelineExecution(SUCCESS));
    wingsPersistence.save(workflowExecution);
    WorkflowExecution retrievedWorkflowExecution =
        workflowExecutionService.fetchLastWorkflowExecution(workflowExecution.getAppId(), workflowId, serviceId, envId);

    assertThat(retrievedWorkflowExecution.getUuid()).isEqualTo(workflowExecution.getUuid());
    assertThat(retrievedWorkflowExecution.getStatus()).isEqualTo(workflowExecution.getStatus());
    assertThat(retrievedWorkflowExecution.getServiceIds()).isEqualTo(asList(serviceId));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldFetchExecutionForVerification() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.ORCHESTRATION);
    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.ORCHESTRATION, SUCCESS);

    String envId = generateUuid();
    String serviceId = generateUuid();
    String workflowId = generateUuid();

    workflowExecution.setEnvIds(asList(envId));
    workflowExecution.setServiceIds(asList(serviceId));
    workflowExecution.setWorkflowId(workflowId);

    workflowExecution.setPipelineExecution(createAndFetchPipelineExecution(SUCCESS));
    wingsPersistence.save(workflowExecution);
    WorkflowExecution retrievedWorkflowExecution = workflowExecutionService.getWorkflowExecutionForVerificationService(
        workflowExecution.getAppId(), workflowExecution.getUuid());

    assertThat(retrievedWorkflowExecution.getUuid()).isEqualTo(workflowExecution.getUuid());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldFetchLatestExecutionForInfraMappingIds() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.ORCHESTRATION);
    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.ORCHESTRATION, SUCCESS);

    String infraId = generateUuid();
    workflowExecution.setInfraMappingIds(asList(infraId));
    workflowExecution.setPipelineExecution(createAndFetchPipelineExecution(SUCCESS));
    wingsPersistence.save(workflowExecution);
    WorkflowExecution retrievedWorkflowExecution =
        workflowExecutionService.getLatestExecutionsFor(workflowExecution.getAppId(), infraId, 1, null, false).get(0);

    assertThat(retrievedWorkflowExecution.getUuid()).isEqualTo(workflowExecution.getUuid());
    assertThat(retrievedWorkflowExecution.getStatus()).isEqualTo(workflowExecution.getStatus());
    assertThat(retrievedWorkflowExecution.getInfraMappingIds()).isEqualTo(asList(infraId));
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldFetchWorkflowExecutionList() {
    ExecutionArgs executionArgs = createExecutionArgs(WorkflowType.ORCHESTRATION);
    WorkflowExecution workflowExecution = createWorkflowExecution(executionArgs, WorkflowType.ORCHESTRATION, SUCCESS);

    String envId = generateUuid();
    String workflowId = generateUuid();
    workflowExecution.setEnvIds(asList(envId));
    workflowExecution.setWorkflowId(workflowId);
    wingsPersistence.save(workflowExecution);
    WorkflowExecution retrievedWorkflowExecution =
        workflowExecutionService.fetchWorkflowExecutionList(workflowExecution.getAppId(), workflowId, envId, 0, 1)
            .get(0);

    assertThat(retrievedWorkflowExecution.getUuid()).isEqualTo(workflowExecution.getUuid());
    assertThat(retrievedWorkflowExecution.getStatus()).isEqualTo(workflowExecution.getStatus());
    assertThat(retrievedWorkflowExecution.getEnvIds()).isEqualTo(asList(envId));
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldListDeployedNodes() {
    String appId = generateUuid();
    String envId = generateUuid();
    String workflowId = wingsPersistence.save(aWorkflow().appId(appId).envId(envId).build());
    int numOfExecutionSummaries = 2;
    int numOfHosts = 3;
    List<ElementExecutionSummary> executionSummaries = new ArrayList<>();
    Map<String, HostElement> hostElements = new HashMap<>();
    for (int i = 0; i < numOfExecutionSummaries; i++) {
      List<InstanceStatusSummary> instanceElements = new ArrayList<>();
      for (int j = 0; j < numOfHosts; j++) {
        Instance ec2Instance = new Instance();
        ec2Instance.setPrivateDnsName(generateUuid());
        ec2Instance.setPublicDnsName(generateUuid());

        Host host =
            aHost().withEc2Instance(ec2Instance).withAppId(appId).withEnvId(envId).withHostName(generateUuid()).build();
        String hostId = wingsPersistence.save(host);
        HostElement hostElement = HostElement.builder().hostName(generateUuid()).uuid(hostId).build();
        instanceElements.add(
            anInstanceStatusSummary().withInstanceElement(anInstanceElement().host(hostElement).build()).build());
        hostElements.put(hostId, hostElement);
      }
      executionSummaries.add(anElementExecutionSummary().withInstanceStatusSummaries(instanceElements).build());
    }
    StateMachine stateMachine = new StateMachine();
    stateMachine.setInitialStateName("some-state");
    stateMachine.setStates(Lists.newArrayList(new ApprovalState(stateMachine.getInitialStateName())));
    stateMachine.setAppId(appId);
    String stateMachineId = wingsPersistence.save(stateMachine);

    WorkflowExecution workflowExecution = WorkflowExecution.builder()
                                              .appId(appId)
                                              .envId(envId)
                                              .stateMachine(stateMachine)
                                              .workflowId(workflowId)
                                              .status(ExecutionStatus.SUCCESS)
                                              .serviceExecutionSummaries(executionSummaries)
                                              .build();
    wingsPersistence.save(workflowExecution);
    List<InstanceElement> deployedNodes = workflowResource.getDeployedNodes(appId, workflowId).getResource();
    assertThat(deployedNodes).hasSize(numOfExecutionSummaries * numOfHosts);
    deployedNodes.forEach(deployedNode -> {
      assertThat(hostElements.containsKey(deployedNode.getHost().getUuid())).isTrue();
      HostElement hostElement = hostElements.get(deployedNode.getHost().getUuid());
      assertThat(deployedNode.getHost().getHostName()).isEqualTo(hostElement.getHostName());
      assertThat(deployedNode.getHost().getEc2Instance())
          .isEqualTo(hostService.get(appId, envId, hostElement.getUuid()).getEc2Instance());
    });
  }

  private WorkflowExecution createWorkflowExecution(
      ExecutionArgs executionArgs, WorkflowType workflowType, ExecutionStatus executionStatus) {
    return WorkflowExecution.builder()
        .appId(APP_ID)
        .appName(APP_NAME)
        .envType(NON_PROD)
        .status(executionStatus)
        .workflowType(workflowType)
        .executionArgs(executionArgs)
        .uuid(generateUuid())
        .build();
  }

  private PipelineExecution createAndFetchPipelineExecution(ExecutionStatus executionStatus) {
    JiraExecutionData executionData =
        JiraExecutionData.builder().executionStatus(ExecutionStatus.SUCCESS).issueUrl("tempJiraUrl").build();

    PipelineStageExecution pipelineStageExecution = PipelineStageExecution.builder()
                                                        .status(executionStatus)
                                                        .message("Pipeline execution")
                                                        .stateExecutionData(executionData)
                                                        .build();
    return PipelineExecution.Builder.aPipelineExecution()
        .withPipelineStageExecutions(asList(pipelineStageExecution))
        .withStatus(executionStatus)
        .withPipeline(constructPipeline("PipelineName"))
        .build();
  }

  private ExecutionArgs createExecutionArgs(WorkflowType workflowType) {
    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(workflowType);
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(generateUuid());

    return executionArgs;
  }
}