package software.wings.service.impl.security.auth;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.ALL_APP_ENTITIES;
import static software.wings.security.PermissionAttribute.PermissionType.APPLICATION_CREATE_DELETE;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.PermissionType.PIPELINE;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.WORKFLOW;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.beans.Workflow;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.WorkflowFilter;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class AuthHandlerTest extends WingsBaseTest {
  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private InfrastructureProvisionerService infrastructureProvisionerService;
  @Mock private EnvironmentService environmentService;
  @Mock private WorkflowService workflowService;
  @Mock private PipelineService pipelineService;

  @InjectMocks @Inject private AuthHandler authHandler;

  private List<PermissionType> accountPermissionTypes =
      asList(ACCOUNT_MANAGEMENT, APPLICATION_CREATE_DELETE, USER_PERMISSION_MANAGEMENT);
  private List<Action> allActions = asList(Action.CREATE, Action.UPDATE, Action.READ, Action.DELETE, Action.EXECUTE);

  private List<String> appIds = asList(APP_ID, "appId1", "appId2", "appId3");
  private Service service1 = Service.builder().uuid(generateUuid()).appId(APP_ID).build();
  private Service service2 = Service.builder().uuid(generateUuid()).appId(APP_ID).build();

  private InfrastructureProvisioner infrastructureProvisioner =
      TerraformInfrastructureProvisioner.builder().appId(APP_ID).build();

  private Environment dev =
      anEnvironment().withUuid(generateUuid()).withAppId(APP_ID).withEnvironmentType(EnvironmentType.NON_PROD).build();
  private Environment qa =
      anEnvironment().withUuid(generateUuid()).withAppId(APP_ID).withEnvironmentType(EnvironmentType.NON_PROD).build();
  private Environment prod =
      anEnvironment().withUuid(generateUuid()).withAppId(APP_ID).withEnvironmentType(EnvironmentType.PROD).build();
  private Environment dr =
      anEnvironment().withUuid(generateUuid()).withAppId(APP_ID).withEnvironmentType(EnvironmentType.PROD).build();

  private Workflow workflow1 = aWorkflow().withUuid(generateUuid()).withAppId(APP_ID).withEnvId(dev.getUuid()).build();
  private Workflow workflow2 = aWorkflow().withUuid(generateUuid()).withAppId(APP_ID).withEnvId(qa.getUuid()).build();
  private Workflow workflow3 = aWorkflow().withUuid(generateUuid()).withAppId(APP_ID).withEnvId(prod.getUuid()).build();
  private Workflow buildWorkflow = aWorkflow().withUuid(generateUuid()).withAppId(APP_ID).withEnvId(null).build();

  private Pipeline pipeline1 =
      Pipeline.builder()
          .uuid(generateUuid())
          .appId(APP_ID)
          .pipelineStages(
              asList(PipelineStage.builder()
                         .pipelineStageElements(asList(PipelineStageElement.builder()
                                                           .properties(ImmutableMap.of("envId", dev.getUuid()))
                                                           .type(ENV_STATE.name())
                                                           .build()))
                         .build(),
                  PipelineStage.builder()
                      .pipelineStageElements(asList(PipelineStageElement.builder()
                                                        .properties(ImmutableMap.of("envId", prod.getUuid()))
                                                        .type(ENV_STATE.name())
                                                        .build()))
                      .build()))
          .build();

  private Pipeline pipeline2 =
      Pipeline.builder()
          .uuid(generateUuid())
          .appId(APP_ID)
          .pipelineStages(
              asList(PipelineStage.builder()
                         .pipelineStageElements(asList(PipelineStageElement.builder()
                                                           .properties(ImmutableMap.of("envId", prod.getUuid()))
                                                           .type(ENV_STATE.name())
                                                           .build()))
                         .build()))
          .build();

  private Pipeline approvalPipeline =
      Pipeline.builder()
          .uuid(generateUuid())
          .appId(APP_ID)
          .pipelineStages(
              asList(PipelineStage.builder()
                         .pipelineStageElements(asList(PipelineStageElement.builder().type(APPROVAL.name()).build()))
                         .build()))
          .build();

  private Map<String, Object> getEmptyEnvMap() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("envId", null);
    return map;
  }

  private Pipeline buildPipeline =
      Pipeline.builder()
          .uuid(generateUuid())
          .appId(APP_ID)
          .pipelineStages(asList(
              PipelineStage.builder()
                  .pipelineStageElements(asList(PipelineStageElement.builder().type(APPROVAL.name()).build(),
                      PipelineStageElement.builder().properties(getEmptyEnvMap()).type(ENV_STATE.name()).build()))
                  .build()))
          .build();

  private AccountPermissions accountPermissions =
      AccountPermissions.builder().permissions(new HashSet(accountPermissionTypes)).build();
  private AppPermission allAppPermission =
      AppPermission.builder()
          .permissionType(ALL_APP_ENTITIES)
          .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
          .actions(new HashSet(allActions))
          .build();

  @Test
  public void shouldWorkForAccountAdministratorFirstTime() {
    when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(asList());
    List<UserGroup> userGroups = asList(UserGroup.builder()
                                            .accountId(ACCOUNT_ID)
                                            .accountPermissions(accountPermissions)
                                            .appPermissions(new HashSet(asList(allAppPermission)))
                                            .build());
    UserPermissionInfo userPermissionInfo = authHandler.getUserPermissionInfo(ACCOUNT_ID, userGroups);

    assertThat(userPermissionInfo).isNotNull().hasFieldOrPropertyWithValue("accountId", ACCOUNT_ID);
    AccountPermissionSummary accountPermissionSummary = userPermissionInfo.getAccountPermissionSummary();
    assertThat(accountPermissionSummary).isNotNull();
    assertThat(accountPermissionSummary.getPermissions())
        .isNotNull()
        .hasSize(3)
        .hasSameElementsAs(accountPermissionTypes);

    assertThat(isEmpty(userPermissionInfo.getAppPermissionMap()));
  }

  @Test
  public void shouldFetchPermissionsForAccountAdministrator() {
    setupForAllApp();
    List<UserGroup> userGroups = asList(UserGroup.builder()
                                            .accountId(ACCOUNT_ID)
                                            .accountPermissions(accountPermissions)
                                            .appPermissions(new HashSet(asList(allAppPermission)))
                                            .build());
    UserPermissionInfo userPermissionInfo = authHandler.getUserPermissionInfo(ACCOUNT_ID, userGroups);

    assertThat(userPermissionInfo).isNotNull().hasFieldOrPropertyWithValue("accountId", ACCOUNT_ID);
    AccountPermissionSummary accountPermissionSummary = userPermissionInfo.getAccountPermissionSummary();
    assertThat(accountPermissionSummary).isNotNull();
    assertThat(accountPermissionSummary.getPermissions())
        .isNotNull()
        .hasSize(3)
        .hasSameElementsAs(accountPermissionTypes);

    verifyAllAppPermissions(userPermissionInfo);
  }

  private void setupForAllApp() {
    when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(appIds);

    PageResponse<Service> svcResponse = aPageResponse().withResponse(asList(service1, service2)).build();
    when(serviceResourceService.list(any(PageRequest.class), eq(false), eq(false))).thenReturn(svcResponse);

    PageResponse<InfrastructureProvisioner> infrastructureProvisionersResponse =
        aPageResponse().withResponse(asList(infrastructureProvisioner)).build();
    when(infrastructureProvisionerService.list(any(PageRequest.class))).thenReturn(infrastructureProvisionersResponse);

    PageResponse<Environment> envResponse = aPageResponse().withResponse(asList(dev, qa, prod, dr)).build();
    when(environmentService.list(any(PageRequest.class), eq(false))).thenReturn(envResponse);

    PageResponse<Workflow> workflowResponse =
        aPageResponse().withResponse(asList(workflow1, workflow2, workflow3)).build();
    when(workflowService.listWorkflowsWithoutOrchestration(any(PageRequest.class))).thenReturn(workflowResponse);

    PageResponse<Pipeline> pipelineResponse = aPageResponse().withResponse(asList(pipeline1, pipeline2)).build();
    when(pipelineService.listPipelines(any(PageRequest.class))).thenReturn(pipelineResponse);
  }

  private void setupForNoEnvs() {
    setupForOneEnv(null);
  }

  private void setupForOneEnv(Environment env) {
    when(appService.getAppIdsByAccountId(ACCOUNT_ID)).thenReturn(appIds);
    PageResponse<Service> svcResponse = aPageResponse().withResponse(asList(service1, service2)).build();
    when(serviceResourceService.list(any(PageRequest.class), eq(false), eq(false))).thenReturn(svcResponse);

    PageResponse<Environment> envResponse;
    if (env == null) {
      envResponse = aPageResponse().withResponse(asList()).build();
    } else {
      envResponse = aPageResponse().withResponse(asList(env)).build();
    }
    when(environmentService.list(any(PageRequest.class), eq(false))).thenReturn(envResponse);

    PageResponse<Workflow> workflowResponse =
        aPageResponse().withResponse(asList(workflow1, workflow2, workflow3, buildWorkflow)).build();
    when(workflowService.listWorkflowsWithoutOrchestration(any(PageRequest.class))).thenReturn(workflowResponse);

    PageResponse<Pipeline> pipelineResponse =
        aPageResponse().withResponse(asList(pipeline1, pipeline2, buildPipeline, approvalPipeline)).build();
    when(pipelineService.listPipelines(any(PageRequest.class))).thenReturn(pipelineResponse);
  }

  private void setupForDevEnv() {
    setupForOneEnv(dev);
  }

  private void verifyAllAppPermissions(UserPermissionInfo userPermissionInfo) {
    assertThat(userPermissionInfo.getAppPermissionMap())
        .isNotNull()
        .hasSize(4)
        .containsOnlyKeys(APP_ID, "appId1", "appId2", "appId3");
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID)).isNotNull();
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID))
        .extracting("canCreateService", "canCreateEnvironment", "canCreateWorkflow", "canCreatePipeline")
        .contains(true, true, true, true);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getServicePermissions())
        .isNotNull()
        .containsOnlyKeys(service1.getUuid(), service2.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getServicePermissions().get(service1.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getProvisionerPermissions())
        .isNotNull()
        .containsOnlyKeys(infrastructureProvisioner.getUuid());

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getProvisionerPermissions().get(
                   infrastructureProvisioner.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getEnvPermissions())
        .isNotNull()
        .containsOnlyKeys(dev.getUuid(), qa.getUuid(), prod.getUuid(), dr.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getEnvPermissions().get(dev.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getEnvPermissions().get(qa.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getEnvPermissions().get(prod.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getEnvPermissions().get(dr.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getWorkflowPermissions())
        .isNotNull()
        .containsOnlyKeys(workflow1.getUuid(), workflow2.getUuid(), workflow3.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getWorkflowPermissions().get(workflow1.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getWorkflowPermissions().get(workflow2.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getWorkflowPermissions().get(workflow3.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions())
        .isNotNull()
        .containsOnlyKeys(pipeline1.getUuid(), pipeline2.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions().get(pipeline1.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions().get(pipeline2.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions())
        .isNotNull()
        .containsOnlyKeys(
            workflow1.getUuid(), workflow2.getUuid(), workflow3.getUuid(), pipeline1.getUuid(), pipeline2.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions().get(workflow1.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions().get(workflow2.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions().get(workflow3.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions().get(pipeline1.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions().get(pipeline2.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
  }

  @Test
  public void shouldFetchPermissionsForAppAdministrator() {
    setupForAllApp();
    List<UserGroup> userGroups =
        asList(UserGroup.builder().accountId(ACCOUNT_ID).appPermissions(new HashSet(asList(allAppPermission))).build());
    UserPermissionInfo userPermissionInfo = authHandler.getUserPermissionInfo(ACCOUNT_ID, userGroups);

    assertThat(userPermissionInfo).isNotNull().hasFieldOrPropertyWithValue("accountId", ACCOUNT_ID);
    AccountPermissionSummary accountPermissionSummary = userPermissionInfo.getAccountPermissionSummary();
    assertThat(accountPermissionSummary.getPermissions()).isEmpty();
    verifyAllAppPermissions(userPermissionInfo);
  }

  @Test
  public void shouldFetchPermissionsForProdSupport() {
    setupForAllApp();
    EnvFilter envFilter = new EnvFilter();
    envFilter.setFilterTypes(Sets.newHashSet(PROD));

    WorkflowFilter workflowFilter = new WorkflowFilter();
    workflowFilter.setFilterTypes(Sets.newHashSet(PROD));

    AppPermission envPermission = AppPermission.builder()
                                      .permissionType(ENV)
                                      .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                      .entityFilter(envFilter)
                                      .actions(new HashSet(allActions))
                                      .build();

    AppPermission workflowPermission = AppPermission.builder()
                                           .permissionType(WORKFLOW)
                                           .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                           .entityFilter(workflowFilter)
                                           .actions(new HashSet(allActions))
                                           .build();

    AppPermission pipelinePermission = AppPermission.builder()
                                           .permissionType(PIPELINE)
                                           .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                           .entityFilter(envFilter)
                                           .actions(new HashSet(allActions))
                                           .build();

    AppPermission deploymentPermission =
        AppPermission.builder()
            .permissionType(DEPLOYMENT)
            .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    List<UserGroup> userGroups =
        asList(UserGroup.builder()
                   .accountId(ACCOUNT_ID)
                   .appPermissions(
                       new HashSet(asList(envPermission, workflowPermission, pipelinePermission, deploymentPermission)))
                   .build());
    UserPermissionInfo userPermissionInfo = authHandler.getUserPermissionInfo(ACCOUNT_ID, userGroups);

    assertThat(userPermissionInfo).isNotNull().hasFieldOrPropertyWithValue("accountId", ACCOUNT_ID);
    AccountPermissionSummary accountPermissionSummary = userPermissionInfo.getAccountPermissionSummary();
    assertThat(accountPermissionSummary.getPermissions()).isEmpty();

    assertThat(userPermissionInfo.getAppPermissionMap())
        .isNotNull()
        .hasSize(4)
        .containsOnlyKeys(APP_ID, "appId1", "appId2", "appId3");
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID)).isNotNull();
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID))
        .extracting("canCreateService", "canCreateEnvironment", "canCreateWorkflow", "canCreatePipeline")
        .contains(false, false, false, false);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getServicePermissions()).isNull();

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getEnvPermissions())
        .isNotNull()
        .containsOnlyKeys(prod.getUuid(), dr.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getEnvPermissions().get(prod.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getEnvPermissions().get(dr.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getWorkflowPermissions())
        .isNotNull()
        .containsOnlyKeys(workflow3.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getWorkflowPermissions().get(workflow3.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions())
        .isNotNull()
        .containsOnlyKeys(pipeline2.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions().get(pipeline2.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions())
        .isNotNull()
        .containsOnlyKeys(workflow3.getUuid(), pipeline2.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions().get(workflow3.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions().get(pipeline2.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
  }

  @Test
  public void shouldFetchPermissionsForOneEnvOnly() {
    setupForAllApp();
    EnvFilter envFilter = new EnvFilter();
    envFilter.setFilterTypes(Sets.newHashSet(EnvFilter.FilterType.SELECTED));
    envFilter.setIds(Sets.newHashSet(dev.getUuid()));

    WorkflowFilter workflowFilter = new WorkflowFilter();
    workflowFilter.setFilterTypes(Sets.newHashSet(NON_PROD));

    AppPermission envPermission =
        AppPermission.builder()
            .permissionType(ENV)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission workflowPermission =
        AppPermission.builder()
            .permissionType(WORKFLOW)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(workflowFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission pipelinePermission =
        AppPermission.builder()
            .permissionType(PIPELINE)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission deploymentPermission =
        AppPermission.builder()
            .permissionType(DEPLOYMENT)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    List<UserGroup> userGroups =
        asList(UserGroup.builder()
                   .accountId(ACCOUNT_ID)
                   .appPermissions(
                       new HashSet(asList(envPermission, workflowPermission, pipelinePermission, deploymentPermission)))
                   .build());
    UserPermissionInfo userPermissionInfo = authHandler.getUserPermissionInfo(ACCOUNT_ID, userGroups);

    assertThat(userPermissionInfo).isNotNull().hasFieldOrPropertyWithValue("accountId", ACCOUNT_ID);
    AccountPermissionSummary accountPermissionSummary = userPermissionInfo.getAccountPermissionSummary();
    assertThat(accountPermissionSummary.getPermissions()).isEmpty();

    assertThat(userPermissionInfo.getAppPermissionMap()).isNotNull().hasSize(1).containsOnlyKeys(APP_ID);
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID)).isNotNull();
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID))
        .extracting("canCreateService", "canCreateEnvironment", "canCreateWorkflow", "canCreatePipeline")
        .contains(false, false, false, false);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getServicePermissions()).isNull();

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getEnvPermissions())
        .isNotNull()
        .containsOnlyKeys(dev.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getEnvPermissions().get(dev.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getWorkflowPermissions())
        .isNotNull()
        .containsOnlyKeys(workflow1.getUuid(), workflow2.getUuid());

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions()).isNull();

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions())
        .isNotNull()
        .containsOnlyKeys(workflow1.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions().get(workflow1.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
  }

  @Test
  public void shouldFetchPermissionsForMultiplePermissionsInUserGroup() {
    setupForAllApp();
    EnvFilter envFilter = new EnvFilter();
    envFilter.setFilterTypes(Sets.newHashSet(EnvFilter.FilterType.SELECTED));
    envFilter.setIds(Sets.newHashSet(dev.getUuid()));

    WorkflowFilter workflowFilter = new WorkflowFilter();
    workflowFilter.setFilterTypes(Sets.newHashSet(NON_PROD));

    AppPermission envPermission =
        AppPermission.builder()
            .permissionType(ENV)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission workflowPermission =
        AppPermission.builder()
            .permissionType(WORKFLOW)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(workflowFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission pipelinePermission =
        AppPermission.builder()
            .permissionType(PIPELINE)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission deploymentPermission =
        AppPermission.builder()
            .permissionType(DEPLOYMENT)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    WorkflowFilter workflowFilter1 = new WorkflowFilter();
    workflowFilter1.setFilterTypes(Sets.newHashSet(NON_PROD, PROD));

    AppPermission workflowPermission1 = AppPermission.builder()
                                            .permissionType(WORKFLOW)
                                            .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                            .entityFilter(workflowFilter1)
                                            .actions(Sets.newHashSet(Action.READ))
                                            .build();

    List<UserGroup> userGroups = asList(UserGroup.builder()
                                            .accountId(ACCOUNT_ID)
                                            .appPermissions(new HashSet(asList(envPermission, workflowPermission,
                                                workflowPermission1, pipelinePermission, deploymentPermission)))
                                            .build());
    UserPermissionInfo userPermissionInfo = authHandler.getUserPermissionInfo(ACCOUNT_ID, userGroups);

    assertThat(userPermissionInfo).isNotNull().hasFieldOrPropertyWithValue("accountId", ACCOUNT_ID);
    AccountPermissionSummary accountPermissionSummary = userPermissionInfo.getAccountPermissionSummary();
    assertThat(accountPermissionSummary.getPermissions()).isEmpty();

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID)).isNotNull();
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID))
        .extracting("canCreateService", "canCreateEnvironment", "canCreateWorkflow", "canCreatePipeline")
        .contains(false, false, false, false);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getServicePermissions()).isNull();

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getEnvPermissions())
        .isNotNull()
        .containsOnlyKeys(dev.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getEnvPermissions().get(dev.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getWorkflowPermissions())
        .isNotNull()
        .containsOnlyKeys(workflow1.getUuid(), workflow2.getUuid(), workflow3.getUuid());

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getWorkflowPermissions().get(workflow1.getUuid()))
        .isNotNull()
        .containsExactlyInAnyOrder(Action.UPDATE, Action.READ, Action.EXECUTE, Action.DELETE);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions()).isNull();

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions())
        .isNotNull()
        .containsOnlyKeys(workflow1.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions().get(workflow1.getUuid()))
        .isNotNull()
        .contains(Action.UPDATE, Action.READ, Action.DELETE);
  }

  @Test
  public void shouldFetchPermissionsForNoEnv() {
    setupForNoEnvs();
    EnvFilter envFilter = new EnvFilter();
    envFilter.setFilterTypes(Sets.newHashSet(PROD, NON_PROD));

    WorkflowFilter workflowFilter = new WorkflowFilter();
    workflowFilter.setFilterTypes(Sets.newHashSet(PROD, NON_PROD));

    AppPermission envPermission =
        AppPermission.builder()
            .permissionType(ENV)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission workflowPermission =
        AppPermission.builder()
            .permissionType(WORKFLOW)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(workflowFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission pipelinePermission =
        AppPermission.builder()
            .permissionType(PIPELINE)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission deploymentPermission =
        AppPermission.builder()
            .permissionType(DEPLOYMENT)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    List<UserGroup> userGroups =
        asList(UserGroup.builder()
                   .accountId(ACCOUNT_ID)
                   .appPermissions(
                       new HashSet(asList(envPermission, workflowPermission, pipelinePermission, deploymentPermission)))
                   .build());
    UserPermissionInfo userPermissionInfo = authHandler.getUserPermissionInfo(ACCOUNT_ID, userGroups);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getWorkflowPermissions())
        .isNotNull()
        .containsOnlyKeys(buildWorkflow.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions())
        .isNotNull()
        .containsOnlyKeys(approvalPipeline.getUuid(), buildPipeline.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions())
        .isNotNull()
        .containsOnlyKeys(buildWorkflow.getUuid(), approvalPipeline.getUuid(), buildPipeline.getUuid());
  }

  @Test
  public void shouldFetchPermissionsForBuildAndApprovalPipeline() {
    setupForNoEnvs();
    EnvFilter envFilter = new EnvFilter();
    envFilter.setFilterTypes(Sets.newHashSet(PROD));

    WorkflowFilter workflowFilter = new WorkflowFilter();
    workflowFilter.setFilterTypes(Sets.newHashSet(PROD));

    AppPermission envPermission =
        AppPermission.builder()
            .permissionType(ENV)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission workflowPermission =
        AppPermission.builder()
            .permissionType(WORKFLOW)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(workflowFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission pipelinePermission =
        AppPermission.builder()
            .permissionType(PIPELINE)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission deploymentPermission =
        AppPermission.builder()
            .permissionType(DEPLOYMENT)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(envFilter)
            .actions(new HashSet(allActions))
            .build();

    List<UserGroup> userGroups =
        asList(UserGroup.builder()
                   .accountId(ACCOUNT_ID)
                   .appPermissions(
                       new HashSet(asList(envPermission, workflowPermission, pipelinePermission, deploymentPermission)))
                   .build());
    UserPermissionInfo userPermissionInfo = authHandler.getUserPermissionInfo(ACCOUNT_ID, userGroups);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getWorkflowPermissions())
        .isNotNull()
        .containsOnlyKeys(buildWorkflow.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions())
        .isNotNull()
        .containsOnlyKeys(approvalPipeline.getUuid(), buildPipeline.getUuid());
    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions())
        .isNotNull()
        .containsOnlyKeys(buildWorkflow.getUuid(), approvalPipeline.getUuid(), buildPipeline.getUuid());
  }

  @Test
  public void testPermissionsForPipelinesInMultipleEnvsAndMultiplePermissions() {
    setupForAllApp();
    EnvFilter devFilter = new EnvFilter();
    devFilter.setFilterTypes(Sets.newHashSet(EnvFilter.FilterType.SELECTED));
    devFilter.setIds(Sets.newHashSet(dev.getUuid()));

    EnvFilter prodFilter = new EnvFilter();
    prodFilter.setFilterTypes(Sets.newHashSet(EnvFilter.FilterType.PROD));

    WorkflowFilter workflowFilter = new WorkflowFilter();
    workflowFilter.setFilterTypes(Sets.newHashSet(NON_PROD));

    AppPermission devEnvPermission =
        AppPermission.builder()
            .permissionType(ENV)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(devFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission workflowPermission =
        AppPermission.builder()
            .permissionType(WORKFLOW)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(workflowFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission devPipelinePermission =
        AppPermission.builder()
            .permissionType(PIPELINE)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(devFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission prodPipelinePermission =
        AppPermission.builder()
            .permissionType(PIPELINE)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(prodFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission devDeploymentPermission =
        AppPermission.builder()
            .permissionType(DEPLOYMENT)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(devFilter)
            .actions(new HashSet(allActions))
            .build();

    AppPermission prodDeploymentPermission =
        AppPermission.builder()
            .permissionType(DEPLOYMENT)
            .appFilter(
                GenericEntityFilter.builder().filterType(FilterType.SELECTED).ids(Sets.newHashSet(APP_ID)).build())
            .entityFilter(prodFilter)
            .actions(new HashSet(allActions))
            .build();

    WorkflowFilter workflowFilter1 = new WorkflowFilter();
    workflowFilter1.setFilterTypes(Sets.newHashSet(NON_PROD, PROD));

    AppPermission workflowPermission1 = AppPermission.builder()
                                            .permissionType(WORKFLOW)
                                            .appFilter(GenericEntityFilter.builder().filterType(FilterType.ALL).build())
                                            .entityFilter(workflowFilter1)
                                            .actions(Sets.newHashSet(Action.READ))
                                            .build();

    UserGroup devUserGroup = UserGroup.builder()
                                 .accountId(ACCOUNT_ID)
                                 .appPermissions(new HashSet(asList(devEnvPermission, workflowPermission,
                                     workflowPermission1, devPipelinePermission, devDeploymentPermission)))
                                 .build();
    UserGroup prodUserGroup = UserGroup.builder()
                                  .accountId(ACCOUNT_ID)
                                  .appPermissions(new HashSet(asList(devEnvPermission, workflowPermission,
                                      workflowPermission1, prodPipelinePermission, prodDeploymentPermission)))
                                  .build();

    UserGroup devAndProdUserGroup =
        UserGroup.builder()
            .accountId(ACCOUNT_ID)
            .appPermissions(new HashSet(asList(devEnvPermission, workflowPermission, workflowPermission1,
                devPipelinePermission, prodPipelinePermission, devDeploymentPermission, prodDeploymentPermission)))
            .build();

    // Scenario 1
    List<UserGroup> userGroups = asList(devUserGroup, prodUserGroup);

    UserPermissionInfo userPermissionInfo = authHandler.getUserPermissionInfo(ACCOUNT_ID, userGroups);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions())
        .isNotNull()
        .containsOnlyKeys(pipeline1.getUuid(), pipeline2.getUuid());

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions())
        .isNotNull()
        .containsKeys(pipeline1.getUuid(), pipeline2.getUuid());

    // Scenario 2
    userGroups = asList(prodUserGroup);

    userPermissionInfo = authHandler.getUserPermissionInfo(ACCOUNT_ID, userGroups);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions())
        .isNotNull()
        .containsOnlyKeys(pipeline2.getUuid());

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions())
        .isNotNull()
        .containsKeys(pipeline2.getUuid())
        .doesNotContainKeys(pipeline1.getUuid());

    // Scenario 3
    userGroups = asList(devAndProdUserGroup);

    userPermissionInfo = authHandler.getUserPermissionInfo(ACCOUNT_ID, userGroups);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions())
        .isNotNull()
        .containsOnlyKeys(pipeline1.getUuid(), pipeline2.getUuid());

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions())
        .isNotNull()
        .containsKeys(pipeline1.getUuid(), pipeline2.getUuid());

    // Scenario 4
    userGroups = asList(devUserGroup);

    userPermissionInfo = authHandler.getUserPermissionInfo(ACCOUNT_ID, userGroups);

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getPipelinePermissions()).isNull();

    assertThat(userPermissionInfo.getAppPermissionMap().get(APP_ID).getDeploymentPermissions())
        .doesNotContainKeys(pipeline1.getUuid(), pipeline2.getUuid());
  }

  @Test
  public void shouldGetAllEntities_multiplePages() {
    // Scenario page size = 10, total = 11
    int total = 11;
    int pageSize = 10;
    String pageSizeStr = "10";

    PageRequest<Environment> pageRequest1 =
        PageRequestBuilder.aPageRequest().withLimit(pageSizeStr).withOffset("0").build();
    List<Environment> expectedEnv = createEnvs("dev", 0, total);

    PageResponse<Environment> firstPage =
        aPageResponse().withResponse(createEnvs("dev", 0, pageSize)).withTotal(total).build();
    PageResponse<Environment> lastPage =
        aPageResponse().withResponse(createEnvs("dev", pageSize, total)).withTotal(total).build();

    when(environmentService.list(any(PageRequest.class), eq(false)))
        .thenAnswer((Answer<PageResponse<Environment>>) invocation -> {
          Object[] arguments = invocation.getArguments();
          PageRequest pageRequest = (PageRequest) arguments[0];
          if (pageRequest.getOffset().equals("0")) {
            return firstPage;
          } else if (pageRequest.getOffset().equals(pageSizeStr)) {
            return lastPage;
          } else {
            return null;
          }
        });

    List<Environment> allEntities =
        authHandler.getAllEntities(pageRequest1, () -> environmentService.list(pageRequest1, false));
    assertEquals(allEntities.size(), total);
    assertThat(allEntities).containsExactlyInAnyOrder(expectedEnv.toArray(new Environment[0]));
  }

  @Test
  public void shouldGetAllEntities_multiplePages_multiplesOfPageSize() {
    // Scenario page size = 10, total = 20
    int total = 20;
    int pageSize = 10;
    String pageSizeStr = "10";
    PageRequest<Environment> pageRequest1 =
        PageRequestBuilder.aPageRequest().withLimit(pageSizeStr).withOffset("0").build();
    List<Environment> expectedEnv = createEnvs("dev", 0, total);

    PageResponse<Environment> firstPage =
        aPageResponse().withResponse(createEnvs("dev", 0, 10)).withTotal(total).build();
    PageResponse<Environment> lastPage =
        aPageResponse().withResponse(createEnvs("dev", pageSize, total)).withTotal(total).build();

    when(environmentService.list(any(PageRequest.class), eq(false)))
        .thenAnswer((Answer<PageResponse<Environment>>) invocation -> {
          Object[] arguments = invocation.getArguments();
          PageRequest pageRequest = (PageRequest) arguments[0];
          if (pageRequest.getOffset().equals("0")) {
            return firstPage;
          } else if (pageRequest.getOffset().equals(pageSizeStr)) {
            return lastPage;
          } else {
            return null;
          }
        });

    List<Environment> allEntities =
        authHandler.getAllEntities(pageRequest1, () -> environmentService.list(pageRequest1, false));
    assertEquals(allEntities.size(), total);
    assertThat(allEntities).containsExactlyInAnyOrder(expectedEnv.toArray(new Environment[0]));
  }

  @Test
  public void shouldGetAllEntities_onePage() {
    // Scenario page size = 10, total = 8
    int total = 8;
    String pageSizeStr = "10";
    PageRequest<Environment> pageRequest1 =
        PageRequestBuilder.aPageRequest().withLimit(pageSizeStr).withOffset("0").build();
    List<Environment> expectedEnv = createEnvs("dev", 0, total);

    PageResponse<Environment> firstPage =
        aPageResponse().withResponse(createEnvs("dev", 0, total)).withTotal(total).build();
    when(environmentService.list(eq(pageRequest1), eq(false))).thenReturn(firstPage);

    List<Environment> allEntities =
        authHandler.getAllEntities(pageRequest1, () -> environmentService.list(pageRequest1, false));
    assertEquals(allEntities.size(), total);
    assertThat(allEntities).containsExactlyInAnyOrder(expectedEnv.toArray(new Environment[0]));
  }

  @Test
  public void shouldGetAllEntities_noResults() {
    // Scenario page size = 10, total = 0
    int total = 0;
    String pageSizeStr = "10";

    PageRequest<Environment> pageRequest1 =
        PageRequestBuilder.aPageRequest().withLimit(pageSizeStr).withOffset("0").build();

    PageResponse<Environment> firstPage = aPageResponse().withResponse(Lists.newArrayList()).withTotal(total).build();
    when(environmentService.list(eq(pageRequest1), eq(false))).thenReturn(firstPage);

    List<Environment> allEntities =
        authHandler.getAllEntities(pageRequest1, () -> environmentService.list(pageRequest1, false));
    assertEquals(allEntities.size(), total);
  }

  private List<Environment> createEnvs(String prefix, int start, int end) {
    List<Environment> envList = Lists.newArrayList();
    for (int i = start; i < end; i++) {
      envList.add(anEnvironment()
                      .withUuid(Integer.toString(i))
                      .withName(prefix + i)
                      .withAppId(APP_ID)
                      .withEnvironmentType(EnvironmentType.NON_PROD)
                      .build());
    }
    return envList;
  }
}
