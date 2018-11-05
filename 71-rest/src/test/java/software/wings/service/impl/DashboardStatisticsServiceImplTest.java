package software.wings.service.impl;

import static com.google.common.collect.Sets.newHashSet;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static java.util.Arrays.asList;
import static java.util.Objects.deepEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static software.wings.beans.WorkflowExecution.WorkflowExecutionBuilder.aWorkflowExecution;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_1_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ACCOUNT_2_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_1_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_2_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_3_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_4_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_5_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.APP_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.COMPUTE_PROVIDER_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.CONTAINER_1_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.CONTAINER_2_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.CONTAINER_3_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.CONTAINER_4_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.CONTAINER_5_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.CONTAINER_6_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.CONTAINER_7_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.CONTAINER_8_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_1_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_2_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_3_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_4_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_5_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_6_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.ENV_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_1_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_2_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_3_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_4_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_5_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_6_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_7_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_8_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INFRA_MAPPING_NAME;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_10_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_11_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_12_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_1_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_2_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_3_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_4_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_5_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_6_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_7_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_8_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.INSTANCE_9_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.KUBE_CLUSTER;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_1_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_2_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_3_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_4_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_5_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_6_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_7_ID;
import static software.wings.service.impl.instance.InstanceSyncTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.BUILD_NO;
import static software.wings.utils.WingsTestConstants.PIPELINE_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_NAME;
import static software.wings.utils.WingsTestConstants.USER_ID;
import static software.wings.utils.WingsTestConstants.USER_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_EXECUTION_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.rule.RealMongo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.Service;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.Instance.InstanceBuilder;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.beans.instance.dashboard.ArtifactSummary;
import software.wings.beans.instance.dashboard.EntitySummary;
import software.wings.beans.instance.dashboard.InstanceStatsByService;
import software.wings.beans.instance.dashboard.service.DeploymentHistory;
import software.wings.beans.instance.dashboard.service.ServiceInstanceDashboard;
import software.wings.dl.WingsPersistence;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.PipelineSummary;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author rktummala on 9/4/18
 */
public class DashboardStatisticsServiceImplTest extends WingsBaseTest {
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private EnvironmentService environmentService;
  @Mock private InfrastructureMappingService infraMappingService;
  @Mock private AppService appService;

  @Inject private WingsPersistence wingsPersistence;
  @InjectMocks @Inject private DashboardStatisticsService dashboardService = spy(DashboardStatisticsServiceImpl.class);

  private Instance instance1;
  private Instance instance2;
  private Instance instance3;
  private Instance instance4;
  private Instance instance5;
  private Instance instance6;
  private Instance instance7;
  private Instance instance8;
  private Instance deletedInstance9;
  private Instance deletedInstance10;
  private Instance deletedInstance11;
  private Instance deletedInstance12;
  private Instance deletedInstance13;
  private Instance deletedInstance14;
  private Instance deletedInstance15;
  private Instance deletedInstance16;
  private User user;
  private long currentTime;

  @Before
  public void init() {
    saveInstancesToDB();
    user = User.Builder.anUser()
               .withName(USER_NAME)
               .withUuid(USER_ID)
               .withAccounts(asList(Account.Builder.anAccount().withUuid(ACCOUNT_1_ID).build(),
                   Account.Builder.anAccount().withUuid(ACCOUNT_2_ID).build()))
               .build();
    user.setUseNewRbac(true);

    Map<String, AppPermissionSummaryForUI> appPermissionsMap = Maps.newHashMap();
    setAppPermissionsMap(appPermissionsMap, ENV_1_ID, APP_1_ID);
    setAppPermissionsMap(appPermissionsMap, ENV_2_ID, APP_2_ID);
    setAppPermissionsMap(appPermissionsMap, ENV_3_ID, APP_2_ID);
    setAppPermissionsMap(appPermissionsMap, ENV_4_ID, APP_3_ID);
    setAppPermissionsMap(appPermissionsMap, ENV_5_ID, APP_4_ID);
    setAppPermissionsMap(appPermissionsMap, ENV_6_ID, APP_5_ID);

    UserPermissionInfo userPermissionInfo = UserPermissionInfo.builder()
                                                .accountId(ACCOUNT_ID)
                                                .isRbacEnabled(true)
                                                .appPermissionMap(appPermissionsMap)
                                                .build();

    user.setUserRequestContext(UserRequestContext.builder()
                                   .accountId(ACCOUNT_ID)
                                   .userPermissionInfo(userPermissionInfo)
                                   .appIdFilterRequired(true)
                                   .appIds(Sets.newHashSet(APP_1_ID, APP_2_ID, APP_3_ID, APP_4_ID, APP_5_ID))
                                   .build());
    UserThreadLocal.set(user);
  }

