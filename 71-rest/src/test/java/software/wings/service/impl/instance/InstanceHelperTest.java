package software.wings.service.impl.instance;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.queue.Queue;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.stubbing.Answer;
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
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.Ec2InstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.beans.infrastructure.instance.key.deployment.AwsAmiDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.AwsCodeDeployDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.infrastructure.instance.key.deployment.DeploymentKey;
import software.wings.service.impl.instance.sync.ContainerSync;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
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
import java.util.concurrent.atomic.AtomicInteger;

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
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private Queue<DeploymentEvent> deploymentEventQueue;
  @Mock private ExecutionContext context;
  @Mock private ContainerSync containerSync;
  @Mock private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @InjectMocks @Spy WorkflowStandardParams workflowStandardParams;

  @InjectMocks @Inject private AwsCodeDeployInstanceHandler awsCodeDeployInstanceHandler;
  @InjectMocks @Inject private AwsAmiInstanceHandler awsAmiInstanceHandler;
  @InjectMocks @Inject private AwsInstanceHandler awsInstanceHandler;
  @InjectMocks @Inject private ContainerInstanceHandler containerInstanceHandler;
  @InjectMocks @Inject private PcfInstanceHandler pcfInstanceHandler;
  @InjectMocks @Inject private AzureInstanceHandler azureInstanceHandler;
  @InjectMocks @Inject private SpotinstAmiInstanceHandler spotinstAmiInstanceHandler;

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
    workflowExecution = WorkflowExecution.builder()
                            .appId("app_1")
                            .appName("app1")
                            .uuid(WORKFLOW_EXECUTION_ID)
                            .name("workflow1")
                            .pipelineSummary(null)
                            .envName("envName")
                            .envId("env_1")
                            .envType(EnvironmentType.PROD)
                            .triggeredBy(EmbeddedUser.builder().name("user1").uuid("user_1").build())
                            .build();

    when(instanceService.saveOrUpdate(anyList())).thenAnswer(i -> i.getArguments()[0]);

    doReturn(Application.Builder.anApplication().name("app").uuid("app_1").accountId(ACCOUNT_ID).build())
        .when(appService)
        .get(anyString());

    doNothing().when(deploymentEventQueue).send(any());

    // This mocking will be used for
    // workflowStandardParams.getArtifactForService(phaseExecutionData.getManifestByServiceId())
    final String ARTIFACT_ID_1 = "artifact_1";
    final String ARTIFACT_ID_2 = "artifact_2";
    final String ARTIFACT_STREAM_ID_1 = "artifactStream_1";
    final String ARTIFACT_STREAM_ID_2 = "artifactStream_2";
    final String SERVICE_ID_2 = "service_2";
    workflowStandardParams.setAppId(APP_ID);
    workflowStandardParams.setArtifactIds(asList(ARTIFACT_ID_1, ARTIFACT_ID_2));
    when(artifactService.get(anyString())).thenAnswer(invocation -> {
      if (invocation.getArgumentAt(0, String.class).equals(ARTIFACT_ID_1)) {
        return Artifact.Builder.anArtifact()
            .withUuid(ARTIFACT_ID_1)
            .withDisplayName("artifact1")
            .withArtifactStreamId(ARTIFACT_STREAM_ID_1)
            .withArtifactSourceName("sourceName")
            .withMetadata(Collections.singletonMap("buildNo", "1.0"))
            .build();
      } else {
        return Artifact.Builder.anArtifact()
            .withUuid(ARTIFACT_ID_2)
            .withDisplayName("artifact2")
            .withArtifactStreamId(ARTIFACT_STREAM_ID_2)
            .withArtifactSourceName("sourceName")
            .withMetadata(Collections.singletonMap("buildNo", "1.0"))
            .build();
      }
    });
    when(serviceResourceService.get(SERVICE_ID))
        .thenReturn(Service.builder().artifactStreamIds(Collections.singletonList(ARTIFACT_STREAM_ID_1)).build());
    when(serviceResourceService.get(SERVICE_ID_2))
        .thenReturn(Service.builder().artifactStreamIds(Collections.singletonList(ARTIFACT_STREAM_ID_2)).build());
    when(artifactStreamServiceBindingService.listArtifactStreamIds(SERVICE_ID))
        .thenReturn(Collections.singletonList(ARTIFACT_STREAM_ID_1));
    when(artifactStreamServiceBindingService.listArtifactStreamIds(SERVICE_ID_2))
        .thenReturn(Collections.singletonList(ARTIFACT_STREAM_ID_2));
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
    doReturn(names).when(containerSync).getControllerNames(any(), any(), any());
  }

  @Test
  @Category(UnitTests.class)
  public void testExtractInstanceOrContainerInfoBaseOnType_PDS() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData = instaceHelperTestHelper.initExecutionSummary(
        InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH, WorkflowServiceHelper.DEPLOY_SERVICE, endsAtTime, "");
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
    doReturn(INFRA_MAP_ID).when(context).fetchInfraMappingId();

    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, APP_ID, workflowExecution, phaseStepSubWorkflow, context);

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
  @Category(UnitTests.class)
  public void testExtractInstanceOrContainerInfoBaseOnType_For_AWS_SSH_AmiInfraMapping() {
    endsAtTime = System.currentTimeMillis();
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData = instaceHelperTestHelper.initExecutionSummary(
        InfrastructureMappingType.AWS_SSH, WorkflowServiceHelper.DEPLOY_SERVICE, endsAtTime, DeploymentType.SSH.name());
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
    doReturn(INFRA_MAP_ID).when(context).fetchInfraMappingId();

    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, APP_ID, workflowExecution, phaseStepSubWorkflow, context);

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
  @Category(UnitTests.class)
  public void testExtractInstanceOrContainerInfoBaseOnType_For_AWS_SSH_CodeDeployInfraMapping() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData = instaceHelperTestHelper.initExecutionSummary(
        InfrastructureMappingType.AWS_SSH, WorkflowServiceHelper.DEPLOY_SERVICE, endsAtTime, DeploymentType.SSH.name());
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
    doReturn(INFRA_MAP_ID).when(context).fetchInfraMappingId();

    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, "app_1", workflowExecution, phaseStepSubWorkflow, context);

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
  @Category(UnitTests.class)
  public void testExtractInstanceOrContainerInfoBaseOnType_For_AMI() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData = instaceHelperTestHelper.initExecutionSummary(
        InfrastructureMappingType.AWS_AMI, WorkflowServiceHelper.DEPLOY_SERVICE, endsAtTime, DeploymentType.AMI.name());
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
    doReturn(INFRA_MAP_ID).when(context).fetchInfraMappingId();

    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, "app_1", workflowExecution, phaseStepSubWorkflow, context);

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
    assertThat(event).isNotNull();
    assertThat(event.getRetries()).isEqualTo(0);
    assertThat(event).isNotNull();
    assertThat(event.getDeploymentSummaries()).isNotNull();
    assertThat(event.getDeploymentSummaries()).hasSize(2);
    DeploymentInfo deploymentInfo1 = event.getDeploymentSummaries().get(0).getDeploymentInfo();
    DeploymentInfo deploymentInfo2 = event.getDeploymentSummaries().get(1).getDeploymentInfo();
    assertThat(deploymentInfo1 instanceof AwsAutoScalingGroupDeploymentInfo).isTrue();
    assertThat(deploymentInfo2 instanceof AwsAutoScalingGroupDeploymentInfo).isTrue();
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(1));
    assertThat(asList("asgNew", "asgOld")
                   .contains(((AwsAmiDeploymentKey) event.getDeploymentSummaries().get(0).getAwsAmiDeploymentKey())
                                 .getAutoScalingGroupName()))
        .isTrue();
    assertThat(asList("asgNew", "asgOld")
                   .contains(((AwsAmiDeploymentKey) event.getDeploymentSummaries().get(1).getAwsAmiDeploymentKey())
                                 .getAutoScalingGroupName()))
        .isTrue();
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
  @Category(UnitTests.class)
  public void testExtractInstanceOrContainerInfoBaseOnType_For_CodeDeploy() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData = instaceHelperTestHelper.initExecutionSummary(InfrastructureMappingType.AWS_AWS_CODEDEPLOY,
        WorkflowServiceHelper.DEPLOY_SERVICE, endsAtTime, DeploymentType.AWS_CODEDEPLOY.getDisplayName());

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
    doReturn(INFRA_MAP_ID).when(context).fetchInfraMappingId();

    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, "app_1", workflowExecution, phaseStepSubWorkflow, context);

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
    assertThat(event).isNotNull();
    assertThat(event.getRetries()).isEqualTo(0);

    assertThat(event).isNotNull();
    assertThat(event.getDeploymentSummaries()).isNotNull();
    assertThat(event.getDeploymentSummaries()).hasSize(1);
    DeploymentInfo deploymentInfo = event.getDeploymentSummaries().get(0).getDeploymentInfo();
    assertThat(deploymentInfo instanceof AwsCodeDeployDeploymentInfo).isTrue();
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));

    assertThat(event.getDeploymentSummaries().get(0).getDeploymentInfo() instanceof AwsCodeDeployDeploymentInfo)
        .isTrue();
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertThat(
        ((AwsCodeDeployDeploymentKey) event.getDeploymentSummaries().get(0).getAwsCodeDeployDeploymentKey()).getKey())
        .isEqualTo(CODE_DEPLOY_key);
  }

  @Test
  @Category(UnitTests.class)
  public void testExtractInstanceOrContainerInfoBaseOnType_For_ECS() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData = instaceHelperTestHelper.initExecutionSummary(InfrastructureMappingType.AWS_ECS,
        WorkflowServiceHelper.DEPLOY_CONTAINERS, endsAtTime, DeploymentType.ECS.getDisplayName());
    phaseStepExecutionData = getPhaseStepExecutionData(phaseExecutionData);
    doReturn(EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping()
                 .withUuid(INFRA_MAP_ID)
                 .withInfraMappingType(InfrastructureMappingType.AWS_ECS.getName())
                 .withAppId(APP_ID)
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());
    doReturn(INFRA_MAP_ID).when(context).fetchInfraMappingId();

    PhaseStepSubWorkflow phaseStepSubWorkflow = new PhaseStepSubWorkflow("Name");
    phaseStepSubWorkflow.setRollback(false);
    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, APP_ID, workflowExecution, phaseStepSubWorkflow, context);

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
    assertThat(event).isNotNull();
    assertThat(event.getRetries()).isEqualTo(0);

    DeploymentInfo deploymentInfo1 = event.getDeploymentSummaries().get(0).getDeploymentInfo();
    DeploymentInfo deploymentInfo2 = event.getDeploymentSummaries().get(1).getDeploymentInfo();
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(1));
    assertThat(deploymentInfo1 instanceof ContainerDeploymentInfoWithNames).isTrue();
    assertThat(deploymentInfo2 instanceof ContainerDeploymentInfoWithNames).isTrue();

    assertThat(
        ((ContainerDeploymentInfoWithNames) event.getDeploymentSummaries().get(0).getDeploymentInfo()).getClusterName())
        .isEqualTo(CLUSTER_NAME);
    assertThat(
        ((ContainerDeploymentInfoWithNames) event.getDeploymentSummaries().get(1).getDeploymentInfo()).getClusterName())
        .isEqualTo(CLUSTER_NAME);

    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("ecsNew");
    serviceNames.add("ecsOld");

    assertThat(serviceNames.contains(
                   ((ContainerDeploymentKey) event.getDeploymentSummaries().get(0).getContainerDeploymentKey())
                       .getContainerServiceName()))
        .isTrue();
    assertThat(serviceNames.contains(
                   ((ContainerDeploymentKey) event.getDeploymentSummaries().get(1).getContainerDeploymentKey())
                       .getContainerServiceName()))
        .isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testExtractInstanceOrContainerInfoBaseOnType_For_Kubernetes() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData =
        instaceHelperTestHelper.initKubernetesExecutionSummary(InfrastructureMappingType.GCP_KUBERNETES,
            WorkflowServiceHelper.DEPLOY_CONTAINERS, endsAtTime, DeploymentType.KUBERNETES.getDisplayName(), false);
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
    doReturn(INFRA_MAP_ID).when(context).fetchInfraMappingId();

    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, APP_ID, workflowExecution, phaseStepSubWorkflow, context);

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
    assertThat(event).isNotNull();
    assertThat(event.getRetries()).isEqualTo(0);

    DeploymentInfo deploymentInfo1 = event.getDeploymentSummaries().get(0).getDeploymentInfo();
    DeploymentInfo deploymentInfo2 = event.getDeploymentSummaries().get(1).getDeploymentInfo();
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(1));
    assertThat(deploymentInfo1 instanceof ContainerDeploymentInfoWithNames).isTrue();
    assertThat(deploymentInfo2 instanceof ContainerDeploymentInfoWithNames).isTrue();

    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(1));

    assertThat(
        ((ContainerDeploymentInfoWithNames) event.getDeploymentSummaries().get(0).getDeploymentInfo()).getClusterName())
        .isEqualTo(CLUSTER_NAME);
    assertThat(
        ((ContainerDeploymentInfoWithNames) event.getDeploymentSummaries().get(1).getDeploymentInfo()).getClusterName())
        .isEqualTo(CLUSTER_NAME);

    Set<String> serviceNames = new HashSet<>();
    serviceNames.add("kubernetesNew");
    serviceNames.add("kubernetesOld");

    assertThat(serviceNames.contains(
                   ((ContainerDeploymentKey) event.getDeploymentSummaries().get(0).getContainerDeploymentKey())
                       .getContainerServiceName()))
        .isTrue();
    assertThat(serviceNames.contains(
                   ((ContainerDeploymentKey) event.getDeploymentSummaries().get(1).getContainerDeploymentKey())
                       .getContainerServiceName()))
        .isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testExtractInstanceOrContainerInfoBaseOnType_For_Helm_Kubernetes() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData =
        instaceHelperTestHelper.initKubernetesExecutionSummary(InfrastructureMappingType.GCP_KUBERNETES,
            WorkflowServiceHelper.DEPLOY_CONTAINERS, endsAtTime, DeploymentType.KUBERNETES.getDisplayName(), true);
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
    doReturn(INFRA_MAP_ID).when(context).fetchInfraMappingId();

    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, APP_ID, workflowExecution, phaseStepSubWorkflow, context);

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
    assertThat(event).isNotNull();
    assertThat(event.getRetries()).isEqualTo(0);

    assertThat(event.getDeploymentSummaries()).isNotNull();
    assertThat(event.getDeploymentSummaries()).hasSize(1);

    DeploymentSummary deploymentSummary = event.getDeploymentSummaries().get(0);
    DeploymentInfo deploymentInfo = event.getDeploymentSummaries().get(0).getDeploymentInfo();

    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertThat(deploymentInfo instanceof ContainerDeploymentInfoWithLabels).isTrue();
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));

    ContainerDeploymentInfoWithLabels containerDeploymentInfoWithLabels =
        (ContainerDeploymentInfoWithLabels) deploymentInfo;
    assertThat(containerDeploymentInfoWithLabels.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(containerDeploymentInfoWithLabels.getLabels()).isNotNull();
    assertThat(containerDeploymentInfoWithLabels.getLabels()).hasSize(1);
    assertThat(containerDeploymentInfoWithLabels.getLabels().get(0).getName()).isEqualTo("release");
    assertThat(containerDeploymentInfoWithLabels.getLabels().get(0).getValue()).isEqualTo("version1");

    assertThat(deploymentSummary.getContainerDeploymentKey().getLabels()).isNotNull();
    assertThat(deploymentSummary.getContainerDeploymentKey().getLabels().get(0).getName()).isEqualTo("release");
    assertThat(deploymentSummary.getContainerDeploymentKey().getLabels().get(0).getValue()).isEqualTo("version1");

    assertThat(containerDeploymentInfoWithLabels.getLabels().get(0).getName()).isEqualTo("release");
    assertThat(containerDeploymentInfoWithLabels.getLabels().get(0).getValue()).isEqualTo("version1");
  }

  @Test
  @Category(UnitTests.class)
  public void testExtractInstanceOrContainerInfoBaseOnType_For_Helm_Kubernetes_rollback() {
    endsAtTime = System.currentTimeMillis();
    phaseExecutionData =
        instaceHelperTestHelper.initKubernetesExecutionSummary(InfrastructureMappingType.GCP_KUBERNETES,
            WorkflowServiceHelper.DEPLOY_CONTAINERS, endsAtTime, DeploymentType.KUBERNETES.getDisplayName(), true);
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
    doReturn(INFRA_MAP_ID).when(context).fetchInfraMappingId();

    instanceHelper.extractInstanceOrDeploymentInfoBaseOnType(STATE_EXECUTION_INSTANCE_ID, phaseExecutionData,
        phaseStepExecutionData, workflowStandardParams, APP_ID, workflowExecution, phaseStepSubWorkflow, context);

    // Capture the argument of the doSomething function
    ArgumentCaptor<DeploymentEvent> captor = ArgumentCaptor.forClass(DeploymentEvent.class);
    verify(deploymentEventQueue).send(captor.capture());

    /*
     * The way we test here is,
     * We have mocked deploymentQueueEvent.send(DeploymentEvent) to doNothing as we dont want to process queue.
     * We capture the argument i.e. deploymentEvent that is generated and sent to deploymentEventQueue, and
     * validate to check its valid
     */
    DeploymentEvent event = captor.getValue();
    assertThat(event).isNotNull();
    assertThat(event.getRetries()).isEqualTo(0);
    assertThat(event.isRollback()).isTrue();

    assertThat(event.getDeploymentSummaries()).isNotNull();
    assertThat(event.getDeploymentSummaries()).hasSize(1);

    DeploymentSummary deploymentSummary = event.getDeploymentSummaries().get(0);
    assertThat(deploymentSummary.getContainerDeploymentKey()).isNotNull();
    // This is validating rollback version
    assertThat(deploymentSummary.getContainerDeploymentKey().getNewVersion()).isEqualTo("0");
    DeploymentInfo deploymentInfo = event.getDeploymentSummaries().get(0).getDeploymentInfo();

    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));
    assertThat(deploymentInfo instanceof ContainerDeploymentInfoWithLabels).isTrue();
    assertDeploymentSummaryObject(event.getDeploymentSummaries().get(0));

    ContainerDeploymentInfoWithLabels containerDeploymentInfoWithLabels =
        (ContainerDeploymentInfoWithLabels) deploymentInfo;
    assertThat(containerDeploymentInfoWithLabels.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(containerDeploymentInfoWithLabels.getLabels()).isNotNull();
    assertThat(containerDeploymentInfoWithLabels.getLabels()).hasSize(1);
    assertThat(containerDeploymentInfoWithLabels.getLabels().get(0).getName()).isEqualTo("release");
    assertThat(containerDeploymentInfoWithLabels.getLabels().get(0).getValue()).isEqualTo("version1");

    assertThat(deploymentSummary.getContainerDeploymentKey().getLabels()).isNotNull();
    assertThat(deploymentSummary.getContainerDeploymentKey().getLabels().get(0).getName()).isEqualTo("release");
    assertThat(deploymentSummary.getContainerDeploymentKey().getLabels().get(0).getValue()).isEqualTo("version1");

    assertThat(containerDeploymentInfoWithLabels.getLabels().get(0).getName()).isEqualTo("release");
    assertThat(containerDeploymentInfoWithLabels.getLabels().get(0).getValue()).isEqualTo("version1");
  }

  @Test
  @Category(UnitTests.class)
  public void testIsSupported() throws Exception {
    assertThat(instanceHelper.isSupported(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH)).isFalse();
    assertThat(instanceHelper.isSupported(InfrastructureMappingType.PHYSICAL_DATA_CENTER_WINRM)).isFalse();
    assertThat(instanceHelper.isSupported(InfrastructureMappingType.AWS_AWS_LAMBDA)).isFalse();
    assertThat(instanceHelper.isSupported(InfrastructureMappingType.AWS_ECS)).isTrue();
    assertThat(instanceHelper.isSupported(InfrastructureMappingType.AWS_AMI)).isTrue();
    assertThat(instanceHelper.isSupported(InfrastructureMappingType.AWS_AWS_CODEDEPLOY)).isTrue();
    assertThat(instanceHelper.isSupported(InfrastructureMappingType.GCP_KUBERNETES)).isTrue();
    assertThat(instanceHelper.isSupported(InfrastructureMappingType.AWS_SSH)).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void testGetPrivateDnsName() throws Exception {
    String privateDnsName = "ip-172-31-11-6.ec2.internal";

    // privateDnsName is nonNull and contains .
    String name = instanceHelper.getPrivateDnsName(privateDnsName);
    assertThat(name).isEqualTo("ip-172-31-11-6");

    // privateDnsName is nonNull and does not contains . (not sure if this can happen, but good to handle)
    privateDnsName = "ip-172-31-11-6ec2_internal";
    name = instanceHelper.getPrivateDnsName(privateDnsName);
    assertThat(name).isEqualTo(privateDnsName);

    // privateDnsName is nonNull and empty
    privateDnsName = "";
    name = instanceHelper.getPrivateDnsName(privateDnsName);
    assertThat(name).isEqualTo(StringUtils.EMPTY);

    // privateDnsName is nonNull and contains spaces
    privateDnsName = "  ";
    name = instanceHelper.getPrivateDnsName(privateDnsName);
    assertThat(name).isEqualTo(StringUtils.EMPTY);

    // privateDnsName is null
    privateDnsName = null;
    name = instanceHelper.getPrivateDnsName(privateDnsName);
    assertThat(name).isEqualTo(StringUtils.EMPTY);
  }

  private void assertDeploymentSummaryObject(DeploymentSummary deploymentSummary) {
    assertThat(deploymentSummary.getAppId()).isEqualTo(APP_ID);
    assertThat(deploymentSummary.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(deploymentSummary.getInfraMappingId()).isEqualTo(INFRA_MAP_ID);
    assertThat(deploymentSummary.getWorkflowExecutionId()).isEqualTo(WORKFLOW_EXECUTION_ID);
    assertThat(deploymentSummary.getStateExecutionInstanceId()).isEqualTo(STATE_EXECUTION_INSTANCE_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void testManualSyncSuccess() throws Exception {
    InstanceHandlerFactory instanceHandlerFactory =
        spy(new InstanceHandlerFactory(containerInstanceHandler, awsInstanceHandler, awsAmiInstanceHandler,
            awsCodeDeployInstanceHandler, pcfInstanceHandler, azureInstanceHandler, spotinstAmiInstanceHandler));
    FieldUtils.writeField(instanceHelper, "instanceHandlerFactory", instanceHandlerFactory, true);

    doReturn(new InstanceHandler() {
      @Override
      public void syncInstances(String appId, String infraMappingId) throws WingsException {}

      @Override
      public void handleNewDeployment(List<DeploymentSummary> deploymentSummaries, boolean rollback)
          throws WingsException {}

      @Override
      public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
          PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
          InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact)
          throws WingsException {
        return null;
      }

      @Override
      public DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo) {
        return null;
      }

      @Override
      protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {}
    })
        .when(instanceHandlerFactory)
        .getInstanceHandler(any(InfrastructureMapping.class));

    doReturn(PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
                 .withUuid(INFRA_MAP_ID)
                 .withInfraMappingType("PHYSICAL_DATA_CENTER_SSH")
                 .withAppId(APP_ID)
                 .withServiceId(SERVICE_ID)
                 .withEnvId(ENV_ID)
                 .withInfraMappingType(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name())
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());
    final AtomicInteger count = new AtomicInteger(0);
    doAnswer((Answer<Void>) invocationOnMock -> {
      count.incrementAndGet();
      return null;
    })
        .when(instanceService)
        .updateSyncSuccess(anyString(), anyString(), anyString(), anyString(), anyString(), anyLong());
    instanceHelper.manualSync(APP_ID, INFRA_MAP_ID);
    assertThat(count.get()).isEqualTo(1);
  }

  @Test
  @Category(UnitTests.class)
  public void testManualSyncFailure() throws Exception {
    InstanceHandlerFactory instanceHandlerFactory =
        spy(new InstanceHandlerFactory(containerInstanceHandler, awsInstanceHandler, awsAmiInstanceHandler,
            awsCodeDeployInstanceHandler, pcfInstanceHandler, azureInstanceHandler, spotinstAmiInstanceHandler));
    FieldUtils.writeField(instanceHelper, "instanceHandlerFactory", instanceHandlerFactory, true);

    doReturn(new InstanceHandler() {
      @Override
      public void syncInstances(String appId, String infraMappingId) throws WingsException {
        throw WingsException.builder().message("cannot connect").build();
      }

      @Override
      public void handleNewDeployment(List<DeploymentSummary> deploymentSummaries, boolean rollback)
          throws WingsException {}

      @Override
      public Optional<List<DeploymentInfo>> getDeploymentInfo(PhaseExecutionData phaseExecutionData,
          PhaseStepExecutionData phaseStepExecutionData, WorkflowExecution workflowExecution,
          InfrastructureMapping infrastructureMapping, String stateExecutionInstanceId, Artifact artifact)
          throws WingsException {
        return null;
      }

      @Override
      public DeploymentKey generateDeploymentKey(DeploymentInfo deploymentInfo) {
        return null;
      }

      @Override
      protected void setDeploymentKey(DeploymentSummary deploymentSummary, DeploymentKey deploymentKey) {}
    })
        .when(instanceHandlerFactory)
        .getInstanceHandler(any(InfrastructureMapping.class));

    doReturn(PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping()
                 .withUuid(INFRA_MAP_ID)
                 .withInfraMappingType("PHYSICAL_DATA_CENTER_SSH")
                 .withAppId(APP_ID)
                 .withServiceId(SERVICE_ID)
                 .withEnvId(ENV_ID)
                 .withInfraMappingType(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name())
                 .build())
        .when(infraMappingService)
        .get(anyString(), anyString());
    final AtomicInteger count = new AtomicInteger(0);
    doAnswer((Answer<Void>) invocationOnMock -> {
      count.incrementAndGet();
      return null;
    })
        .when(instanceService)
        .handleSyncFailure(anyString(), anyString(), anyString(), anyString(), anyString(), anyLong(), anyString());
    instanceHelper.manualSync(APP_ID, INFRA_MAP_ID);
    assertThat(count.get()).isEqualTo(1);
  }
}
