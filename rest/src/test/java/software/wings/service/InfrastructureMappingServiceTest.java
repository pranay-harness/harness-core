package software.wings.service;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.eraro.ErrorCode.INVALID_REQUEST;
import static io.harness.persistence.HQuery.allChecks;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.DirectKubernetesInfrastructureMapping.Builder.aDirectKubernetesInfrastructureMapping;
import static software.wings.beans.EcsInfrastructureMapping.Builder.anEcsInfrastructureMapping;
import static software.wings.beans.GcpKubernetesInfrastructureMapping.Builder.aGcpKubernetesInfrastructureMapping;
import static software.wings.beans.PhysicalDataCenterConfig.Builder.aPhysicalDataCenterConfig;
import static software.wings.beans.PhysicalInfrastructureMapping.Builder.aPhysicalInfrastructureMapping;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;
import static software.wings.beans.ServiceInstanceSelectionParams.Builder.aServiceInstanceSelectionParams;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.settings.SettingValue.SettingVariableTypes.AWS;
import static software.wings.settings.SettingValue.SettingVariableTypes.DIRECT;
import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;
import static software.wings.settings.SettingValue.SettingVariableTypes.PHYSICAL_DATA_CENTER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.CLUSTER_NAME;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID;
import static software.wings.utils.WingsTestConstants.COMPUTE_PROVIDER_ID_CHANGED;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_INSTANCE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ecs.model.LaunchType;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.WingsException;
import io.harness.persistence.HQuery;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Key;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.DelegateTask;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.beans.EcsInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.GcpConfig;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.PcfInfrastructureMapping;
import software.wings.beans.PhysicalInfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowPhase.WorkflowPhaseBuilder;
import software.wings.beans.infrastructure.Host;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.generator.InfrastructureMappingGenerator;
import software.wings.generator.OwnerManager;
import software.wings.scheduler.JobScheduler;
import software.wings.service.impl.AwsInfrastructureProvider;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.impl.InfrastructureMappingServiceImpl;
import software.wings.service.impl.StaticInfrastructureProvider;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvider;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.utils.WingsTestConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by anubhaw on 1/10/17.
 */
public class InfrastructureMappingServiceTest extends WingsBaseTest {
  public static final String ORGANIZATION = "ORGANIZATION";
  public static final String SPACE = "SPACE";
  public static final String ROUTE = "ROUTE";
  @Mock private WingsPersistence wingsPersistence;
  @Mock private Map<String, InfrastructureProvider> infrastructureProviders;
  @Mock private StaticInfrastructureProvider staticInfrastructureProvider;
  @Mock private AwsInfrastructureProvider awsInfrastructureProvider;

  @Mock private ServiceInstanceService serviceInstanceService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private SettingsService settingsService;
  @Mock private AppService appService;
  @Mock private EnvironmentService envService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private WorkflowService workflowService;
  @Mock private JobScheduler jobScheduler;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private YamlDirectoryService yamlDirectoryService;
  @Mock private YamlChangeSetHelper yamlChangeSetHelper;

  @Inject @InjectMocks private InfrastructureMappingService infrastructureMappingService;

  @Inject InfrastructureMappingGenerator infrastructureMappingGenerator;
  @Inject private OwnerManager ownerManager;

  @Mock private SecretManager secretManager;
  @Mock private HQuery<InfrastructureMapping> query;
  @Mock private UpdateOperations<InfrastructureMapping> updateOperations;
  @Mock private FieldEnd end;
  @Mock private Application app;
  @Mock private Environment env;
  @Mock private Service service;
  @Mock private ContainerService containerService;

  @Before
  public void setUp() throws Exception {
    when(infrastructureProviders.get(SettingVariableTypes.AWS.name())).thenReturn(awsInfrastructureProvider);
    when(infrastructureProviders.get(PHYSICAL_DATA_CENTER.name())).thenReturn(staticInfrastructureProvider);
    when(wingsPersistence.createQuery(InfrastructureMapping.class)).thenReturn(query);
    when(wingsPersistence.createUpdateOperations(InfrastructureMapping.class)).thenReturn(updateOperations);
    when(query.filter(any(), any())).thenReturn(query);
    when(secretManager.getEncryptionDetails(anyObject(), anyString(), anyString())).thenReturn(Collections.emptyList());
    setInternalState(infrastructureMappingService, "secretManager", secretManager);

    when(appService.get(APP_ID)).thenReturn(app);
    when(envService.get(APP_ID, ENV_ID, false)).thenReturn(env);
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(service);
    when(app.getName()).thenReturn(APP_NAME);
    when(env.getName()).thenReturn(ENV_NAME);
    when(service.getName()).thenReturn(SERVICE_NAME);
  }