  @After
  public void teardown() {
    UserThreadLocal.unset();
  }

  private void setAppPermissionsMap(
      Map<String, AppPermissionSummaryForUI> appPermissionsMap, String envId, String appId) {
    Map<String, Set<Action>> envPermissionMap = Maps.newHashMap();
    envPermissionMap.put(envId, newHashSet(Action.READ));

    AppPermissionSummaryForUI appPermissionSummaryForUI =
        AppPermissionSummaryForUI.builder().envPermissions(envPermissionMap).build();

    appPermissionsMap.put(appId, appPermissionSummaryForUI);
  }

  private void saveInstancesToDB() {
    currentTime = System.currentTimeMillis();
    instance1 = buildInstance(
        INSTANCE_1_ID, ACCOUNT_1_ID, APP_1_ID, SERVICE_1_ID, ENV_1_ID, INFRA_MAPPING_1_ID, CONTAINER_1_ID, currentTime);
    wingsPersistence.save(instance1);
    instance2 = buildInstance(
        INSTANCE_2_ID, ACCOUNT_1_ID, APP_1_ID, SERVICE_2_ID, ENV_1_ID, INFRA_MAPPING_2_ID, CONTAINER_2_ID, currentTime);
    wingsPersistence.save(instance2);
    instance3 = buildInstance(INSTANCE_3_ID, ACCOUNT_1_ID, APP_2_ID, SERVICE_3_ID, ENV_2_ID, INFRA_MAPPING_3_ID,
        CONTAINER_3_ID, currentTime - 10000);
    wingsPersistence.save(instance3);
    instance4 = buildInstance(INSTANCE_4_ID, ACCOUNT_1_ID, APP_2_ID, SERVICE_4_ID, ENV_3_ID, INFRA_MAPPING_4_ID,
        CONTAINER_4_ID, currentTime - 20000);
    wingsPersistence.save(instance4);
    instance5 = buildInstance(INSTANCE_5_ID, ACCOUNT_1_ID, APP_3_ID, SERVICE_5_ID, ENV_4_ID, INFRA_MAPPING_5_ID,
        CONTAINER_5_ID, currentTime - 30000);
    wingsPersistence.save(instance5);
    instance6 = buildInstance(INSTANCE_6_ID, ACCOUNT_1_ID, APP_3_ID, SERVICE_5_ID, ENV_4_ID, INFRA_MAPPING_6_ID,
        CONTAINER_6_ID, currentTime - 30000);
    wingsPersistence.save(instance6);
    instance7 = buildInstance(INSTANCE_7_ID, ACCOUNT_2_ID, APP_4_ID, SERVICE_6_ID, ENV_5_ID, INFRA_MAPPING_7_ID,
        CONTAINER_7_ID, currentTime - 40000);
    wingsPersistence.save(instance7);
    instance8 = buildInstance(INSTANCE_8_ID, ACCOUNT_2_ID, APP_5_ID, SERVICE_7_ID, ENV_6_ID, INFRA_MAPPING_8_ID,
        CONTAINER_8_ID, currentTime - 50000);
    wingsPersistence.save(instance8);
    deletedInstance9 = buildInstance(INSTANCE_9_ID, ACCOUNT_1_ID, APP_5_ID, SERVICE_7_ID, ENV_6_ID, INFRA_MAPPING_8_ID,
        CONTAINER_8_ID, currentTime - 120000, currentTime - 30000);
    wingsPersistence.save(deletedInstance9);
    deletedInstance10 = buildInstance(INSTANCE_10_ID, ACCOUNT_1_ID, APP_5_ID, SERVICE_7_ID, ENV_6_ID,
        INFRA_MAPPING_8_ID, CONTAINER_8_ID, currentTime - 120000, currentTime - 60000);
    wingsPersistence.save(deletedInstance10);
    deletedInstance11 = buildInstance(INSTANCE_11_ID, ACCOUNT_1_ID, APP_5_ID, SERVICE_7_ID, ENV_6_ID,
        INFRA_MAPPING_8_ID, CONTAINER_8_ID, currentTime - 30000, currentTime - 20000);
    wingsPersistence.save(deletedInstance11);
    deletedInstance12 = buildInstance(INSTANCE_12_ID, ACCOUNT_1_ID, APP_5_ID, SERVICE_7_ID, ENV_6_ID,
        INFRA_MAPPING_8_ID, CONTAINER_8_ID, currentTime - 30000, currentTime - 10000);
    wingsPersistence.save(deletedInstance12);
    deletedInstance13 = buildInstance(INSTANCE_9_ID, ACCOUNT_2_ID, APP_5_ID, SERVICE_7_ID, ENV_6_ID, INFRA_MAPPING_8_ID,
        CONTAINER_8_ID, currentTime - 120000, currentTime - 30000);
    wingsPersistence.save(deletedInstance9);
    deletedInstance14 = buildInstance(INSTANCE_10_ID, ACCOUNT_2_ID, APP_5_ID, SERVICE_7_ID, ENV_6_ID,
        INFRA_MAPPING_8_ID, CONTAINER_8_ID, currentTime - 120000, currentTime - 60000);
    wingsPersistence.save(deletedInstance10);
    deletedInstance15 = buildInstance(INSTANCE_11_ID, ACCOUNT_2_ID, APP_5_ID, SERVICE_7_ID, ENV_6_ID,
        INFRA_MAPPING_8_ID, CONTAINER_8_ID, currentTime - 30000, currentTime - 20000);
    wingsPersistence.save(deletedInstance11);
    deletedInstance16 = buildInstance(INSTANCE_12_ID, ACCOUNT_2_ID, APP_5_ID, SERVICE_7_ID, ENV_6_ID,
        INFRA_MAPPING_8_ID, CONTAINER_8_ID, currentTime - 30000, currentTime - 10000);
    wingsPersistence.save(deletedInstance12);
  }

