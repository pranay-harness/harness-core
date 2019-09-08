package software.wings.sm.states;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.api.ServiceElement.Builder.aServiceElement;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import com.amazonaws.regions.Regions;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.SelectedNodeExecutionData;
import software.wings.api.ServiceInstanceArtifactParam;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.HostConnectionType;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.InstanceUnitType;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.key.HostInstanceKey;
import software.wings.common.Constants;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.StateExecutionService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionInstance;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;

public class NodeSelectStateTest extends WingsBaseTest {
  @Mock private ExecutionContextImpl context;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private ContextElement contextElement;
  @Mock private ServiceInstanceArtifactParam serviceInstanceArtifactParam;
  @Mock private Artifact artifact;
  @Mock private ArtifactService artifactService;
  @Mock private InstanceService instanceService;
  @Mock private WorkflowStandardParams workflowStandardParams;
  @Mock private StateExecutionService stateExecutionService;

  @InjectMocks private NodeSelectState nodeSelectState = new DcNodeSelectState("DC_NODE_SELECT");

  StateExecutionInstance stateExecutionInstance = new StateExecutionInstance();
  AwsInfrastructureMapping awsInfrastructureMapping =
      anAwsInfrastructureMapping()
          .withInfraMappingType(InfrastructureMappingType.AWS_SSH.name())
          .withRegion(Regions.US_EAST_1.getName())
          .withUsePublicDns(true)
          .withHostConnectionType(HostConnectionType.PUBLIC_DNS.name())
          .build();

  PhysicalInfrastructureMapping physicalInfrastructureMapping =
      aPhysicalInfrastructureMapping()
          .withInfraMappingType(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH.name())
          .build();

  private ServiceTemplate SERVICE_TEMPLATE =
      aServiceTemplate().withUuid(TEMPLATE_ID).withName("template").withServiceId(SERVICE_ID).build();

  HostInstanceKey hostInstanceKey =
      HostInstanceKey.builder().hostName("host1").infraMappingId(INFRA_MAPPING_ID).build();
  Instance instance = Instance.builder().hostInstanceKey(hostInstanceKey).build();

  ServiceInstance instance1 = aServiceInstance()
                                  .withUuid(generateUuid())
                                  .withHost(aHost().withHostName("host1").build())
                                  .withServiceTemplate(SERVICE_TEMPLATE)
                                  .build();
  ServiceInstance instance2 = aServiceInstance()
                                  .withUuid(generateUuid())
                                  .withHost(aHost().withHostName("host2").build())
                                  .withServiceTemplate(SERVICE_TEMPLATE)
                                  .build();
  ServiceInstance instance3 = aServiceInstance()
                                  .withUuid(generateUuid())
                                  .withHost(aHost().withHostName("host3").build())
                                  .withServiceTemplate(SERVICE_TEMPLATE)
                                  .build();

  List<ServiceInstance> instances = Lists.newArrayList(instance1, instance2, instance3);

  @Before
  public void setUp() throws Exception {
    stateExecutionInstance.setUuid(generateUuid());
    stateExecutionInstance.setDisplayName("DC_NODE_SELECT");
    when(context.getApp()).thenReturn(anApplication().uuid(APP_ID).build());
    when(context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM))
        .thenReturn(PhaseElement.builder()
                        .serviceElement(aServiceElement().withUuid(SERVICE_ID).build())
                        .infraMappingId(INFRA_MAPPING_ID)
                        .build());
    when(context.getStateExecutionInstance()).thenReturn(stateExecutionInstance);
    when(infrastructureMappingService.selectServiceInstances(anyString(), anyString(), anyString(), any()))
        .thenReturn(instances);
    when(infrastructureMappingService.listHostDisplayNames(anyString(), anyString(), anyString()))
        .thenReturn(asList("Host1", "Host2", "Host3"));