  @Test
  public void shouldList() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping = aPhysicalInfrastructureMapping()
                                                                      .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                                      .withComputeProviderSettingId(SETTING_ID)
                                                                      .withAppId(APP_ID)
                                                                      .withEnvId(ENV_ID)
                                                                      .withServiceTemplateId(TEMPLATE_ID)
                                                                      .build();

    PageRequest<InfrastructureMapping> pageRequest = new PageRequest<>();
    PageResponse pageResponse = aPageResponse().withResponse(singletonList(physicalInfrastructureMapping)).build();
    when(wingsPersistence.query(InfrastructureMapping.class, pageRequest, allChecks)).thenReturn(pageResponse);

    PageResponse<InfrastructureMapping> infrastructureMappings = infrastructureMappingService.list(pageRequest);
    assertThat(infrastructureMappings).hasSize(1).containsExactly(physicalInfrastructureMapping);
    verify(wingsPersistence).query(InfrastructureMapping.class, pageRequest, allChecks);
  }

  @Test
  public void shouldSave() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping =
        aPhysicalInfrastructureMapping()
            .withName("NAME")
            .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
            .withComputeProviderSettingId(SETTING_ID)
            .withAppId(APP_ID)
            .withEnvId(ENV_ID)
            .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
            .withComputeProviderType(PHYSICAL_DATA_CENTER.name())
            .withAccountId(ACCOUNT_ID)
            .withDeploymentType(DeploymentType.SSH.name())
            .withServiceTemplateId(TEMPLATE_ID)
            .withHostNames(singletonList(HOST_NAME))
            .withInfraMappingType(PHYSICAL_DATA_CENTER.name())
            .build();

    PhysicalInfrastructureMapping savedPhysicalInfrastructureMapping =
        aPhysicalInfrastructureMapping()
            .withName("NAME")
            .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
            .withComputeProviderSettingId(SETTING_ID)
            .withUuid(WingsTestConstants.INFRA_MAPPING_ID)
            .withAppId(APP_ID)
            .withEnvId(ENV_ID)
            .withComputeProviderType(PHYSICAL_DATA_CENTER.name())
            .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
            .withDeploymentType(DeploymentType.SSH.name())
            .withServiceId(SERVICE_ID)
            .withAccountId(ACCOUNT_ID)
            .withServiceTemplateId(TEMPLATE_ID)
            .withHostNames(singletonList(HOST_NAME))
            .withInfraMappingType(PHYSICAL_DATA_CENTER.name())
            .build();

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);
    doReturn(savedPhysicalInfrastructureMapping)
        .when(wingsPersistence)
        .saveAndGet(InfrastructureMapping.class, physicalInfrastructureMapping);
    doReturn(aServiceTemplate().withAppId(APP_ID).withServiceId(SERVICE_ID).withUuid(TEMPLATE_ID).build())
        .when(serviceTemplateService)
        .get(APP_ID, TEMPLATE_ID);
    doReturn(aSettingAttribute().withUuid(COMPUTE_PROVIDER_ID).withValue(aPhysicalDataCenterConfig().build()).build())
        .when(settingsService)
        .get(COMPUTE_PROVIDER_ID);

    InfrastructureMapping returnedInfrastructureMapping =
        infrastructureMappingService.save(physicalInfrastructureMapping);

    assertThat(returnedInfrastructureMapping.getUuid()).isEqualTo(INFRA_MAPPING_ID);
    verify(serviceTemplateService).get(APP_ID, TEMPLATE_ID);
  }

  @Test
  public void shouldGet() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping = aPhysicalInfrastructureMapping()
                                                                      .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                                      .withComputeProviderSettingId(SETTING_ID)
                                                                      .withAppId(APP_ID)
                                                                      .withEnvId(ENV_ID)
                                                                      .withUuid(INFRA_MAPPING_ID)
                                                                      .withServiceTemplateId(TEMPLATE_ID)
                                                                      .withHostNames(singletonList(HOST_NAME))
                                                                      .build();

    when(wingsPersistence.get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID))
        .thenReturn(physicalInfrastructureMapping);

    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID);
    assertThat(infrastructureMapping.getUuid()).isEqualTo(INFRA_MAPPING_ID);
    verify(wingsPersistence).get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID);
  }

  @Test
  public void shouldUpdate() {
    PhysicalInfrastructureMapping savedInfra = aPhysicalInfrastructureMapping()
                                                   .withName("Name1")
                                                   .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                   .withComputeProviderSettingId(SETTING_ID)
                                                   .withComputeProviderType(PHYSICAL_DATA_CENTER.name())
                                                   .withDeploymentType(DeploymentType.SSH.name())
                                                   .withAppId(APP_ID)
                                                   .withEnvId(ENV_ID)
                                                   .withServiceId(SERVICE_ID)
                                                   .withUuid(INFRA_MAPPING_ID)
                                                   .withServiceTemplateId(TEMPLATE_ID)
                                                   .withHostNames(singletonList(HOST_NAME))
                                                   .withInfraMappingType(PHYSICAL_DATA_CENTER.name())
                                                   .build();

    PhysicalInfrastructureMapping updatedInfra = aPhysicalInfrastructureMapping()
                                                     .withName("Name2")
                                                     .withHostConnectionAttrs("HOST_CONN_ATTR_ID_1")
                                                     .withComputeProviderSettingId(SETTING_ID)
                                                     .withComputeProviderType(PHYSICAL_DATA_CENTER.name())
                                                     .withAccountId(ACCOUNT_ID)
                                                     .withDeploymentType(DeploymentType.SSH.name())
                                                     .withAppId(APP_ID)
                                                     .withEnvId(ENV_ID)
                                                     .withUuid(INFRA_MAPPING_ID)
                                                     .withServiceId(SERVICE_ID)
                                                     .withServiceTemplateId(TEMPLATE_ID)
                                                     .withHostNames(singletonList("HOST_NAME_1"))
                                                     .withInfraMappingType(PHYSICAL_DATA_CENTER.name())
                                                     .build();

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);

    doReturn(savedInfra).when(wingsPersistence).get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID);

    doReturn(aSettingAttribute().withUuid(COMPUTE_PROVIDER_ID).withValue(aPhysicalDataCenterConfig().build()).build())
        .when(settingsService)
        .get(COMPUTE_PROVIDER_ID);

    doReturn(aServiceTemplate().withAppId(APP_ID).withServiceId(SERVICE_ID).withUuid(TEMPLATE_ID).build())
        .when(serviceTemplateService)
        .get(APP_ID, TEMPLATE_ID);

    InfrastructureMapping returnedInfra = infrastructureMappingService.update(updatedInfra);
    assertThat(returnedInfra).isNotNull();
    verify(wingsPersistence, times(2)).get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID);
    verify(staticInfrastructureProvider).updateHostConnAttrs(updatedInfra, updatedInfra.getHostConnectionAttrs());
  }

  @Test
  public void shouldUpdateInfraComputerProviderId() {
    PhysicalInfrastructureMapping savedInfra = aPhysicalInfrastructureMapping()
                                                   .withName("Name3")
                                                   .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                   .withComputeProviderSettingId(SETTING_ID)
                                                   .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                   .withComputeProviderType(PHYSICAL_DATA_CENTER.name())
                                                   .withDeploymentType(DeploymentType.SSH.name())
                                                   .withAppId(APP_ID)
                                                   .withEnvId(ENV_ID)
                                                   .withServiceId(SERVICE_ID)
                                                   .withUuid(INFRA_MAPPING_ID)
                                                   .withServiceTemplateId(TEMPLATE_ID)
                                                   .withHostNames(singletonList(HOST_NAME))
                                                   .withInfraMappingType(PHYSICAL_DATA_CENTER.name())
                                                   .build();

    PhysicalInfrastructureMapping updatedInfra = aPhysicalInfrastructureMapping()
                                                     .withName("Name4")
                                                     .withHostConnectionAttrs("HOST_CONN_ATTR_ID_1")
                                                     .withComputeProviderSettingId(SETTING_ID)
                                                     .withComputeProviderSettingId(COMPUTE_PROVIDER_ID_CHANGED)
                                                     .withComputeProviderName(COMPUTE_PROVIDER_ID_CHANGED)
                                                     .withComputeProviderType(PHYSICAL_DATA_CENTER.name())
                                                     .withAccountId(ACCOUNT_ID)
                                                     .withDeploymentType(DeploymentType.SSH.name())
                                                     .withAppId(APP_ID)
                                                     .withEnvId(ENV_ID)
                                                     .withUuid(INFRA_MAPPING_ID)
                                                     .withServiceId(SERVICE_ID)
                                                     .withServiceTemplateId(TEMPLATE_ID)
                                                     .withHostNames(singletonList(HOST_NAME))
                                                     .withInfraMappingType(PHYSICAL_DATA_CENTER.name())
                                                     .build();

    when(appService.getAccountIdByAppId(APP_ID)).thenReturn(ACCOUNT_ID);

    doReturn(savedInfra).when(wingsPersistence).get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID);

    doReturn(aSettingAttribute()
                 .withUuid(COMPUTE_PROVIDER_ID)
                 .withName(COMPUTE_PROVIDER_ID)
                 .withValue(aPhysicalDataCenterConfig().build())
                 .build())
        .when(settingsService)
        .get(COMPUTE_PROVIDER_ID);

    doReturn(aSettingAttribute()
                 .withUuid(COMPUTE_PROVIDER_ID_CHANGED)
                 .withName(COMPUTE_PROVIDER_ID_CHANGED)
                 .withValue(aPhysicalDataCenterConfig().build())
                 .build())
        .when(settingsService)
        .get(COMPUTE_PROVIDER_ID_CHANGED);

    doReturn(aServiceTemplate().withAppId(APP_ID).withServiceId(SERVICE_ID).withUuid(TEMPLATE_ID).build())
        .when(serviceTemplateService)
        .get(APP_ID, TEMPLATE_ID);

    InfrastructureMapping returnedInfra = infrastructureMappingService.update(updatedInfra);
    assertThat(returnedInfra).isNotNull();
    Map<String, Object> keyValuePairs = new LinkedHashMap<>();
    keyValuePairs.put("loadBalancerId", null);
    keyValuePairs.put("computeProviderSettingId", COMPUTE_PROVIDER_ID_CHANGED);
    keyValuePairs.put("hostConnectionAttrs", "HOST_CONN_ATTR_ID_1");
    keyValuePairs.put("hostNames", singletonList(HOST_NAME));
    keyValuePairs.put("computeProviderName", COMPUTE_PROVIDER_ID_CHANGED);
    keyValuePairs.put("name", "Name4");

    Set<String> fieldsToRemove = new HashSet<>();
    fieldsToRemove.add("provisionerId");
    verify(wingsPersistence)
        .updateFields(PhysicalInfrastructureMapping.class, INFRA_MAPPING_ID, keyValuePairs, fieldsToRemove);
    verify(wingsPersistence, times(2)).get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID);
    verify(staticInfrastructureProvider).updateHostConnAttrs(updatedInfra, updatedInfra.getHostConnectionAttrs());
  }

  @Test
  public void shouldDelete() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping =
        aPhysicalInfrastructureMapping()
            .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
            .withComputeProviderSettingId(SETTING_ID)
            .withAppId(APP_ID)
            .withEnvId(ENV_ID)
            .withComputeProviderType(PHYSICAL_DATA_CENTER.name())
            .withUuid(INFRA_MAPPING_ID)
            .withServiceTemplateId(TEMPLATE_ID)
            .withHostNames(singletonList(HOST_NAME))
            .build();

    when(wingsPersistence.get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID))
        .thenReturn(physicalInfrastructureMapping);
    when(wingsPersistence.delete(physicalInfrastructureMapping)).thenReturn(true);
    when(workflowService.listWorkflows(any(PageRequest.class))).thenReturn(aPageResponse().build());

    infrastructureMappingService.delete(APP_ID, INFRA_MAPPING_ID);

    verify(wingsPersistence).get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID);
    verify(wingsPersistence).delete(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID);
  }

  @Test
  public void shouldPruneDescendingObjects() {
    infrastructureMappingService.pruneDescendingEntities(APP_ID, INFRA_MAPPING_ID);
    InOrder inOrder = inOrder(wingsPersistence, serviceInstanceService);
    inOrder.verify(serviceInstanceService).pruneByInfrastructureMapping(APP_ID, INFRA_MAPPING_ID);
  }

  @Test
  public void shouldThrowExceptionOnReferencedInfraMappingDelete() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping =
        aPhysicalInfrastructureMapping()
            .withUuid(INFRA_MAPPING_ID)
            .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
            .withComputeProviderSettingId(SETTING_ID)
            .withAppId(APP_ID)
            .withEnvId(ENV_ID)
            .withComputeProviderType(PHYSICAL_DATA_CENTER.name())
            .withUuid(INFRA_MAPPING_ID)
            .withServiceTemplateId(TEMPLATE_ID)
            .withHostNames(singletonList(HOST_NAME))
            .build();

    when(wingsPersistence.get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID))
        .thenReturn(physicalInfrastructureMapping);
    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withOrchestrationWorkflow(
                aBasicOrchestrationWorkflow()
                    .withWorkflowPhaseIdMap(ImmutableMap.of(
                        "PHASE_ID", WorkflowPhaseBuilder.aWorkflowPhase().withInfraMappingId(INFRA_MAPPING_ID).build()))
                    .build())
            .build();
    when(workflowService.listWorkflows(any(PageRequest.class)))
        .thenReturn(aPageResponse().withResponse(asList(workflow)).build());

    assertThatThrownBy(() -> infrastructureMappingService.delete(APP_ID, INFRA_MAPPING_ID))
        .isInstanceOf(WingsException.class)
        .hasMessage(INVALID_REQUEST.name());
  }

  @Test
  public void shouldSelectServiceInstancesForPhysicalInfrastructure() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping =
        aPhysicalInfrastructureMapping()
            .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
            .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
            .withAppId(APP_ID)
            .withEnvId(ENV_ID)
            .withComputeProviderType(PHYSICAL_DATA_CENTER.name())
            .withUuid(INFRA_MAPPING_ID)
            .withServiceTemplateId(TEMPLATE_ID)
            .withHostNames(singletonList(HOST_NAME))
            .build();

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(physicalInfrastructureMapping);
    when(settingsService.get(COMPUTE_PROVIDER_ID))
        .thenReturn(
            aSettingAttribute().withUuid(COMPUTE_PROVIDER_ID).withValue(aPhysicalDataCenterConfig().build()).build());
    when(serviceInstanceService.updateInstanceMappings(
             any(ServiceTemplate.class), any(InfrastructureMapping.class), any(List.class)))
        .thenReturn(
            aPageResponse().withResponse(asList(aServiceInstance().withUuid(SERVICE_INSTANCE_ID).build())).build());

    List<ServiceInstance> serviceInstances = infrastructureMappingService.selectServiceInstances(
        APP_ID, INFRA_MAPPING_ID, null, aServiceInstanceSelectionParams().withCount(Integer.MAX_VALUE).build());

    assertThat(serviceInstances).hasSize(1);
    assertThat(serviceInstances).containsExactly(aServiceInstance().withUuid(SERVICE_INSTANCE_ID).build());
    verify(serviceInstanceService)
        .updateInstanceMappings(any(ServiceTemplate.class), any(InfrastructureMapping.class), any(List.class));
  }

  @Test
  public void shouldSelectServiceInstancesForAwsInfrastructure() {
    AwsInfrastructureMapping awsInfrastructureMapping = anAwsInfrastructureMapping()
                                                            .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                            .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                            .withAppId(APP_ID)
                                                            .withEnvId(ENV_ID)
                                                            .withComputeProviderType(AWS.name())
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withServiceTemplateId(TEMPLATE_ID)
                                                            .withRegion(Regions.US_EAST_1.getName())
                                                            .withUsePublicDns(false)
                                                            .build();

    when(infrastructureMappingService.get(APP_ID, INFRA_MAPPING_ID)).thenReturn(awsInfrastructureMapping);

    SettingAttribute computeProviderSetting =
        aSettingAttribute().withUuid(COMPUTE_PROVIDER_ID).withValue(AwsConfig.builder().build()).build();
    when(settingsService.get(COMPUTE_PROVIDER_ID)).thenReturn(computeProviderSetting);

    List<Host> newHosts = singletonList(aHost().withEc2Instance(new Instance().withPrivateDnsName(HOST_NAME)).build());

    when(awsInfrastructureProvider.listHosts(
             awsInfrastructureMapping, computeProviderSetting, Collections.emptyList(), new PageRequest<>()))
        .thenReturn(aPageResponse().withResponse(newHosts).build());

    when(awsInfrastructureProvider.saveHost(newHosts.get(0))).thenReturn(newHosts.get(0));

    ServiceTemplate serviceTemplate = aServiceTemplate().withAppId(APP_ID).withUuid(TEMPLATE_ID).build();
    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(serviceTemplate);

    when(serviceInstanceService.updateInstanceMappings(
             any(ServiceTemplate.class), any(InfrastructureMapping.class), any(List.class)))
        .thenReturn(
            aPageResponse().withResponse(asList(aServiceInstance().withUuid(SERVICE_INSTANCE_ID).build())).build());

    List<ServiceInstance> serviceInstances = infrastructureMappingService.selectServiceInstances(
        APP_ID, INFRA_MAPPING_ID, null, aServiceInstanceSelectionParams().withCount(Integer.MAX_VALUE).build());

    assertThat(serviceInstances).hasSize(1);
    assertThat(serviceInstances).containsExactly(aServiceInstance().withUuid(SERVICE_INSTANCE_ID).build());
    verify(settingsService).get(COMPUTE_PROVIDER_ID);
    verify(awsInfrastructureProvider)
        .listHosts(awsInfrastructureMapping, computeProviderSetting, Collections.emptyList(), new PageRequest<>());
    verify(awsInfrastructureProvider).saveHost(newHosts.get(0));
    verify(serviceTemplateService).get(APP_ID, TEMPLATE_ID);
    verify(serviceInstanceService).updateInstanceMappings(serviceTemplate, awsInfrastructureMapping, newHosts);
  }

  @Test
  public void shouldListPhysicalComputeProviderHosts() {
    PhysicalInfrastructureMapping physicalInfrastructureMapping =
        aPhysicalInfrastructureMapping()
            .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
            .withComputeProviderSettingId(SETTING_ID)
            .withAppId(APP_ID)
            .withEnvId(ENV_ID)
            .withComputeProviderType(PHYSICAL_DATA_CENTER.name())
            .withUuid(INFRA_MAPPING_ID)
            .withServiceTemplateId(TEMPLATE_ID)
            .withHostNames(singletonList(HOST_NAME))
            .build();

    when(serviceTemplateService.getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID))
        .thenReturn(singletonList(new Key<>(ServiceTemplate.class, "serviceTemplate", TEMPLATE_ID)));
    when(query.get()).thenReturn(physicalInfrastructureMapping);

    List<String> hostNames = infrastructureMappingService.listComputeProviderHostDisplayNames(
        APP_ID, ENV_ID, SERVICE_ID, COMPUTE_PROVIDER_ID);
    assertThat(hostNames).hasSize(1).containsExactly(HOST_NAME);
    verify(serviceTemplateService).getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID);
    verify(query).get();
  }

  @Test
  public void shouldListAwsComputeProviderHosts() {
    AwsInfrastructureMapping awsInfrastructureMapping = anAwsInfrastructureMapping()
                                                            .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                            .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                            .withAppId(APP_ID)
                                                            .withEnvId(ENV_ID)
                                                            .withComputeProviderType(AWS.name())
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withServiceTemplateId(TEMPLATE_ID)
                                                            .withRegion(Regions.US_EAST_1.getName())
                                                            .withUsePublicDns(true)
                                                            .build();

    when(serviceTemplateService.getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID))
        .thenReturn(singletonList(new Key<>(ServiceTemplate.class, "serviceTemplate", TEMPLATE_ID)));
    when(query.get()).thenReturn(awsInfrastructureMapping);

    SettingAttribute computeProviderSetting =
        aSettingAttribute().withUuid(COMPUTE_PROVIDER_ID).withValue(AwsConfig.builder().build()).build();
    when(settingsService.get(COMPUTE_PROVIDER_ID)).thenReturn(computeProviderSetting);

    when(awsInfrastructureProvider.listHosts(
             awsInfrastructureMapping, computeProviderSetting, Collections.emptyList(), new PageRequest<>()))
        .thenReturn(
            aPageResponse()
                .withResponse(asList(aHost().withHostName("host1").build(),
                    aHost()
                        .withHostName("host2")
                        .withEc2Instance(new Instance().withTags(new Tag().withKey("Other").withValue("otherValue")))
                        .build(),
                    aHost()
                        .withHostName("host3")
                        .withEc2Instance(new Instance().withTags(new Tag().withKey("Name")))
                        .build(),
                    aHost()
                        .withHostName("host4")
                        .withEc2Instance(new Instance().withTags(new Tag().withKey("Name").withValue("Host 4")))
                        .build()))
                .build());

    List<String> hostNames = infrastructureMappingService.listComputeProviderHostDisplayNames(
        APP_ID, ENV_ID, SERVICE_ID, COMPUTE_PROVIDER_ID);

    assertThat(hostNames).hasSize(4).containsExactly("host1", "host2", "host3", "host4 [Host 4]");
    verify(serviceTemplateService).getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID);
    verify(settingsService).get(COMPUTE_PROVIDER_ID);
    verify(awsInfrastructureProvider)
        .listHosts(awsInfrastructureMapping, computeProviderSetting, Collections.emptyList(), new PageRequest<>());
  }

  @Test
  public void shouldProvisionNodes() {
    AwsInfrastructureMapping awsInfrastructureMapping = anAwsInfrastructureMapping()
                                                            .withHostConnectionAttrs(HOST_CONN_ATTR_ID)
                                                            .withComputeProviderSettingId(SETTING_ID)
                                                            .withAppId(APP_ID)
                                                            .withEnvId(ENV_ID)
                                                            .withComputeProviderType(AWS.name())
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                            .withServiceTemplateId(TEMPLATE_ID)
                                                            .withRegion(Regions.US_EAST_1.getName())
                                                            .withAutoScalingGroupName("AUTOSCALING_GROUP")
                                                            .withProvisionInstances(true)
                                                            .withSetDesiredCapacity(true)
                                                            .withDesiredCapacity(1)
                                                            .build();

    when(wingsPersistence.get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID))
        .thenReturn(awsInfrastructureMapping);

    SettingAttribute computeProviderSetting =
        aSettingAttribute().withUuid(COMPUTE_PROVIDER_ID).withValue(AwsConfig.builder().build()).build();
    when(settingsService.get(COMPUTE_PROVIDER_ID)).thenReturn(computeProviderSetting);

    Host provisionedHost = aHost().withHostName(HOST_NAME).build();
    when(awsInfrastructureProvider.maybeSetAutoScaleCapacityAndGetHosts(
             APP_ID, null, awsInfrastructureMapping, computeProviderSetting))
        .thenReturn(singletonList(provisionedHost));

    when(awsInfrastructureProvider.saveHost(provisionedHost)).thenReturn(provisionedHost);

    ServiceTemplate serviceTemplate = aServiceTemplate().withAppId(APP_ID).withUuid(TEMPLATE_ID).build();
    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID)).thenReturn(serviceTemplate);

    when(serviceInstanceService.updateInstanceMappings(
             any(ServiceTemplate.class), any(InfrastructureMapping.class), any(List.class)))
        .thenReturn(
            aPageResponse().withResponse(asList(aServiceInstance().withUuid(SERVICE_INSTANCE_ID).build())).build());

    List<ServiceInstance> serviceInstances =
        infrastructureMappingService.selectServiceInstances(APP_ID, INFRA_MAPPING_ID, null,
            aServiceInstanceSelectionParams().withCount(1).withExcludedServiceInstanceIds(emptyList()).build());

    assertThat(serviceInstances).hasSize(1);
    assertThat(serviceInstances).containsExactly(aServiceInstance().withUuid(SERVICE_INSTANCE_ID).build());
    verify(settingsService).get(COMPUTE_PROVIDER_ID);
    verify(awsInfrastructureProvider)
        .maybeSetAutoScaleCapacityAndGetHosts(APP_ID, null, awsInfrastructureMapping, computeProviderSetting);
    verify(awsInfrastructureProvider).saveHost(provisionedHost);
    verify(serviceTemplateService).get(APP_ID, TEMPLATE_ID);
    verify(serviceInstanceService)
        .updateInstanceMappings(serviceTemplate, awsInfrastructureMapping, singletonList(provisionedHost));
  }

  @Test
  public void shouldGetContainerRunningInstancesDirectKubernetes() {
    DirectKubernetesInfrastructureMapping directKubernetesInfrastructureMapping =
        aDirectKubernetesInfrastructureMapping()
            .withNamespace("default")
            .withAppId(APP_ID)
            .withEnvId(ENV_ID)
            .withServiceId(SERVICE_ID)
            .withServiceTemplateId(TEMPLATE_ID)
            .withComputeProviderType(DIRECT.name())
            .withUuid(INFRA_MAPPING_ID)
            .build();

    when(serviceTemplateService.getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID))
        .thenReturn(singletonList(new Key<>(ServiceTemplate.class, "serviceTemplate", TEMPLATE_ID)));
    when(wingsPersistence.get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID))
        .thenReturn(directKubernetesInfrastructureMapping);
    when(delegateProxyFactory.get(eq(ContainerService.class), any(DelegateTask.SyncTaskContext.class)))
        .thenReturn(containerService);
    Map<String, Integer> activeCounts = new LinkedHashMap<>();
    activeCounts.put("app-name.service-name.env-name-1", 2);
    activeCounts.put("app-name.service-name.env-name-2", 3);
    ArgumentCaptor<ContainerServiceParams> captor = ArgumentCaptor.forClass(ContainerServiceParams.class);
    when(containerService.getActiveServiceCounts(captor.capture())).thenReturn(activeCounts);

    String result = infrastructureMappingService.getContainerRunningInstances(
        APP_ID, INFRA_MAPPING_ID, "${app.name}.${service.name}.${env.name}");
    assertThat(result).isEqualTo("5");
    assertThat(captor.getValue().getContainerServiceName()).isEqualTo("app-name.service-name.env-name-0");
  }

  @Test
  public void shouldGetContainerRunningInstancesGcp() {
    GcpKubernetesInfrastructureMapping gcpKubernetesInfrastructureMapping =
        aGcpKubernetesInfrastructureMapping()
            .withNamespace("default")
            .withAppId(APP_ID)
            .withEnvId(ENV_ID)
            .withServiceId(SERVICE_ID)
            .withServiceTemplateId(TEMPLATE_ID)
            .withComputeProviderType(GCP.name())
            .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
            .withUuid(INFRA_MAPPING_ID)
            .build();

    when(settingsService.get(COMPUTE_PROVIDER_ID))
        .thenReturn(aSettingAttribute().withUuid(COMPUTE_PROVIDER_ID).withValue(GcpConfig.builder().build()).build());
    when(serviceTemplateService.getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID))
        .thenReturn(singletonList(new Key<>(ServiceTemplate.class, "serviceTemplate", TEMPLATE_ID)));
    when(wingsPersistence.get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID))
        .thenReturn(gcpKubernetesInfrastructureMapping);
    when(delegateProxyFactory.get(eq(ContainerService.class), any(DelegateTask.SyncTaskContext.class)))
        .thenReturn(containerService);
    Map<String, Integer> activeCounts = new LinkedHashMap<>();
    activeCounts.put("app-name.service-name.env-name-1", 2);
    activeCounts.put("app-name.service-name.env-name-2", 3);
    ArgumentCaptor<ContainerServiceParams> captor = ArgumentCaptor.forClass(ContainerServiceParams.class);
    when(containerService.getActiveServiceCounts(captor.capture())).thenReturn(activeCounts);

    String result = infrastructureMappingService.getContainerRunningInstances(
        APP_ID, INFRA_MAPPING_ID, "${app.name}.${service.name}.${env.name}");
    assertThat(result).isEqualTo("5");
    assertThat(captor.getValue().getContainerServiceName()).isEqualTo("app-name.service-name.env-name-0");
  }

  @Test
  public void shouldGetContainerRunningInstancesEcs() {
    EcsInfrastructureMapping ecsInfrastructureMapping = anEcsInfrastructureMapping()
                                                            .withRegion("us-east-1")
                                                            .withAppId(APP_ID)
                                                            .withEnvId(ENV_ID)
                                                            .withServiceId(SERVICE_ID)
                                                            .withServiceTemplateId(TEMPLATE_ID)
                                                            .withComputeProviderType(AWS.name())
                                                            .withComputeProviderSettingId(COMPUTE_PROVIDER_ID)
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .build();

    when(settingsService.get(COMPUTE_PROVIDER_ID))
        .thenReturn(aSettingAttribute().withUuid(COMPUTE_PROVIDER_ID).withValue(AwsConfig.builder().build()).build());
    when(serviceTemplateService.getTemplateRefKeysByService(APP_ID, SERVICE_ID, ENV_ID))
        .thenReturn(singletonList(new Key<>(ServiceTemplate.class, "serviceTemplate", TEMPLATE_ID)));
    when(wingsPersistence.get(InfrastructureMapping.class, APP_ID, INFRA_MAPPING_ID))
        .thenReturn(ecsInfrastructureMapping);
    when(delegateProxyFactory.get(eq(ContainerService.class), any(DelegateTask.SyncTaskContext.class)))
        .thenReturn(containerService);
    Map<String, Integer> activeCounts = new LinkedHashMap<>();
    activeCounts.put("APP_NAME__SERVICE_NAME__ENV_NAME__1", 2);
    activeCounts.put("APP_NAME__SERVICE_NAME__ENV_NAME__2", 3);
    ArgumentCaptor<ContainerServiceParams> captor = ArgumentCaptor.forClass(ContainerServiceParams.class);
    when(containerService.getActiveServiceCounts(captor.capture())).thenReturn(activeCounts);

    String result = infrastructureMappingService.getContainerRunningInstances(
        APP_ID, INFRA_MAPPING_ID, "${app.name}__${service.name}__${env.name}");
    assertThat(result).isEqualTo("5");
    assertThat(captor.getValue().getContainerServiceName()).isEqualTo("APP_NAME__SERVICE_NAME__ENV_NAME__0");
  }

  @Test
  public void testHandleEcsInfraMapping() throws Exception {
    InfrastructureMappingServiceImpl serviceImpl = (InfrastructureMappingServiceImpl) infrastructureMappingService;

    Map<String, Object> keyValuePairs = new HashMap<>();

    MethodUtils.invokeMethod(serviceImpl, true, "handleEcsInfraMapping",
        new Object[] {keyValuePairs,
            anEcsInfrastructureMapping()
                .withClusterName(CLUSTER_NAME)
                .withRegion("us-east-1")
                .withVpcId(null)
                .withSubnetIds(null)
                .withSecurityGroupIds(null)
                .withExecutionRole(null)
                .withLaunchType(LaunchType.EC2.name())
                .build()});

    // map should not contains any null values as that cause db.update to fail
    assertEquals(8, keyValuePairs.size());
    assertEquals(keyValuePairs.get("clusterName"), CLUSTER_NAME);
    assertEquals(keyValuePairs.get("region"), "us-east-1");
    assertEquals(keyValuePairs.get("assignPublicIp"), false);
    assertEquals(keyValuePairs.get("launchType"), LaunchType.EC2.name());
    assertEquals(keyValuePairs.get("executionRole"), StringUtils.EMPTY);
    assertEquals(keyValuePairs.get("vpcId"), StringUtils.EMPTY);

    assertNotNull(keyValuePairs.get("subnetIds"));
    assertEquals(0, ((List) keyValuePairs.get("subnetIds")).size());

    assertNotNull(keyValuePairs.get("securityGroupIds"));
    assertEquals(0, ((List) keyValuePairs.get("securityGroupIds")).size());
  }

  @Test
  public void testHandlePcdInfraMapping() throws Exception {
    InfrastructureMappingServiceImpl serviceImpl = (InfrastructureMappingServiceImpl) infrastructureMappingService;

    Map<String, Object> keyValuePairs = new HashMap<>();

    MethodUtils.invokeMethod(serviceImpl, true, "handlePcfInfraMapping",
        new Object[] {keyValuePairs,
            PcfInfrastructureMapping.builder()
                .organization(ORGANIZATION)
                .space(SPACE)
                .routeMaps(Arrays.asList(ROUTE))
                .tempRouteMap(Arrays.asList(ROUTE))
                .build()});

    assertEquals(4, keyValuePairs.size());
    assertEquals(keyValuePairs.get("organization"), ORGANIZATION);
    assertEquals(keyValuePairs.get("space"), SPACE);
    assertNotNull(keyValuePairs.get("tempRouteMap"));
    assertEquals(1, ((List) keyValuePairs.get("tempRouteMap")).size());
    assertEquals(1, ((List) keyValuePairs.get("routeMaps")).size());
    assertEquals(ROUTE, ((List) keyValuePairs.get("tempRouteMap")).get(0));
    assertEquals(ROUTE, ((List) keyValuePairs.get("routeMaps")).get(0));
  }
}