  private Instance buildInstance(String instanceId, String accountId, String appId, String serviceId, String envId,
      String infraMappingId, String containerId, long createdAt) {
    InstanceBuilder builder = Instance.builder();
    setValues(builder, instanceId, accountId, appId, serviceId, envId, infraMappingId, containerId);
    builder.createdAt(createdAt);
    return builder.build();
  }

  private Instance buildInstance(String instanceId, String accountId, String appId, String serviceId, String envId,
      String infraMappingId, String containerId, long createdAt, long deletedAt) {
    InstanceBuilder builder = Instance.builder();
    setValues(builder, instanceId, accountId, appId, serviceId, envId, infraMappingId, containerId);
    builder.createdAt(createdAt);
    builder.isDeleted(true);
    builder.deletedAt(deletedAt);
    return builder.build();
  }

  private void setValues(InstanceBuilder instanceBuilder, String instanceId, String accountId, String appId,
      String serviceId, String envId, String infraMappingId, String containerId) {
    instanceBuilder.uuid(instanceId)
        .accountId(accountId)
        .appId(appId)
        .computeProviderId(COMPUTE_PROVIDER_NAME)
        .appName(APP_NAME)
        .envId(envId)
        .envName(ENV_NAME)
        .serviceId(serviceId)
        .serviceName(SERVICE_NAME)
        .envType(EnvironmentType.PROD)
        .infraMappingId(infraMappingId)
        .infraMappingType(InfrastructureMappingType.GCP_KUBERNETES.getName())
        .instanceType(InstanceType.KUBERNETES_CONTAINER_INSTANCE)
        .containerInstanceKey(ContainerInstanceKey.builder().containerId(containerId).build())
        .instanceInfo(KubernetesContainerInfo.builder()
                          .clusterName(KUBE_CLUSTER)
                          .serviceName("service_a_0")
                          .controllerName("controllerName:0")
                          .podName(containerId)
                          .build())
        .build();
  }

