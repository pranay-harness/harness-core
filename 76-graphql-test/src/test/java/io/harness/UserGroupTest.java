package io.harness;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.ALL_APP_ENTITIES;
import static software.wings.security.PermissionAttribute.PermissionType.APPLICATION_CREATE_DELETE;
import static software.wings.security.PermissionAttribute.PermissionType.AUDIT_VIEWER;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.PermissionType.PIPELINE;
import static software.wings.security.PermissionAttribute.PermissionType.PROVISIONER;
import static software.wings.security.PermissionAttribute.PermissionType.SERVICE;
import static software.wings.security.PermissionAttribute.PermissionType.TAG_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_READ;
import static software.wings.security.PermissionAttribute.PermissionType.WORKFLOW;

import com.google.inject.Inject;

import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.category.layer.GraphQLTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.InfrastructureProvisionerGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Service;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.WorkflowFilter;
import software.wings.service.intfc.UserGroupService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class UserGroupTest extends GraphQLTest {
  @Inject private OwnerManager ownerManager;
  @Inject ApplicationGenerator applicationGenerator;
  @Inject ServiceGenerator serviceGenerator;
  @Inject EnvironmentGenerator environmentGenerator;
  private Application application1, application2;
  private Service service1, service2;
  private Environment environment1, environment2;
  @Inject UserGroupService userGroupService;
  private InfrastructureProvisioner infrastructureProvisioner;
  @Inject InfrastructureProvisionerGenerator infrastructureProvisionerGenerator;
  private String accountId;
  final Randomizer.Seed seed = new Randomizer.Seed(0);

  private UserGroup createUserGroup(AccountPermissions accountPermissions, Set<AppPermission> appPermissions) {
    UserGroup userGroup = UserGroup.builder()
                              .accountId(accountId)
                              .description("Test UserGroup")
                              .name("AccountPermission-UserGroup-" + System.currentTimeMillis())
                              .accountPermissions(accountPermissions)
                              .appPermissions(appPermissions)
                              .build();
    return userGroupService.save(userGroup);
  }

  @Before
  public void setup() {
    // Creating applications
    final OwnerManager.Owners owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).build());
    application1 = applicationGenerator.ensureApplication(
        seed, owners, anApplication().name("Application - " + generateUuid()).build());
    accountId = application1.getAccountId();
    application2 = applicationGenerator.ensureApplication(
        seed, owners, anApplication().name("Application - " + generateUuid()).build());

    // Creating Services
    service1 = serviceGenerator.ensureService(seed, owners,
        Service.builder().name("Service1").appId(application1.getUuid()).uuid(UUIDGenerator.generateUuid()).build());
    service2 = serviceGenerator.ensureService(seed, owners,
        Service.builder().name("Service2").appId(application1.getUuid()).uuid(UUIDGenerator.generateUuid()).build());

    // Creating Environment
    final Environment.Builder builder = anEnvironment().appId(application1.getUuid());
    environment1 =
        environmentGenerator.ensureEnvironment(seed, owners, builder.uuid(generateUuid()).name("prod").build());
    environment2 =
        environmentGenerator.ensureEnvironment(seed, owners, builder.uuid(generateUuid()).name("qa").build());

    infrastructureProvisioner = infrastructureProvisionerGenerator.ensurePredefined(
        seed, owners, InfrastructureProvisionerGenerator.InfrastructureProvisioners.TERRAFORM_TEST);
  }

  private Set<PermissionType> createAccountPermissions() {
    List<PermissionType> permissionTypeList = Arrays.asList(APPLICATION_CREATE_DELETE, USER_PERMISSION_READ,
        USER_PERMISSION_MANAGEMENT, TEMPLATE_MANAGEMENT, ACCOUNT_MANAGEMENT, AUDIT_VIEWER, TAG_MANAGEMENT);
    return new HashSet<>(permissionTypeList);
  }

  private String createAccountPermissionGQL() {
    String accountPermissions =
        $GQL(/*
{
accountPermissions : [
CREATE_AND_DELETE_APPLICATION,
READ_USERS_AND_GROUPS,
MANAGE_USERS_AND_GROUPS,
MANAGE_TEMPLATE_LIBRARY,
ADMINISTER_OTHER_ACCOUNT_FUNCTIONS,
VIEW_AUDITS,
MANAGE_TAGS
]
}*/);
    return accountPermissions;
  }

  private Set<Action> createActionSet() {
    List<Action> actionsList = Arrays.asList(Action.CREATE, Action.READ, Action.DELETE);
    return new HashSet<>(actionsList);
  }

  private String createMutation(String userGroupId, String permissionsVariable) {
    String mutation =
        $GQL(/*
mutation {
    updateUserGroupPermissions(userGroupId: "%s", permissions : %s){
        accountPermissions
    }
}*/ userGroupId, permissionsVariable);
    return mutation;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testAccountPermissions() {
    // Create Permissions
    Set<PermissionType> allPermissions = createAccountPermissions();
    AccountPermissions accountPermissions = AccountPermissions.builder().permissions(allPermissions).build();
    // Add Permission to userGroup
    UserGroup userGroup = createUserGroup(accountPermissions, null);
    String userGroupId = userGroup.getUuid();
    // Add Permission to userGroup using gql
    String accountPermissionsString = createAccountPermissionGQL();
    String mutationAccountPermissions = createMutation(userGroupId, accountPermissionsString);

    {
      // This query will update the userGroup
      qlExecute(mutationAccountPermissions, accountId);
    }
    // Compare this two
    UserGroup updatedUserGroup = userGroupService.get(accountId, userGroupId);
    assertThat(userGroup.getAccountPermissions())
        .isEqualToComparingFieldByField(updatedUserGroup.getAccountPermissions());
    // delete the userGroup
    userGroupService.delete(accountId, userGroupId, true);
  }

  private void testAppPermissions(UserGroup userGroup, String appPermissionString) {
    String userGroupId = userGroup.getUuid();
    String mutationAccountPermissions = createMutation(userGroupId, appPermissionString);

    {
      // This query will update the userGroup
      qlExecute(mutationAccountPermissions, accountId);
    }
    // Compare this two
    UserGroup updatedUserGroup = userGroupService.get(accountId, userGroupId);
    assertThat(Objects.deepEquals(userGroup.getAppPermissions(), updatedUserGroup.getAppPermissions())).isTrue();
    // delete the userGroup
    userGroupService.delete(accountId, userGroupId, true);
  }

  private String createAllEntityAllAppsPermissionGQL() {
    String appPermissionString = $GQL(
        /*
{
       appPermissions :
     		             [
                          {
                             permissionType : ALL,
                              applications:    {
                                                  filterType: ALL
											   },
                              actions:        [
                                                CREATE,DELETE,READ]
              			  		 }
              			  ]
    }*/);

    return appPermissionString;
  }

  private GenericEntityFilter createGenericFilterwithTypeAll() {
    return GenericEntityFilter.builder().filterType("ALL").build();
  }
  private GenericEntityFilter createGenericFilterwithIds(String id1, String id2) {
    Set<String> ids = new HashSet<>(Arrays.asList(id1, id2));
    return GenericEntityFilter.builder().filterType("SELECTED").ids(ids).build();
  }

  private AppPermission createAllEntityAllAppsPermission() {
    Set<Action> actions = createActionSet();
    GenericEntityFilter appFilter = createGenericFilterwithTypeAll();
    return AppPermission.builder().permissionType(ALL_APP_ENTITIES).actions(actions).appFilter(appFilter).build();
  }

  private AppPermission createAllEntitySelectedAppsPermission() {
    GenericEntityFilter appFilter = createGenericFilterwithIds(application1.getUuid(), application2.getUuid());
    return AppPermission.builder()
        .permissionType(ALL_APP_ENTITIES)
        .actions(createActionSet())
        .appFilter(appFilter)
        .build();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testAllEntityAppPermissions() {
    AppPermission allApplications = createAllEntityAllAppsPermission();
    Set<AppPermission> appPermissions = new HashSet<>();
    appPermissions.add(allApplications);
    UserGroup userGroup = createUserGroup(null, appPermissions);
    String userGroupId = userGroup.getUuid();
    // Add Permission to userGroup using gql
    String appPermissionString = createAllEntityAllAppsPermissionGQL();
    testAppPermissions(userGroup, appPermissionString);
  }

  private String getAppIdsFilter() {
    return String.format("appIds: [\"%s\", \"%s\"]", application1.getUuid(), application2.getUuid());
  }

  private String createAllEntitySelectedAppsPermissionGQL() {
    String appPermissionString = $GQL(
        /*
{
       appPermissions :
                            [
                              {
                              permissionType : ALL,
                              applications:    {
                                                  %s
                                               },
                              actions:        [CREATE,DELETE,READ]
                              }
                            ]
    }*/ getAppIdsFilter());

    return appPermissionString;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testPermissionsforSelectedApplications() {
    AppPermission selectedApplications = createAllEntitySelectedAppsPermission();
    Set<AppPermission> appPermissions = new HashSet<>();
    appPermissions.add(selectedApplications);
    UserGroup userGroup = createUserGroup(null, appPermissions);
    String userGroupId = userGroup.getUuid();
    // Add Permission to userGroup using gql
    String appPermissionString = createAllEntitySelectedAppsPermissionGQL();
    testAppPermissions(userGroup, appPermissionString);
  }

  private String createServiceAllAppsPermissionGQL() {
    String appPermissionString = $GQL(
        /*
{
       appPermissions :
                          [
                          {
                             permissionType : SERVICE,
                             applications   : {
                                                 filterType: ALL
                                              },
                              services      :  {
                                                     filterType :ALL
                                               },
                              actions:        [CREATE,DELETE,READ]
                                  }
                                 ]
}*/);

    return appPermissionString;
  }

  private AppPermission createServiceAllAppPermission() {
    Set<Action> actions = createActionSet();
    GenericEntityFilter appFilter = createGenericFilterwithTypeAll();
    GenericEntityFilter serviceFilter = createGenericFilterwithTypeAll();
    return AppPermission.builder()
        .permissionType(SERVICE)
        .entityFilter(serviceFilter)
        .actions(actions)
        .appFilter(appFilter)
        .build();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testServiceAppPermissions() {
    AppPermission appPermission = createServiceAllAppPermission();
    Set<AppPermission> appPermissions = new HashSet<>();
    appPermissions.add(appPermission);
    UserGroup userGroup = createUserGroup(null, appPermissions);
    String userGroupId = userGroup.getUuid();
    // Add Permission to userGroup using gql
    String appPermissionString = createServiceAllAppsPermissionGQL();
    testAppPermissions(userGroup, appPermissionString);
  }

  private String getServiceIds() {
    return String.format("serviceIds: [\"%s\", \"%s\"]", service1.getUuid(), service2.getUuid());
  }

  private String createServiceWithIdPermissionGQL() {
    String appPermissionString = $GQL(
        /*
{
       appPermissions :
                             [
                          {
                             permissionType : SERVICE,
                             applications   : {
                                                 filterType: ALL
                                               },
                             services      :  {
                                                     %s
                                               },
                              actions:        [CREATE,DELETE,READ]
                                  }
                                 ]
}*/ getServiceIds());

    return appPermissionString;
  }

  private AppPermission createServiceWithIdPermission() {
    GenericEntityFilter appFilter = createGenericFilterwithTypeAll();
    GenericEntityFilter serviceFilter = createGenericFilterwithIds(service1.getUuid(), service2.getUuid());
    return AppPermission.builder()
        .permissionType(SERVICE)
        .entityFilter(serviceFilter)
        .actions(createActionSet())
        .appFilter(appFilter)
        .build();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testServiceAppPermissionsWithIds() {
    AppPermission appPermission = createServiceWithIdPermission();
    Set<AppPermission> appPermissions = new HashSet<>();
    appPermissions.add(appPermission);
    UserGroup userGroup = createUserGroup(null, appPermissions);
    String userGroupId = userGroup.getUuid();
    // Add Permission to userGroup using gql
    String appPermissionString = createServiceWithIdPermissionGQL();
    testAppPermissions(userGroup, appPermissionString);
  }

  private String createEnvAllAppsPermissionGQL() {
    String appPermissionString = $GQL(
        /*
{
       appPermissions :
                             [
                          {
                             permissionType : ENV,
                             applications   : {
                                                 filterType: ALL
                                              },
                             environments      :  {
                                                     filterTypes :
[PRODUCTION_ENVIRONMENTS,NON_PRODUCTION_ENVIRONMENTS]
                                               },
                              actions:        [CREATE,DELETE,READ]
                                  }
                                 ]
}*/);

    return appPermissionString;
  }

  private AppPermission createEnvAllAppsPermission() {
    Set<Action> actions = createActionSet();
    GenericEntityFilter appFilter = createGenericFilterwithTypeAll();
    List<String> envList = Arrays.asList("PROD", "NON_PROD");
    Set<String> envFilterTypes = new HashSet<>(envList);
    EnvFilter envFilter = EnvFilter.builder().filterTypes(envFilterTypes).build();
    return AppPermission.builder()
        .permissionType(ENV)
        .entityFilter(envFilter)
        .actions(actions)
        .appFilter(appFilter)
        .build();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testEnvAppPermissions() {
    AppPermission appPermission = createEnvAllAppsPermission();
    Set<AppPermission> appPermissions = new HashSet<>();
    appPermissions.add(appPermission);
    UserGroup userGroup = createUserGroup(null, appPermissions);
    String userGroupId = userGroup.getUuid();
    // Add Permission to userGroup using gql
    String appPermissionString = createEnvAllAppsPermissionGQL();
    testAppPermissions(userGroup, appPermissionString);
  }

  private String getEnvFilter() {
    return String.format("envIds: [\"%s\", \"%s\"]", environment1.getUuid(), environment2.getUuid());
  }

  private String createEnvWithIdsPermissionGQL() {
    String appPermissionString = $GQL(
        /*
{
       appPermissions :
                             [
                          {
                             permissionType : ENV,
                             applications   : {
                                                 %s
                                              },
                             environments      :  {
                                                     %s
                                               },
                              actions:        [CREATE,DELETE,READ]
                                  }
                                 ]
}*/ getAppIdsFilter(), getEnvFilter());

    return appPermissionString;
  }

  private AppPermission createEnvWithIdsPermission() {
    Set<Action> actions = createActionSet();
    GenericEntityFilter appFilter = createGenericFilterwithIds(application1.getUuid(), application2.getUuid());
    List<String> envList = Arrays.asList(environment1.getUuid(), environment2.getUuid());
    Set<String> ids = new HashSet<>(envList);
    EnvFilter envFilter = EnvFilter.builder().ids(ids).filterTypes(new HashSet<>(Arrays.asList("SELECTED"))).build();
    return AppPermission.builder()
        .permissionType(ENV)
        .entityFilter(envFilter)
        .actions(actions)
        .appFilter(appFilter)
        .build();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testEnvWithIdsPermissions() {
    AppPermission appPermission = createEnvWithIdsPermission();
    Set<AppPermission> appPermissions = new HashSet<>();
    appPermissions.add(appPermission);
    UserGroup userGroup = createUserGroup(null, appPermissions);
    String userGroupId = userGroup.getUuid();
    // Add Permission to userGroup using gql
    String appPermissionString = createEnvWithIdsPermissionGQL();
    testAppPermissions(userGroup, appPermissionString);
  }

  private String createWorkflowAllAppsPermissionGQL() {
    String appPermissionString = $GQL(
        /*
{
       appPermissions :
                             [
                          {
                             permissionType : WORKFLOW ,
                             applications   : {
                                                 filterType: ALL
                                              },
                             workflows      :  {
                                                 filterTypes :
[PRODUCTION_WORKFLOWS,NON_PRODUCTION_WORKFLOWS,WORKFLOW_TEMPLATES]
                                               },
                              actions:        [CREATE,DELETE,READ]
                                  }
                                 ]
}*/);

    return appPermissionString;
  }

  private AppPermission createWorkflowAllAppsPermission() {
    Set<Action> actions = createActionSet();
    GenericEntityFilter appFilter = createGenericFilterwithTypeAll();
    List<String> workflowList = Arrays.asList("PROD", "NON_PROD", "TEMPLATES");
    Set<String> workflowFilterTypes = new HashSet<>(workflowList);
    WorkflowFilter workflowFilter = new WorkflowFilter(null, workflowFilterTypes);
    return AppPermission.builder()
        .permissionType(WORKFLOW)
        .entityFilter(workflowFilter)
        .actions(actions)
        .appFilter(appFilter)
        .build();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testWorkflowAppPermissions() {
    AppPermission appPermission = createWorkflowAllAppsPermission();
    Set<AppPermission> appPermissions = new HashSet<>();
    appPermissions.add(appPermission);
    UserGroup userGroup = createUserGroup(null, appPermissions);
    String userGroupId = userGroup.getUuid();
    // Add Permission to userGroup using gql
    String appPermissionString = createWorkflowAllAppsPermissionGQL();
    testAppPermissions(userGroup, appPermissionString);
  }

  private String createDeploymentAllAppsPermissionGQL() {
    String appPermissionString = $GQL(
        /*
{
       appPermissions :
                             [
                          {
                             permissionType : DEPLOYMENT ,
                             applications   : {
                                                 filterType: ALL
                                              },
                             deployments      :  {
                                                     filterTypes :
[PRODUCTION_ENVIRONMENTS,NON_PRODUCTION_ENVIRONMENTS]
                                               },
                              actions:        [CREATE,DELETE,READ]
                                  }
                                 ]
}*/);

    return appPermissionString;
  }

  private AppPermission createDeploymentAllAppsPermission() {
    Set<Action> actions = createActionSet();
    GenericEntityFilter appFilter = createGenericFilterwithTypeAll();
    List<String> deploymentList = Arrays.asList("PROD", "NON_PROD");
    Set<String> deploymentFilterTypes = new HashSet<>(deploymentList);
    EnvFilter deploymentFilter = EnvFilter.builder().filterTypes(deploymentFilterTypes).build();
    return AppPermission.builder()
        .permissionType(DEPLOYMENT)
        .entityFilter(deploymentFilter)
        .actions(actions)
        .appFilter(appFilter)
        .build();
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testDeploymentAppPermissions() {
    AppPermission appPermission = createDeploymentAllAppsPermission();
    Set<AppPermission> appPermissions = new HashSet<>();
    appPermissions.add(appPermission);
    UserGroup userGroup = createUserGroup(null, appPermissions);
    String userGroupId = userGroup.getUuid();
    // Add Permission to userGroup using gql
    String appPermissionString = createDeploymentAllAppsPermissionGQL();
    testAppPermissions(userGroup, appPermissionString);
  }

  private String createPiplelineAllAppsPermissionGQL() {
    String appPermissionString = $GQL(
        /*
{
       appPermissions :
                             [
                          {
                             permissionType : PIPELINE ,
                             applications   : {
                                                 filterType: ALL
                                              },
                             pipelines      :  {
                                                     filterTypes :
[PRODUCTION_PIPELINES,NON_PRODUCTION_PIPELINES]
                                               },
                              actions:        [CREATE,DELETE,READ]
                                  }
                                 ]
}*/);

    return appPermissionString;
  }

  private AppPermission createPipelineAllAppsPermission() {
    Set<Action> actions = createActionSet();
    GenericEntityFilter appFilter = createGenericFilterwithTypeAll();
    List<String> deploymentList = Arrays.asList("PROD", "NON_PROD");
    Set<String> deploymentFilterTypes = new HashSet<>(deploymentList);
    EnvFilter deploymentFilter = EnvFilter.builder().filterTypes(deploymentFilterTypes).build();
    return AppPermission.builder()
        .permissionType(PIPELINE)
        .entityFilter(deploymentFilter)
        .actions(actions)
        .appFilter(appFilter)
        .build();
  }
  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testPipelineAppPermissions() {
    AppPermission appPermission = createPipelineAllAppsPermission();
    Set<AppPermission> appPermissions = new HashSet<>();
    appPermissions.add(appPermission);
    UserGroup userGroup = createUserGroup(null, appPermissions);
    String userGroupId = userGroup.getUuid();
    // Add Permission to userGroup using gql
    String appPermissionString = createPiplelineAllAppsPermissionGQL();
    testAppPermissions(userGroup, appPermissionString);
  }

  private String createProvisionerseAllAppsPermissionGQL() {
    String appPermissionString = $GQL(
        /*
{
       appPermissions :
                             [
                          {
                             permissionType :PROVISIONER,
                             applications   : {
                                                 filterType: ALL
                                              },
                             provisioners      :  {
                                                     filterType : ALL
                                               },
                              actions:        [CREATE,DELETE,READ]
                                  }
                                 ]
}*/);

    return appPermissionString;
  }

  private AppPermission createProvisonersAllAppsPermission() {
    Set<Action> actions = createActionSet();
    GenericEntityFilter appFilter = createGenericFilterwithTypeAll();
    GenericEntityFilter provisionerFilter = createGenericFilterwithTypeAll();
    return AppPermission.builder()
        .permissionType(PROVISIONER)
        .entityFilter(provisionerFilter)
        .actions(actions)
        .appFilter(appFilter)
        .build();
  }
  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testProvisonersPermissions() {
    AppPermission appPermission = createProvisonersAllAppsPermission();
    Set<AppPermission> appPermissions = new HashSet<>();
    appPermissions.add(appPermission);
    UserGroup userGroup = createUserGroup(null, appPermissions);
    String userGroupId = userGroup.getUuid();
    // Add Permission to userGroup using gql
    String appPermissionString = createProvisionerseAllAppsPermissionGQL();
    testAppPermissions(userGroup, appPermissionString);
  }

  private String getprovisionerFilter() {
    return String.format("provisionerIds: [\"%s\"]", infrastructureProvisioner.getUuid());
  }

  private String createListOfAppPermissionsGQL() {
    String appPermissionString = $GQL(
        /*
{
       appPermissions :
                          [
                            {
                             permissionType :PROVISIONER,
                             applications   :  {
                                                   %s
                                               },
                             provisioners    : {
                                                     %s
                                               },
                              actions:        [CREATE,DELETE,READ]
                             },
                             {
                             permissionType :DEPLOYMENT,
                             applications   :  {
                                                 %s
                                               },
                             deployments     : {
                                                     %s
                                               },
                              actions:        [CREATE,DELETE,READ]
                             },
                             {
                             permissionType :PIPELINE,
                             applications   :  {
                                                 %s
                                               },
                             pipelines    :    {
                                                     %s
                                               },
                              actions:        [CREATE,DELETE,READ]
                             }
                           ]
}*/ getAppIdsFilter(), getprovisionerFilter(), getAppIdsFilter(), getEnvFilter(), getAppIdsFilter(), getEnvFilter());
    return appPermissionString;
  }

  private GenericEntityFilter createGenericFilterwithId(String id) {
    Set<String> ids = new HashSet<>(Arrays.asList(id));
    return GenericEntityFilter.builder().filterType("SELECTED").ids(ids).build();
  }
  private Set<AppPermission> createListOfAppPermissions() {
    Set<AppPermission> appPermissions = new HashSet<>();
    GenericEntityFilter appFilter = createGenericFilterwithIds(application1.getUuid(), application2.getUuid());
    GenericEntityFilter provisionerFilter = createGenericFilterwithId(infrastructureProvisioner.getUuid());
    AppPermission provionserPermissions = AppPermission.builder()
                                              .permissionType(PROVISIONER)
                                              .entityFilter(provisionerFilter)
                                              .actions(createActionSet())
                                              .appFilter(appFilter)
                                              .build();
    List<String> envList = Arrays.asList(environment1.getUuid(), environment2.getUuid());
    Set<String> ids = new HashSet<>(envList);
    EnvFilter envFilter = EnvFilter.builder().ids(ids).filterTypes(new HashSet<>(Arrays.asList("SELECTED"))).build();
    AppPermission deploymentPermissions = AppPermission.builder()
                                              .permissionType(DEPLOYMENT)
                                              .entityFilter(envFilter)
                                              .actions(createActionSet())
                                              .appFilter(appFilter)
                                              .build();
    AppPermission pipelinePermission = AppPermission.builder()
                                           .permissionType(PIPELINE)
                                           .entityFilter(envFilter)
                                           .actions(createActionSet())
                                           .appFilter(appFilter)
                                           .build();
    appPermissions.add(provionserPermissions);
    appPermissions.add(deploymentPermissions);
    appPermissions.add(pipelinePermission);
    return appPermissions;
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category({GraphQLTests.class, UnitTests.class})
  public void testUpdatingMultipleAppPermissions() {
    Set<AppPermission> appPermissions = createListOfAppPermissions();
    UserGroup userGroup = createUserGroup(null, appPermissions);
    String userGroupId = userGroup.getUuid();
    // Add Permission to userGroup using gql
    String appPermissionString = createListOfAppPermissionsGQL();
    testAppPermissions(userGroup, appPermissionString);
  }
}
