package software.wings.service.impl.instance;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.api.AwsAutoScalingGroupDeploymentInfo;
import software.wings.api.AwsCodeDeployDeploymentInfo;
import software.wings.api.ContainerDeploymentInfoWithLabels;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.DeploymentEvent;
import software.wings.api.DeploymentInfo;
import software.wings.api.DeploymentSummary;
import software.wings.api.DeploymentType;
import software.wings.api.PhaseExecutionData;
import software.wings.api.PhaseStepExecutionData;
import software.wings.api.PhaseStepExecutionData.PhaseStepExecutionDataBuilder;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.CodeDeployInfrastructureMapping.CodeDeployInfrastructureMappingBuilder;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.beans.infrastructure.instance.key.deployment.AwsAmiDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.AwsCodeDeployDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.common.Constants;
import software.wings.core.queue.Queue;
import software.wings.service.impl.instance.sync.ContainerSync;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.PhaseStepExecutionSummary;
import software.wings.sm.WorkflowStandardParams;
import software.wings.sm.states.PhaseStepSubWorkflow;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class InstanceHelperTest extends WingsBaseTest {
  public static final String INFRA_MAP_ID = "infraMap_1";
  public static final String CODE_DEPLOY_DEPLOYMENT_ID = "codeDeployment_id";
  public static final String CODE_DEPLOY_APP_NAME = "codeDeployment_app";
  public static final String CODE_DEPLOY_GROUP_NAME = "codeDeployment_group";
  public static final String CODE_DEPLOY_key = "codeDeployment_key";
  public static final String APP_ID = "app_1";
  public static final String ACCOUNT_ID = "ACCOUNT_ID";
  public static final String SERVICE_ID = "SERVICE_ID";
  public static final String WORKFLOW_EXECUTION_ID = "workflow_1";
  public static final String STATE_EXECUTION_INSTANCE_ID = "stateExeInstanceId_1";
  public static final String CLUSTER_NAME = "clusterName";
  @Mock private InstanceService instanceService;
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private HostService hostService;
  @Mock private AppService appService;
  @Mock private ArtifactService artifactService;
  @Mock private Queue<DeploymentEvent> deploymentEventQueue;
  @Mock private ExecutionContext context;
  @Mock private ContainerSync containerSync;
  @InjectMocks @Spy WorkflowStandardParams workflowStandardParams;

  @InjectMocks @Inject private AwsCodeDeployInstanceHandler awsCodeDeployInstanceHandler;
  @InjectMocks @Inject private AwsAmiInstanceHandler awsAmiInstanceHandler;
  @InjectMocks @Inject private ContainerInstanceHandler containerInstanceHandler;
  @InjectMocks @Inject private InstanceHelper instanceHelper;
  private WorkflowExecution workflowExecution;
  private PhaseExecutionData phaseExecutionData;
  private PhaseStepExecutionData phaseStepExecutionData;
  private long endsAtTime;
  private String PRIVATE_DNS_1 = "ip-171-31-14-5.us-west-2.compute.internal";
  private String PRIVATE_DNS_2 = "ip-172-31-14-5.us-west-2.compute.internal";
  private String HOST_NAME_IP1 = "ip-171-31-14-5";
  private String HOST_NAME_IP2 = "ip-172-31-14-5";
  private String PUBLIC_DNS_1 = "ec2-53-218-107-144.us-west-2.compute.amazonaws.com";
  private String PUBLIC_DNS_2 = "ec2-54-218-107-144.us-west-2.compute.amazonaws.com";
  static com.amazonaws.services.ec2.model.Instance instance1;
  static com.amazonaws.services.ec2.model.Instance instance2;
  private InstanceHelperTestHelper instaceHelperTestHelper = new InstanceHelperTestHelper();
  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    workflowExecution = WorkflowExecutionBuilder.aWorkflowExecution()
                            .withAppId("app_1")
                            .withAppName("app1")
                            .withUuid(WORKFLOW_EXECUTION_ID)
                            .withName("workflow1")
                            .withPipelineSummary(null)
                            .withEnvName("envName")
                            .withEnvId("env_1")
                            .withEnvType(EnvironmentType.PROD)
                            .withTriggeredBy(EmbeddedUser.builder().name("user1").uuid("user_1").build())
                            .build();

    when(instanceService.saveOrUpdate(anyList())).thenAnswer(i -> i.getArguments()[0]);

    doReturn(Application.Builder.anApplication().withName("app").withUuid("app_1").withAccountId(ACCOUNT_ID).build())
        .when(appService)
        .get(anyString());

    doNothing().when(deploymentEventQueue).send(any());

    // This mocking will be used for workflowStandardParams.getArtifactForService(phaseExecutionData.getServiceId())
    workflowStandardParams.setAppId(APP_ID);
    workflowStandardParams.setArtifactIds(asList("artifact_1", "artifact_2"));
    when(artifactService.get(anyString(), anyString())).thenAnswer(invocation -> {
      if (invocation.getArgumentAt(1, String.class).equals("artifact_1")) {
        return Artifact.Builder.anArtifact()
            .withUuid("artifact_1")
            .withDisplayName("artifact1")
            .withArtifactStreamId("artifactStream_1")
            .withArtifactSourceName("sourceName")
            .withMetadata(Collections.singletonMap("buildNo", "1.0"))
            .withServiceIds(asList(SERVICE_ID))
            .build();
      } else {
        return Artifact.Builder.anArtifact()
            .withUuid("artifact_2")
            .withDisplayName("artifact2")
            .withArtifactStreamId("artifactStream_2")
            .withArtifactSourceName("sourceName")
            .withMetadata(Collections.singletonMap("buildNo", "1.0"))
            .withServiceIds(asList("service_2"))
            .build();
      }
    });
    // ------------------------------------------

    instance1 = new com.amazonaws.services.ec2.model.Instance();
    instance1.setPrivateDnsName(PRIVATE_DNS_1);
    instance1.setPublicDnsName(PUBLIC_DNS_1);

    instance2 = new com.amazonaws.services.ec2.model.Instance();
    instance2.setPrivateDnsName(PRIVATE_DNS_2);
    instance2.setPublicDnsName(PUBLIC_DNS_2);

    Set<String> names = new HashSet<>();
    names.add("name1");
    names.add("name2");
    doReturn(names).when(containerSync).getControllerNames(any(), any());
  }

  @Test
  public void testExtractInstanceOrContainerInfoBaseOnType_PDS() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData = instaceHelperTestHelper.initExecutionSummary(
        InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH, Constants.DEPLOY_SERVICE, endsAtTime, "");
    phaseStepExecutionData = getPhaseStepExecutionData(phaseExecutionData);
    doReturn(PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
                 .withUuid(INFRA_MAP_ID)
                 .withInfraMappingType("PHYSICAL_DATA_CENTER_SSH")
                 .withAppId(APP_ID)
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());

    doReturn(Host.Builder.aHost().withHostName("hostName").withUuid("host_1").withPublicDns("host1").build())
        .when(hostService)
        .get(anyString(), anyString(), anyString());

    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow("Name");
    phaseStepSubWorkflow.setRollback(false);

    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, APP_ID, workflowExecution, phaseStepSubWorkflow, null);

    // Capture the argument of the doSomething function
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(instanceService).saveOrUpdate(captor.capture());

    /*
     * We have mocked instanceService.saveOrUpdate() for Physical hosts to doNothing as we dont want to test save
     * functionality here. The way we test here is we capture generated Instance dtos those are passed to saveOrUpdate
     * method and we validate them to see if they are valid
     * */
    List instances = captor.getValue();
    instaceHelperTestHelper.assertInstances(instances, InstanceType.PHYSICAL_HOST_INSTANCE,
        InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH,
        HostInstanceKey.builder().hostName("hostName").infraMappingId(INFRA_MAP_ID).build(),
        HostInstanceKey.builder().hostName("hostName").infraMappingId(INFRA_MAP_ID).build(),
        PhysicalHostInstanceInfo.builder().hostId("host_1").hostName("hostName").hostPublicDns("host1").build(),
        PhysicalHostInstanceInfo.builder().hostId("host_1").hostName("hostName").hostPublicDns("host1").build(),
        endsAtTime);
  }

  @Test
  public void testExtractInstanceOrContainerInfoBaseOnType_For_AWS_SSH_AmiInfraMapping() {
    endsAtTime = System.currentTimeMillis();
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData = instaceHelperTestHelper.initExecutionSummary(
        InfrastructureMappingType.AWS_SSH, Constants.DEPLOY_SERVICE, endsAtTime, DeploymentType.SSH.name());
    phaseStepExecutionData = getPhaseStepExecutionData(phaseExecutionData);
    doReturn(AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping()
                 .withUuid(INFRA_MAP_ID)
                 .withInfraMappingType(InfrastructureMappingType.AWS_SSH.getName())
                 .withAppId(APP_ID)
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());

    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow("Name");
    phaseStepSubWorkflow.setRollback(false);
    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, APP_ID, workflowExecution, phaseStepSubWorkflow, null);

    // Capture the argument of the doSomething function
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(instanceService).saveOrUpdate(captor.capture());

    /*
     * We have mocked instanceService.saveOrUpdate() for AWS_SSH_AMI  to doNothing as we dont want to test save
     * functionality here. The way we test here is we capture generated Instance dtos those are passed to saveOrUpdate
     * method and we validate them to see if they are valid
     * */
    List instances = captor.getValue();
    instaceHelperTestHelper.assertInstances(instances, InstanceType.EC2_CLOUD_INSTANCE,
        InfrastructureMappingType.AWS_SSH,
        HostInstanceKey.builder().hostName(HOST_NAME_IP1).infraMappingId(INFRA_MAP_ID).build(),
        HostInstanceKey.builder().hostName(HOST_NAME_IP2).infraMappingId(INFRA_MAP_ID).build(),
        Ec2InstanceInfo.builder().hostName(HOST_NAME_IP1).hostPublicDns(PUBLIC_DNS_1).ec2Instance(instance1).build(),
        Ec2InstanceInfo.builder().hostName(HOST_NAME_IP2).hostPublicDns(PUBLIC_DNS_2).ec2Instance(instance2).build(),
        endsAtTime);
  }

  @Test
  public void testExtractInstanceOrContainerInfoBaseOnType_For_AWS_SSH_CodeDeployInfraMapping() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData = instaceHelperTestHelper.initExecutionSummary(
        InfrastructureMappingType.AWS_SSH, Constants.DEPLOY_SERVICE, endsAtTime, DeploymentType.SSH.name());
    phaseStepExecutionData = getPhaseStepExecutionData(phaseExecutionData);
    doReturn(CodeDeployInfrastructureMappingBuilder.aCodeDeployInfrastructureMapping()
                 .withUuid(INFRA_MAP_ID)
                 .withInfraMappingType(InfrastructureMappingType.AWS_SSH.getName())
                 .withAppId(APP_ID)
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());

    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow("Name");
    phaseStepSubWorkflow.setRollback(false);
    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, "app_1", workflowExecution, phaseStepSubWorkflow, null);

    // Capture the argument of the doSomething function
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(instanceService).saveOrUpdate(captor.capture());

    /*
     * We have mocked instanceService.saveOrUpdate() for AWS_SSH_CODEDEPLOY  to doNothing as we dont want to test save
     * functionality here. The way we test here is we capture generated Instance dtos those are passed to saveOrUpdate
     * method and we validate them to see if they are valid
     * */
    List instances = captor.getValue();
    instaceHelperTestHelper.assertInstances(instances, InstanceType.EC2_CLOUD_INSTANCE,
        InfrastructureMappingType.AWS_SSH,
        HostInstanceKey.builder().hostName(HOST_NAME_IP1).infraMappingId(INFRA_MAP_ID).build(),
        HostInstanceKey.builder().hostName(HOST_NAME_IP2).infraMappingId(INFRA_MAP_ID).build(),
        Ec2InstanceInfo.builder().hostName(HOST_NAME_IP1).hostPublicDns(PUBLIC_DNS_1).ec2Instance(instance1).build(),
        Ec2InstanceInfo.builder().hostName(HOST_NAME_IP2).hostPublicDns(PUBLIC_DNS_2).ec2Instance(instance2).build(),
        endsAtTime);
  }

  @Test
  public void testExtractInstanceOrContainerInfoBaseOnType_For_AMI() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData = instaceHelperTestHelper.initExecutionSummary(
        InfrastructureMappingType.AWS_AMI, Constants.DEPLOY_SERVICE, endsAtTime, DeploymentType.AMI.name());
    phaseStepExecutionData = getPhaseStepExecutionData(phaseExecutionData);
    doReturn(AwsAmiInfrastructureMapping.Builder.anAwsAmiInfrastructureMapping()
                 .withUuid(INFRA_MAP_ID)
                 .withInfraMappingType(InfrastructureMappingType.AWS_AMI.getName())
                 .withAppId(APP_ID)
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());

    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow("Name");
    phaseStepSubWorkflow.setRollback(false);
    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, "app_1", workflowExecution, phaseStepSubWorkflow, null);

    // Capture the argument of the doSomething function
    ArgumentCaptor<DeploymentEvent> captor = ArgumentCaptor.forClass(DeploymentEvent.class);
    verify(deploymentEventQueue).send(captor.capture());

    /*
     * The way we test here is,
     * We have mocked deploymentQueueEvent.send(DeploymentEvent) to doNothing as we dont want to process queue.
     * We capture the argument i.e. deploymentEvent that is generated and sent to deploymentEventQueue, and
     * validate to check its valid
     * */
    DeploymentEvent event = captor.getValue();
    assertNotNull(event);
    assertEquals(0, event.getRetries());
    assertNotNull(event);
    assertNotNull(event.getDeploymentSummaries());
    assertEquals(2, event.getDeploymentSummaries().size());
    DeploymentInfo deploymentInfo1 = event.getDeploymentSummaries().get(0).getDeploymentInfo();
    DeploymentInfo deploymentInfo2 = event.getDeploymentSummaries().get(1).getDeploymentInfo();
    assertTrue(deploymentInfo1 instanceof AwsAutoScalingGroupDeploymentInfo);
    assertTrue(deploymentInfo2 instanceof AwsAutoScalingGroupDeploymentInfo);
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(1));
    assertTrue(asList("asgNew", "asgOld")
                   .contains(((AwsAmiDeploymentKey) event.getDeploymentSummaries().get(0).getAwsAmiDeploymentKey())
                                 .getAutoScalingGroupName()));
    assertTrue(asList("asgNew", "asgOld")
                   .contains(((AwsAmiDeploymentKey) event.getDeploymentSummaries().get(1).getAwsAmiDeploymentKey())
                                 .getAutoScalingGroupName()));
  }

  private PhaseStepExecutionData getPhaseStepExecutionData(PhaseExecutionData phaseExecutionData) {
    Collection<PhaseStepExecutionSummary> phaseStepExecutionSummaries =
        phaseExecutionData.getPhaseExecutionSummary().getPhaseStepExecutionSummaryMap().values();
    Optional<PhaseStepExecutionSummary> phaseStepExecutionSummary = phaseStepExecutionSummaries.stream().findFirst();
    PhaseStepExecutionDataBuilder phaseStepExecutionDataBuilder =
        PhaseStepExecutionDataBuilder.aPhaseStepExecutionData();
    phaseStepExecutionDataBuilder.withElementStatusSummary(phaseExecutionData.getElementStatusSummary());
    phaseStepExecutionDataBuilder.withPhaseStepExecutionSummary(phaseStepExecutionSummary.get());
    phaseStepExecutionDataBuilder.withEndTs(phaseExecutionData.getEndTs());
    return phaseStepExecutionDataBuilder.build();
  }

  @Test
  public void testExtractInstanceOrContainerInfoBaseOnType_For_CodeDeploy() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData = instaceHelperTestHelper.initExecutionSummary(InfrastructureMappingType.AWS_AWS_CODEDEPLOY,
        Constants.DEPLOY_SERVICE, endsAtTime, DeploymentType.AWS_CODEDEPLOY.getDisplayName());

    phaseStepExecutionData = getPhaseStepExecutionData(phaseExecutionData);

    phaseStepExecutionData.getPhaseStepExecutionSummary();

    doReturn(CodeDeployInfrastructureMappingBuilder.aCodeDeployInfrastructureMapping()
                 .withUuid(INFRA_MAP_ID)
                 .withInfraMappingType(InfrastructureMappingType.AWS_AWS_CODEDEPLOY.getName())
                 .withAppId(APP_ID)
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());

    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow("Name");
    phaseStepSubWorkflow.setRollback(false);
    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, "app_1", workflowExecution, phaseStepSubWorkflow, null);

    // Capture the argument of the doSomething function
    ArgumentCaptor<DeploymentEvent> captor = ArgumentCaptor.forClass(DeploymentEvent.class);
    verify(deploymentEventQueue).send(captor.capture());

    /*
     * The way we test here is,
     * We have mocked deploymentQueueEvent.send(DeploymentEvent) to doNothing as we dont want to process queue.
     * We capture the argument i.e. deploymentEvent that is generated and sent to deploymentEventQueue, and
     * validate to check its valid
     * */
    DeploymentEvent event = captor.getValue();
    assertNotNull(event);
    assertEquals(0, event.getRetries());

    assertNotNull(event);
    assertNotNull(event.getDeploymentSummaries());
    assertEquals(1, event.getDeploymentSummaries().size());
    DeploymentInfo deploymentInfo = event.getDeploymentSummaries().get(0).getDeploymentInfo();
    assertTrue(deploymentInfo instanceof AwsCodeDeployDeploymentInfo);
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));

    assertTrue(event.getDeploymentSummaries().get(0).getDeploymentInfo() instanceof AwsCodeDeployDeploymentInfo);
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertEquals(CODE_DEPLOY_key,
        ((AwsCodeDeployDeploymentKey) event.getDeploymentSummaries().get(0).getAwsCodeDeployDeploymentKey()).getKey());
  }

  @Test
  public void testExtractInstanceOrContainerInfoBaseOnType_For_ECS() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData = instaceHelperTestHelper.initExecutionSummary(InfrastructureMappingType.AWS_ECS,
        Constants.DEPLOY_CONTAINERS, endsAtTime, DeploymentType.ECS.getDisplayName());
    phaseStepExecutionData = getPhaseStepExecutionData(phaseExecutionData);
    doReturn(EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping()
                 .withUuid(INFRA_MAP_ID)
                 .withInfraMappingType(InfrastructureMappingType.AWS_ECS.getName())
                 .withAppId(APP_ID)
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());

    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow("Name");
    phaseStepSubWorkflow.setRollback(false);
    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, APP_ID, workflowExecution, phaseStepSubWorkflow, null);

    // Capture the argument of the doSomething function
    ArgumentCaptor<DeploymentEvent> captor = ArgumentCaptor.forClass(DeploymentEvent.class);
    verify(deploymentEventQueue).send(captor.capture());

    /*
     * The way we test here is,
     * We have mocked deploymentQueueEvent.send(DeploymentEvent) to doNothing as we dont want to process queue.
     * We capture the argument i.e. deploymentEvent that is generated and sent to deploymentEventQueue, and
     * validate to check its valid
     * */
    DeploymentEvent event = captor.getValue();
    assertNotNull(event);
    assertEquals(0, event.getRetries());

    DeploymentInfo deploymentInfo1 = event.getDeploymentSummaries().get(0).getDeploymentInfo();
    DeploymentInfo deploymentInfo2 = event.getDeploymentSummaries().get(1).getDeploymentInfo();
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(1));
    assertTrue(deploymentInfo1 instanceof ContainerDeploymentInfoWithNames);
    assertTrue(deploymentInfo2 instanceof ContainerDeploymentInfoWithNames);

    assertEquals(CLUSTER_NAME,
        ((ContainerDeploymentInfoWithNames) event.getDeploymentSummaries().get(0).getDeploymentInfo())
            .getClusterName());
    assertEquals(CLUSTER_NAME,
        ((ContainerDeploymentInfoWithNames) event.getDeploymentSummaries().get(1).getDeploymentInfo())
            .getClusterName());

    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("ecsNew");
    serviceNames.add("ecsOld");

    assertTrue(serviceNames.contains(
        ((ContainerDeploymentKey) event.getDeploymentSummaries().get(0).getContainerDeploymentKey())
            .getContainerServiceName()));
    assertTrue(serviceNames.contains(
        ((ContainerDeploymentKey) event.getDeploymentSummaries().get(1).getContainerDeploymentKey())
            .getContainerServiceName()));
  }

  @Test
  public void testExtractInstanceOrContainerInfoBaseOnType_For_Kubernetes() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData =
        instaceHelperTestHelper.initKubernetesExecutionSummary(InfrastructureMappingType.GCP_KUBERNETES,
            Constants.DEPLOY_CONTAINERS, endsAtTime, DeploymentType.KUBERNETES.getDisplayName(), false);
    phaseStepExecutionData = getPhaseStepExecutionData(phaseExecutionData);
    doReturn(GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping()
                 .withUuid(INFRA_MAP_ID)
                 .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
                 .withAppId(APP_ID)
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());

    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow("Name");
    phaseStepSubWorkflow.setRollback(false);
    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, APP_ID, workflowExecution, phaseStepSubWorkflow, null);

    // Capture the argument of the doSomething function
    ArgumentCaptor<DeploymentEvent> captor = ArgumentCaptor.forClass(DeploymentEvent.class);
    verify(deploymentEventQueue).send(captor.capture());

    /*
     * The way we test here is,
     * We have mocked deploymentQueueEvent.send(DeploymentEvent) to doNothing as we dont want to process queue.
     * We capture the argument i.e. deploymentEvent that is generated and sent to deploymentEventQueue, and
     * validate to check its valid
     * */
    DeploymentEvent event = captor.getValue();
    assertNotNull(event);
    assertEquals(0, event.getRetries());

    DeploymentInfo deploymentInfo1 = event.getDeploymentSummaries().get(0).getDeploymentInfo();
    DeploymentInfo deploymentInfo2 = event.getDeploymentSummaries().get(1).getDeploymentInfo();
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(1));
    assertTrue(deploymentInfo1 instanceof ContainerDeploymentInfoWithNames);
    assertTrue(deploymentInfo2 instanceof ContainerDeploymentInfoWithNames);

    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(1));

    assertEquals(CLUSTER_NAME,
        ((ContainerDeploymentInfoWithNames) event.getDeploymentSummaries().get(0).getDeploymentInfo())
            .getClusterName());
    assertEquals(CLUSTER_NAME,
        ((ContainerDeploymentInfoWithNames) event.getDeploymentSummaries().get(1).getDeploymentInfo())
            .getClusterName());

    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("kubernetesNew");
    serviceNames.add("kubernetesOld");

    assertTrue(serviceNames.contains(
        ((ContainerDeploymentKey) event.getDeploymentSummaries().get(0).getContainerDeploymentKey())
            .getContainerServiceName()));
    assertTrue(serviceNames.contains(
        ((ContainerDeploymentKey) event.getDeploymentSummaries().get(1).getContainerDeploymentKey())
            .getContainerServiceName()));
  }

  @Test
  public void testExtractInstanceOrContainerInfoBaseOnType_For_Helm_Kubernetes() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData =
        instaceHelperTestHelper.initKubernetesExecutionSummary(InfrastructureMappingType.GCP_KUBERNETES,
            Constants.DEPLOY_CONTAINERS, endsAtTime, DeploymentType.KUBERNETES.getDisplayName(), true);
    phaseStepExecutionData = getPhaseStepExecutionData(phaseExecutionData);
    doReturn(GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping()
                 .withUuid(INFRA_MAP_ID)
                 .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
                 .withAppId(APP_ID)
                 .withClusterName(CLUSTER_NAME)
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());

    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow("Name");
    phaseStepSubWorkflow.setRollback(false);
    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, APP_ID, workflowExecution, phaseStepSubWorkflow, null);

    // Capture the argument of the doSomething function
    ArgumentCaptor<DeploymentEvent> captor = ArgumentCaptor.forClass(DeploymentEvent.class);
    verify(deploymentEventQueue).send(captor.capture());

    /*
     * The way we test here is,
     * We have mocked deploymentQueueEvent.send(DeploymentEvent) to doNothing as we dont want to process queue.
     * We capture the argument i.e. deploymentEvent that is generated and sent to deploymentEventQueue, and
     * validate to check its valid
     * */
    DeploymentEvent event = captor.getValue();
    assertNotNull(event);
    assertEquals(0, event.getRetries());

    assertNotNull(event.getDeploymentSummaries());
    assertEquals(1, event.getDeploymentSummaries().size());

    DeploymentSummary deploymentSummary = event.getDeploymentSummaries().get(0);
    DeploymentInfo deploymentInfo = event.getDeploymentSummaries().get(0).getDeploymentInfo();

    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertTrue(deploymentInfo instanceof ContainerDeploymentInfoWithLabels);
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));

    ContainerDeploymentInfoWithLabels containerDeploymentInfoWithLabels =
        (ContainerDeploymentInfoWithLabels) deploymentInfo;
    assertEquals(CLUSTER_NAME, containerDeploymentInfoWithLabels.getClusterName());
    assertNotNull(containerDeploymentInfoWithLabels.getLabels());
    assertEquals(1, containerDeploymentInfoWithLabels.getLabels().size());
    assertEquals("release", containerDeploymentInfoWithLabels.getLabels().get(0).getName());
    assertEquals("version1", containerDeploymentInfoWithLabels.getLabels().get(0).getValue());

    assertNotNull(deploymentSummary.getContainerDeploymentKey().getLabels());
    assertEquals("release", deploymentSummary.getContainerDeploymentKey().getLabels().get(0).getName());
    assertEquals("version1", deploymentSummary.getContainerDeploymentKey().getLabels().get(0).getValue());

    assertEquals("release", containerDeploymentInfoWithLabels.getLabels().get(0).getName());
    assertEquals("version1", containerDeploymentInfoWithLabels.getLabels().get(0).getValue());
  }

  @Test
  public void testExtractInstanceOrContainerInfoBaseOnType_For_Helm_Kubernetes_rollback() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData =
        instaceHelperTestHelper.initKubernetesExecutionSummary(InfrastructureMappingType.GCP_KUBERNETES,
            Constants.DEPLOY_CONTAINERS, endsAtTime, DeploymentType.KUBERNETES.getDisplayName(), true);
    phaseStepExecutionData = getPhaseStepExecutionData(phaseExecutionData);
    doReturn(GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping()
                 .withUuid(INFRA_MAP_ID)
                 .withInfraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
                 .withAppId(APP_ID)
                 .withClusterName(CLUSTER_NAME)
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());

    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow("Name");
    phaseStepSubWorkflow.setRollback(true);
    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, APP_ID, workflowExecution, phaseStepSubWorkflow, null);

    // Capture the argument of the doSomething function
    ArgumentCaptor<DeploymentEvent> captor = ArgumentCaptor.forClass(DeploymentEvent.class);
    verify(deploymentEventQueue).send(captor.capture());

    /*
     * The way we test here is,
     * We have mocked deploymentQueueEvent.send(DeploymentEvent) to doNothing as we dont want to process queue.
     * We capture the argument i.e. deploymentEvent that is generated and sent to deploymentEventQueue, and
     * validate to check its valid
     * */
    DeploymentEvent event = captor.getValue();
    assertNotNull(event);
    assertEquals(0, event.getRetries());
    assertTrue(event.isRollback());

    assertNotNull(event.getDeploymentSummaries());
    assertEquals(1, event.getDeploymentSummaries().size());

    DeploymentSummary deploymentSummary = event.getDeploymentSummaries().get(0);
    assertNotNull(deploymentSummary.getContainerDeploymentKey());
    // This is validating rollback version
    assertEquals("0", deploymentSummary.getContainerDeploymentKey().getNewVersion());
    DeploymentInfo deploymentInfo = event.getDeploymentSummaries().get(0).getDeploymentInfo();

    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertTrue(deploymentInfo instanceof ContainerDeploymentInfoWithLabels);
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));

    ContainerDeploymentInfoWithLabels containerDeploymentInfoWithLabels =
        (ContainerDeploymentInfoWithLabels) deploymentInfo;
    assertEquals(CLUSTER_NAME, containerDeploymentInfoWithLabels.getClusterName());
    assertNotNull(containerDeploymentInfoWithLabels.getLabels());
    assertEquals(1, containerDeploymentInfoWithLabels.getLabels().size());
    assertEquals("release", containerDeploymentInfoWithLabels.getLabels().get(0).getName());
    assertEquals("version1", containerDeploymentInfoWithLabels.getLabels().get(0).getValue());

    assertNotNull(deploymentSummary.getContainerDeploymentKey().getLabels());
    assertEquals("release", deploymentSummary.getContainerDeploymentKey().getLabels().get(0).getName());
    assertEquals("version1", deploymentSummary.getContainerDeploymentKey().getLabels().get(0).getValue());

    assertEquals("release", containerDeploymentInfoWithLabels.getLabels().get(0).getName());
    assertEquals("version1", containerDeploymentInfoWithLabels.getLabels().get(0).getValue());
  }

  @Test
  public void testIsSupported() throws Exception {
    assertFalse((Boolean) MethodUtils.invokeMethod(
        instanceHelper, true, "isSupported", new Object[] {InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH}));
    assertFalse((Boolean) MethodUtils.invokeMethod(
        instanceHelper, true, "isSupported", new Object[] {InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM}));
    assertFalse((Boolean) MethodUtils.invokeMethod(
        instanceHelper, true, "isSupported", new Object[] {InfrastructureMappingType.AWS_AWS_LAMBDA}));
    assertTrue((Boolean) MethodUtils.invokeMethod(
        instanceHelper, true, "isSupported", new Object[] {InfrastructureMappingType.AWS_ECS}));
    assertTrue((Boolean) MethodUtils.invokeMethod(
        instanceHelper, true, "isSupported", new Object[] {InfrastructureMappingType.AWS_AMI}));
    assertTrue((Boolean) MethodUtils.invokeMethod(
        instanceHelper, true, "isSupported", new Object[] {InfrastructureMappingType.AWS_AWS_CODEDEPLOY}));
    assertTrue((Boolean) MethodUtils.invokeMethod(
        instanceHelper, true, "isSupported", new Object[] {InfrastructureMappingType.GCP_KUBERNETES}));
    assertTrue((Boolean) MethodUtils.invokeMethod(
        instanceHelper, true, "isSupported", new Object[] {InfrastructureMappingType.AWS_SSH}));
  }

  @Test
  public void testGetPrivateDnsName() throws Exception {
    String privateDnsName = "ip-172-31-11-6.ec2.internal";

    // privateDnsName is nonNull and contains .
    String name =
        (String) MethodUtils.invokeMethod(instanceHelper, true, "getPrivateDnsName", new Object[] {privateDnsName});
    assertEquals("ip-172-31-11-6", name);

    // privateDnsName is nonNull and does not contains . (not sure if this can happen, but good to handle)
    privateDnsName = "ip-172-31-11-6ec2_internal";
    name = (String) MethodUtils.invokeMethod(instanceHelper, true, "getPrivateDnsName", new Object[] {privateDnsName});
    assertEquals(privateDnsName, name);

    // privateDnsName is nonNull and empty
    privateDnsName = "";
    name = (String) MethodUtils.invokeMethod(instanceHelper, true, "getPrivateDnsName", new Object[] {privateDnsName});
    assertEquals(StringUtils.EMPTY, name);

    // privateDnsName is nonNull and contains spaces
    privateDnsName = "  ";
    name = (String) MethodUtils.invokeMethod(instanceHelper, true, "getPrivateDnsName", new Object[] {privateDnsName});
    assertEquals(StringUtils.EMPTY, name);

    // privateDnsName is null
    privateDnsName = null;
    name = (String) MethodUtils.invokeMethod(instanceHelper, true, "getPrivateDnsName", new Object[] {privateDnsName});
    assertEquals(StringUtils.EMPTY, name);
  }

  private void assertDeploymentSummaryObject(DeploymentSummary deploymentSummary) {
    assertEquals(APP_ID, deploymentSummary.getAppId());
    assertEquals(ACCOUNT_ID, deploymentSummary.getAccountId());
    assertEquals(INFRA_MAP_ID, deploymentSummary.getInfraMappingId());
    assertEquals(WORKFLOW_EXECUTION_ID, deploymentSummary.getWorkflowExecutionId());
    assertEquals(STATE_EXECUTION_INSTANCE_ID, deploymentSummary.getStateExecutionInstanceId());
  }
}