  @Test
  @RealMongo
  public void shallTestInstanceStats() {
    try {
      List<String> appIdList = asList(APP_1_ID, APP_2_ID, APP_3_ID, APP_4_ID, APP_5_ID);
      List<InstanceStatsByService> currentAppInstanceStatsByService =
          dashboardService.getAppInstanceStatsByService(ACCOUNT_1_ID, appIdList, System.currentTimeMillis());
      assertEquals(7, currentAppInstanceStatsByService.size());
      List<InstanceStatsByService> appInstanceStatsByServiceAtTime =
          dashboardService.getAppInstanceStatsByService(ACCOUNT_1_ID, appIdList, System.currentTimeMillis());
      assertEquals(7, appInstanceStatsByServiceAtTime.size());
      List<Instance> currentInstances =
          dashboardService.getAppInstancesForAccount(ACCOUNT_1_ID, System.currentTimeMillis());
      assertEquals(6, currentInstances.size());

      List<Instance> instancesAtTime = dashboardService.getAppInstancesForAccount(ACCOUNT_1_ID, currentTime - 60000);
      assertEquals(2, instancesAtTime.size());

      appInstanceStatsByServiceAtTime =
          dashboardService.getAppInstanceStatsByService(ACCOUNT_1_ID, appIdList, currentTime - 60000);
      assertEquals(1, appInstanceStatsByServiceAtTime.size());
      assertEquals(2, appInstanceStatsByServiceAtTime.get(0).getTotalCount());

      instancesAtTime = dashboardService.getAppInstancesForAccount(ACCOUNT_1_ID, currentTime - 50000);
      assertEquals(1, instancesAtTime.size());

      appInstanceStatsByServiceAtTime =
          dashboardService.getAppInstanceStatsByService(ACCOUNT_1_ID, appIdList, currentTime - 50000);
      assertEquals(1, appInstanceStatsByServiceAtTime.size());
      assertEquals(2, appInstanceStatsByServiceAtTime.get(0).getTotalCount());

      instancesAtTime = dashboardService.getAppInstancesForAccount(ACCOUNT_1_ID, currentTime - 40000);
      assertEquals(1, instancesAtTime.size());

      appInstanceStatsByServiceAtTime =
          dashboardService.getAppInstanceStatsByService(ACCOUNT_1_ID, appIdList, currentTime - 40000);
      assertEquals(2, appInstanceStatsByServiceAtTime.size());
      assertEquals(1, appInstanceStatsByServiceAtTime.get(0).getTotalCount());
      assertEquals(2, appInstanceStatsByServiceAtTime.get(1).getTotalCount());

      instancesAtTime = dashboardService.getAppInstancesForAccount(ACCOUNT_1_ID, currentTime - 30000);
      assertEquals(5, instancesAtTime.size());

      instancesAtTime = dashboardService.getAppInstancesForAccount(ACCOUNT_1_ID, currentTime - 20000);
      assertEquals(5, instancesAtTime.size());

      instancesAtTime = dashboardService.getAppInstancesForAccount(ACCOUNT_1_ID, currentTime - 10000);
      assertEquals(5, instancesAtTime.size());

      instancesAtTime = dashboardService.getAppInstancesForAccount(ACCOUNT_1_ID, currentTime);
      assertEquals(6, instancesAtTime.size());

      instancesAtTime = dashboardService.getAppInstancesForAccount(ACCOUNT_2_ID, currentTime - 30000);
      assertEquals(2, instancesAtTime.size());
    } finally {
      UserThreadLocal.unset();
    }
  }