    when(context.getContextElement(ContextElementType.INSTANCE)).thenReturn(contextElement);
    when(context.getContextElement(
             ContextElementType.PARAM, ServiceInstanceArtifactParam.SERVICE_INSTANCE_ARTIFACT_PARAMS))
        .thenReturn(serviceInstanceArtifactParam);
    when(context.getContextElement(ContextElementType.STANDARD)).thenReturn(workflowStandardParams);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestDonotExcludeHostsWithSameArtifact() {
    nodeSelectState.setInstanceCount(3);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(awsInfrastructureMapping);
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.fetchInfraMappingId()).thenReturn(INFRA_MAPPING_ID);
    when(contextElement.getUuid()).thenReturn(instance1.getUuid());
    when(serviceInstanceArtifactParam.getInstanceArtifactMap())
        .thenReturn(ImmutableMap.of(instance1.getUuid(), ARTIFACT_ID));
    when(artifactService.get(ARTIFACT_ID)).thenReturn(artifact);
    when(workflowStandardParams.isExcludeHostsWithSameArtifact()).thenReturn(false);

    PageResponse<Instance> pageResponse = aPageResponse().withResponse(asList(instance)).build();

    when(instanceService.list(any(PageRequest.class))).thenReturn(pageResponse);

    ExecutionResponse executionResponse = nodeSelectState.execute(context);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData()).isNotNull();

    SelectedNodeExecutionData selectedNodeExecutionData =
        (SelectedNodeExecutionData) executionResponse.getStateExecutionData();
    assertThat(selectedNodeExecutionData).isNotNull();
    assertThat(selectedNodeExecutionData.getServiceInstanceList()).size().isEqualTo(3);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestDonotExcludeHostsWithSameArtifactForRolling() {
    nodeSelectState.setInstanceCount(3);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(awsInfrastructureMapping);
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.fetchInfraMappingId()).thenReturn(INFRA_MAPPING_ID);
    when(context.getOrchestrationWorkflowType()).thenReturn(OrchestrationWorkflowType.ROLLING);
    when(contextElement.getUuid()).thenReturn(instance1.getUuid());
    when(serviceInstanceArtifactParam.getInstanceArtifactMap())
        .thenReturn(ImmutableMap.of(instance1.getUuid(), ARTIFACT_ID));
    when(artifactService.get(ARTIFACT_ID)).thenReturn(artifact);
    when(workflowStandardParams.isExcludeHostsWithSameArtifact()).thenReturn(true);

    PageResponse<Instance> pageResponse = aPageResponse().withResponse(asList(instance)).build();

    when(instanceService.list(any(PageRequest.class))).thenReturn(pageResponse);

    ExecutionResponse executionResponse = nodeSelectState.execute(context);
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData()).isNotNull();

    SelectedNodeExecutionData selectedNodeExecutionData =
        (SelectedNodeExecutionData) executionResponse.getStateExecutionData();
    assertThat(selectedNodeExecutionData).isNotNull();
    assertThat(selectedNodeExecutionData.getServiceInstanceList()).size().isEqualTo(3);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestExcludeHostsForPhysicalSshInfra() {
    nodeSelectState.setInstanceCount(3);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(physicalInfrastructureMapping);
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.fetchInfraMappingId()).thenReturn(INFRA_MAPPING_ID);
    when(contextElement.getUuid()).thenReturn(instance1.getUuid());
    when(serviceInstanceArtifactParam.getInstanceArtifactMap())
        .thenReturn(ImmutableMap.of(instance1.getUuid(), ARTIFACT_ID));
    when(artifactService.get(ARTIFACT_ID)).thenReturn(artifact);
    when(workflowStandardParams.isExcludeHostsWithSameArtifact()).thenReturn(true);

    PageResponse<Instance> pageResponse = aPageResponse().withResponse(asList(instance)).build();

    when(instanceService.list(any(PageRequest.class))).thenReturn(pageResponse);

    ExecutionResponse executionResponse = nodeSelectState.execute(context);

    assertResponse(executionResponse);
  }

  private void assertResponse(ExecutionResponse executionResponse) {
    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData()).isNotNull();
    SelectedNodeExecutionData selectedNodeExecutionData =
        (SelectedNodeExecutionData) executionResponse.getStateExecutionData();
    assertThat(selectedNodeExecutionData).isNotNull();
    assertThat(selectedNodeExecutionData.getServiceInstanceList()).size().isEqualTo(2);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestExcludeHostsWithSameArtifact() {
    nodeSelectState.setInstanceCount(3);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(awsInfrastructureMapping);
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.fetchInfraMappingId()).thenReturn(INFRA_MAPPING_ID);
    when(contextElement.getUuid()).thenReturn(instance1.getUuid());
    when(serviceInstanceArtifactParam.getInstanceArtifactMap())
        .thenReturn(ImmutableMap.of(instance1.getUuid(), ARTIFACT_ID));
    when(artifactService.get(ARTIFACT_ID)).thenReturn(artifact);
    when(workflowStandardParams.isExcludeHostsWithSameArtifact()).thenReturn(true);

    PageResponse<Instance> pageResponse = aPageResponse().withResponse(asList(instance)).build();

    when(instanceService.list(any(PageRequest.class))).thenReturn(pageResponse);

    ExecutionResponse executionResponse = nodeSelectState.execute(context);

    assertResponse(executionResponse);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldSucceedForPartialPercentageInstances() {
    nodeSelectState.setInstanceCount(1);
    nodeSelectState.setInstanceUnitType(InstanceUnitType.PERCENTAGE);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(physicalInfrastructureMapping);
    when(infrastructureMappingService.selectServiceInstances(anyString(), anyString(), anyString(), any()))
        .thenReturn(emptyList());
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.fetchInfraMappingId()).thenReturn(INFRA_MAPPING_ID);
    when(contextElement.getUuid()).thenReturn(instance1.getUuid());
    when(serviceInstanceArtifactParam.getInstanceArtifactMap())
        .thenReturn(ImmutableMap.of(instance1.getUuid(), ARTIFACT_ID));
    when(artifactService.get(ARTIFACT_ID)).thenReturn(artifact);
    when(workflowStandardParams.isExcludeHostsWithSameArtifact()).thenReturn(true);

    PageResponse<Instance> pageResponse = aPageResponse().withResponse(asList(instance)).build();

    when(instanceService.list(any(PageRequest.class))).thenReturn(pageResponse);

    ExecutionResponse executionResponse = nodeSelectState.execute(context);

    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    assertThat(executionResponse.getStateExecutionData()).isNotNull();
    SelectedNodeExecutionData selectedNodeExecutionData =
        (SelectedNodeExecutionData) executionResponse.getStateExecutionData();
    assertThat(selectedNodeExecutionData).isNotNull();
    assertThat(selectedNodeExecutionData.getServiceInstanceList()).size().isEqualTo(0);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFailForZeroTotalInstances() {
    nodeSelectState.setInstanceCount(100);
    nodeSelectState.setInstanceUnitType(InstanceUnitType.PERCENTAGE);
    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(physicalInfrastructureMapping);
    when(infrastructureMappingService.selectServiceInstances(anyString(), anyString(), anyString(), any()))
        .thenReturn(emptyList());
    when(infrastructureMappingService.listHostDisplayNames(anyString(), anyString(), anyString()))
        .thenReturn(emptyList());
    when(context.getAppId()).thenReturn(APP_ID);
    when(context.fetchInfraMappingId()).thenReturn(INFRA_MAPPING_ID);
    when(contextElement.getUuid()).thenReturn(instance1.getUuid());
    when(serviceInstanceArtifactParam.getInstanceArtifactMap())
        .thenReturn(ImmutableMap.of(instance1.getUuid(), ARTIFACT_ID));
    when(artifactService.get(ARTIFACT_ID)).thenReturn(artifact);
    when(workflowStandardParams.isExcludeHostsWithSameArtifact()).thenReturn(true);

    PageResponse<Instance> pageResponse = aPageResponse().withResponse(asList(instance)).build();

    when(instanceService.list(any(PageRequest.class))).thenReturn(pageResponse);

    ExecutionResponse executionResponse = nodeSelectState.execute(context);

    assertThat(executionResponse.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(executionResponse.getStateExecutionData()).isNull();
  }
}