  @Test
  @RealMongo
  public void shallGetServiceInstanceDashboard() {
    try {
      user.setUserRequestContext(UserRequestContext.builder()
                                     .accountId(ACCOUNT_ID)
                                     .userPermissionInfo(user.getUserRequestContext().getUserPermissionInfo())
                                     .appIdFilterRequired(true)
                                     .appIds(Sets.newHashSet(APP_1_ID))
                                     .build());

      PipelineSummary pipelineSummary =
          PipelineSummary.builder().pipelineId(PIPELINE_EXECUTION_ID).pipelineName(PIPELINE_NAME).build();

      ExecutionArgs executionArgs = new ExecutionArgs();
      executionArgs.setArtifacts(asList(Artifact.Builder.anArtifact()
                                            .withAppId(APP_1_ID)
                                            .withServiceIds(asList(SERVICE_1_ID))
                                            .withDisplayName(ARTIFACT_NAME)
                                            .withUuid(ARTIFACT_ID)
                                            .withArtifactSourceName(ARTIFACT_SOURCE_NAME)
                                            .build()));

      WorkflowExecution workflowExecution = aWorkflowExecution()
                                                .withPipelineSummary(pipelineSummary)
                                                .withExecutionArgs(executionArgs)
                                                .withAppId(APP_1_ID)
                                                .withStatus(ExecutionStatus.ERROR)
                                                .withEnvIds(asList(ENV_1_ID))
                                                .withServiceIds(asList(SERVICE_1_ID, SERVICE_2_ID))
                                                .withInfraMappingIds(asList(INFRA_MAPPING_1_ID, INFRA_MAPPING_2_ID))
                                                .withWorkflowId(WORKFLOW_ID)
                                                .withUuid(WORKFLOW_EXECUTION_ID)
                                                .withName(WORKFLOW_NAME)
                                                .build();
      PageResponse<WorkflowExecution> executionsPageResponse =
          aPageResponse().withResponse(asList(workflowExecution)).build();
      when(workflowExecutionService.listExecutions(any(PageRequest.class), anyBoolean()))
          .thenReturn(executionsPageResponse);

      List<Service> serviceList = Lists.newArrayList();
      Service service1 = Service.builder().uuid(SERVICE_1_ID).name(SERVICE_NAME).appId(APP_1_ID).build();
      serviceList.add(service1);
      Service service2 = Service.builder().uuid(SERVICE_2_ID).name(SERVICE_NAME).appId(APP_1_ID).build();
      serviceList.add(service2);
      PageResponse<Environment> servicesPageResponse = aPageResponse().withResponse(serviceList).build();
      when(serviceResourceService.list(any(PageRequest.class), anyBoolean(), anyBoolean()))
          .thenReturn(servicesPageResponse);
      when(serviceResourceService.get(anyString(), anyString())).thenReturn(service1);

      List<Environment> envList = Lists.newArrayList();
      envList.add(
          Environment.Builder.anEnvironment().withUuid(ENV_1_ID).withName(ENV_NAME).withAppId(APP_1_ID).build());
      PageResponse<Environment> envsPageResponse = aPageResponse().withResponse(envList).build();
      when(environmentService.list(any(PageRequest.class), anyBoolean())).thenReturn(envsPageResponse);

      List<InfrastructureMapping> infraList = Lists.newArrayList();
      InfrastructureMapping infra1 = new GcpKubernetesInfrastructureMapping();
      infra1.setEnvId(ENV_1_ID);
      infra1.setServiceId(SERVICE_1_ID);
      infra1.setAccountId(ACCOUNT_1_ID);
      infra1.setAppId(APP_1_ID);
      infra1.setUuid(INFRA_MAPPING_1_ID);
      infra1.setName(INFRA_MAPPING_NAME);
      infraList.add(infra1);

      InfrastructureMapping infra2 = new GcpKubernetesInfrastructureMapping();
      infra2.setEnvId(ENV_1_ID);
      infra2.setServiceId(SERVICE_2_ID);
      infra2.setAccountId(ACCOUNT_1_ID);
      infra2.setAppId(APP_1_ID);
      infra2.setUuid(INFRA_MAPPING_2_ID);
      infra2.setName(INFRA_MAPPING_NAME);
      infraList.add(infra2);

      PageResponse<InfrastructureMapping> infrasPageResponse = aPageResponse().withResponse(infraList).build();

      when(infraMappingService.list(any(PageRequest.class))).thenReturn(infrasPageResponse);
      long deployTime = System.currentTimeMillis();
      ArtifactSummary artifactSummary =
          ArtifactSummary.builder().id(ARTIFACT_ID).name(ARTIFACT_NAME).buildNo(BUILD_NO).build();
      EntitySummary env =
          EntitySummary.builder().id(ENV_1_ID).name(ENV_NAME).type(EntityType.ENVIRONMENT.name()).build();
      EntitySummary infraSummary1 = EntitySummary.builder()
                                        .id(INFRA_MAPPING_1_ID)
                                        .name(INFRA_MAPPING_NAME)
                                        .type(EntityType.INFRASTRUCTURE_MAPPING.name())
                                        .build();
      EntitySummary infraSummary2 = EntitySummary.builder()
                                        .id(INFRA_MAPPING_2_ID)
                                        .name(INFRA_MAPPING_NAME)
                                        .type(EntityType.INFRASTRUCTURE_MAPPING.name())
                                        .build();
      EntitySummary workflow = EntitySummary.builder()
                                   .id(WORKFLOW_EXECUTION_ID)
                                   .name(WORKFLOW_NAME)
                                   .type(EntityType.WORKFLOW.name())
                                   .build();
      EntitySummary pipeline = EntitySummary.builder()
                                   .id(PIPELINE_EXECUTION_ID)
                                   .name(PIPELINE_NAME)
                                   .type(EntityType.PIPELINE.name())
                                   .build();
      EntitySummary user = EntitySummary.builder().id(USER_ID).name(USER_NAME).type(EntityType.USER.name()).build();
      DeploymentHistory expectedDeployment = DeploymentHistory.builder()
                                                 .artifact(artifactSummary)
                                                 .deployedAt(new Date(deployTime))
                                                 .envs(asList(env))
                                                 .inframappings(asList(infraSummary1, infraSummary2))
                                                 .instanceCount(1)
                                                 .pipeline(pipeline)
                                                 .workflow(workflow)
                                                 .status("ERROR")
                                                 .triggeredBy(user)
                                                 .build();

      ServiceInstanceDashboard serviceInstanceDashboard =
          dashboardService.getServiceInstanceDashboard(ACCOUNT_1_ID, APP_1_ID, SERVICE_1_ID);
      assertNotNull(serviceInstanceDashboard);
      assertEquals(1, serviceInstanceDashboard.getCurrentActiveInstancesList().size());
      assertEquals(1, serviceInstanceDashboard.getCurrentActiveInstancesList().get(0).getInstanceCount());
      assertEquals(1, serviceInstanceDashboard.getDeploymentHistoryList().size());
      DeploymentHistory deploymentHistory = serviceInstanceDashboard.getDeploymentHistoryList().get(0);
      assertTrue(deepEquals(expectedDeployment.getEnvs(), deploymentHistory.getEnvs()));
      assertTrue(deepEquals(expectedDeployment.getInframappings(), deploymentHistory.getInframappings()));
      assertTrue(deepEquals(expectedDeployment.getStatus(), deploymentHistory.getStatus()));
      assertTrue(deepEquals(expectedDeployment.getWorkflow(), deploymentHistory.getWorkflow()));
    } finally {
      UserThreadLocal.unset();
    }
  }
}
