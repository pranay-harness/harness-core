package software.wings.graphql.datafetcher.userGroup;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.graphql.schema.type.permissions.QLAccountPermissionType.ADMINISTER_OTHER_ACCOUNT_FUNCTIONS;
import static software.wings.graphql.schema.type.permissions.QLAccountPermissionType.CREATE_AND_DELETE_APPLICATION;
import static software.wings.graphql.schema.type.permissions.QLAccountPermissionType.MANAGE_TAGS;
import static software.wings.graphql.schema.type.permissions.QLAccountPermissionType.MANAGE_TEMPLATE_LIBRARY;
import static software.wings.graphql.schema.type.permissions.QLAccountPermissionType.MANAGE_USERS_AND_GROUPS;
import static software.wings.graphql.schema.type.permissions.QLAccountPermissionType.READ_USERS_AND_GROUPS;
import static software.wings.graphql.schema.type.permissions.QLAccountPermissionType.VIEW_AUDITS;
import static software.wings.security.PermissionAttribute.Action.CREATE;
import static software.wings.security.PermissionAttribute.Action.DELETE;
import static software.wings.security.PermissionAttribute.Action.EXECUTE;
import static software.wings.security.PermissionAttribute.Action.READ;
import static software.wings.security.PermissionAttribute.Action.UPDATE;
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
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.application.AppFilterController;
import software.wings.graphql.schema.type.QLAppFilter;
import software.wings.graphql.schema.type.QLEnvFilterType;
import software.wings.graphql.schema.type.permissions.QLAccountPermissionType;
import software.wings.graphql.schema.type.permissions.QLAccountPermissions;
import software.wings.graphql.schema.type.permissions.QLActions;
import software.wings.graphql.schema.type.permissions.QLAppPermissions;
import software.wings.graphql.schema.type.permissions.QLAppPermissions.QLAppPermissionsBuilder;
import software.wings.graphql.schema.type.permissions.QLDeploymentFilterType;
import software.wings.graphql.schema.type.permissions.QLDeploymentPermissions;
import software.wings.graphql.schema.type.permissions.QLEnvPermissions;
import software.wings.graphql.schema.type.permissions.QLGroupPermissions;
import software.wings.graphql.schema.type.permissions.QLPermissionType;
import software.wings.graphql.schema.type.permissions.QLPermissionsFilterType;
import software.wings.graphql.schema.type.permissions.QLPipelineFilterType;
import software.wings.graphql.schema.type.permissions.QLPipelinePermissions;
import software.wings.graphql.schema.type.permissions.QLProivionerPermissions;
import software.wings.graphql.schema.type.permissions.QLServicePermissions;
import software.wings.graphql.schema.type.permissions.QLUserGroupPermissions;
import software.wings.graphql.schema.type.permissions.QLWorkflowFilterType;
import software.wings.graphql.schema.type.permissions.QLWorkflowPermissions;
import software.wings.security.EnvFilter;
import software.wings.security.Filter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.WorkflowFilter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class UserGroupPermissionsController {
  @Inject AppFilterController appFilterController;
  final String SELECTED = "SELECTED";
  final String NON_PROD = "NON_PROD";
  final String PROD = "PROD";
  final String ALL = "ALL";
  final String TEMPLATES = "TEMPLATES";

  /*
   *   Utility functions to convert GraphQL InputTypes to the userGroup Types of portal
   */
  // user Given Account Permissions to the portal permissionType
  private PermissionType mapAccountPermissions(QLAccountPermissionType permissionType) {
    switch (permissionType) {
      case CREATE_AND_DELETE_APPLICATION:
        return APPLICATION_CREATE_DELETE;
      case READ_USERS_AND_GROUPS:
        return USER_PERMISSION_READ;
      case MANAGE_USERS_AND_GROUPS:
        return USER_PERMISSION_MANAGEMENT;
      case MANAGE_TEMPLATE_LIBRARY:
        return TEMPLATE_MANAGEMENT;
      case ADMINISTER_OTHER_ACCOUNT_FUNCTIONS:
        return ACCOUNT_MANAGEMENT;
      case VIEW_AUDITS:
        return AUDIT_VIEWER;
      case MANAGE_TAGS:
        return TAG_MANAGEMENT;
      default:
        logger.error("Invalid Account Permission Type {} given by the user", permissionType.toString());
    }
    throw new InvalidRequestException("Invalid Account Permission Type Given by the user");
  }

  // Map the userGivenActions to the portal Actions
  private Action mapAppActions(QLActions action) {
    switch (action) {
      case CREATE:
        return CREATE; // Change this later on
      case READ:
        return READ;
      case UPDATE:
        return UPDATE;
      case DELETE:
        return DELETE;
      case EXECUTE:
        return EXECUTE;
      default:
        logger.error("Invalid Action {} given by the user", action.toString());
    }
    throw new InvalidRequestException("Invalid action given by the user");
  }

  // Map the permissions given by the users to the portal permissions
  private PermissionType mapToApplicationPermission(QLPermissionType permissionType) {
    switch (permissionType) {
      case ALL:
        return ALL_APP_ENTITIES;
      case SERVICE:
        return SERVICE;
      case ENV:
        return ENV;
      case WORKFLOW:
        return WORKFLOW;
      case PIPELINE:
        return PIPELINE;
      case DEPLOYMENT:
        return DEPLOYMENT;
      case PROVISIONER:
        return PROVISIONER;
      default:
        logger.error("Invalid Permission Type {} given by the user", permissionType.toString());
    }
    throw new InvalidRequestException("Invalid Permission Type given by the user");
  }

  private EnvFilter createEnvFilter(QLEnvPermissions envPermissions) {
    Set<String> filterTypes = new HashSet<>();
    if (isNotEmpty(envPermissions.getEnvIds())) {
      filterTypes.add(SELECTED);
    } else {
      if (envPermissions.getFilterTypes().contains(QLEnvFilterType.PRODUCTION_ENVIRONMENTS)) {
        filterTypes.add(PROD);
      }
      if (envPermissions.getFilterTypes().contains(QLEnvFilterType.NON_PRODUCTION_ENVIRONMENTS)) {
        filterTypes.add(NON_PROD);
      }
    }

    return EnvFilter.builder().ids(envPermissions.getEnvIds()).filterTypes(filterTypes).build();
  }

  private WorkflowFilter createWorkflowFilter(QLWorkflowPermissions workflowPermissions) {
    Set<String> filterTypes = new HashSet<>();
    if (isNotEmpty(workflowPermissions.getEnvIds())) {
      filterTypes.add(SELECTED);
    } else {
      if (workflowPermissions.getFilterTypes().contains(QLWorkflowFilterType.PRODUCTION_WORKFLOWS)) {
        filterTypes.add(PROD);
      }
      if (workflowPermissions.getFilterTypes().contains(QLWorkflowFilterType.NON_PRODUCTION_WORKFLOWS)) {
        filterTypes.add(NON_PROD);
      }
      if (workflowPermissions.getFilterTypes().contains(QLWorkflowFilterType.WORKFLOW_TEMPLATES)) {
        filterTypes.add(TEMPLATES);
      }
    }
    return new WorkflowFilter(workflowPermissions.getEnvIds(), filterTypes);
  }

  private Filter createDeploymentFilter(QLDeploymentPermissions deploymentPermissions) {
    Set<String> filterTypes = new HashSet<>();
    if (isNotEmpty(deploymentPermissions.getEnvIds())) {
      filterTypes.add(SELECTED);
    } else {
      if (deploymentPermissions.getFilterTypes().contains(QLDeploymentFilterType.PRODUCTION_ENVIRONMENTS)) {
        filterTypes.add(PROD);
      }
      if (deploymentPermissions.getFilterTypes().contains(QLDeploymentFilterType.NON_PRODUCTION_ENVIRONMENTS)) {
        filterTypes.add(NON_PROD);
      }
    }
    return WorkflowFilter.builder().ids(deploymentPermissions.getEnvIds()).filterTypes(filterTypes).build();
  }

  private Filter createPipelineFilter(QLPipelinePermissions pipelinePermissions) {
    Set<String> filterTypes = new HashSet<>();
    if (isNotEmpty(pipelinePermissions.getEnvIds())) {
      filterTypes.add(SELECTED);
    } else {
      if (pipelinePermissions.getFilterTypes().contains(QLPipelineFilterType.PRODUCTION_PIPELINES)) {
        filterTypes.add(PROD);
      }
      if (pipelinePermissions.getFilterTypes().contains(QLPipelineFilterType.NON_PRODUCTION_PIPELINES)) {
        filterTypes.add(NON_PROD);
      }
    }
    return WorkflowFilter.builder().ids(pipelinePermissions.getEnvIds()).filterTypes(filterTypes).build();
  }

  private AppPermission convertToAppPermissionEntity(QLAppPermissions permission) {
    // Converting the GraphQL actions to portal actions
    Set<QLActions> actionsList = permission.getActions();
    Set<Action> actions = actionsList.stream().map(this ::mapAppActions).collect(Collectors.toSet());
    // Change the graphQL permissionType to the portal permissionType enum
    QLPermissionType permissionType = permission.getPermissionType();
    PermissionType appPermissionType = mapToApplicationPermission(permissionType);
    // Create the applicationFilter for the permissions
    GenericEntityFilter appFilter = appFilterController.createGenericEntityFilter(permission.getApplications());
    Filter entityFilter;
    String filterType;
    switch (permissionType) {
      case ALL:
        entityFilter = null;
        break;
      case SERVICE:
        Set<String> serviceIds = permission.getServices().getServiceIds();
        if (serviceIds != null) {
          filterType = isNotEmpty(serviceIds) ? SELECTED : ALL;
        } else {
          filterType = ALL;
        }
        entityFilter = GenericEntityFilter.builder().filterType(filterType).ids(serviceIds).build();
        break;
      case ENV:
        entityFilter = createEnvFilter(permission.getEnvironments());
        break;
      case WORKFLOW:
        entityFilter = createWorkflowFilter(permission.getWorkflows());
        break;
      case DEPLOYMENT:
        entityFilter = createDeploymentFilter(permission.getDeployments());
        break;
      case PIPELINE:
        entityFilter = createPipelineFilter(permission.getPipelines());
        break;
      case PROVISIONER:
        Set<String> provisionerIds = permission.getProvisioners().getProvisionerIds();
        if (provisionerIds != null) {
          filterType = isNotEmpty(provisionerIds) ? SELECTED : ALL;
        } else {
          filterType = ALL;
        }
        entityFilter = GenericEntityFilter.builder().filterType(filterType).ids(provisionerIds).build();
        break;
      default:
        logger.error("Invalid Application Permission Type {} given by the user", permissionType.toString());
        throw new InvalidRequestException("Invalid Application Permission type given by the user");
    }

    return AppPermission.builder()
        .permissionType(appPermissionType)
        .appFilter(appFilter)
        .entityFilter(entityFilter)
        .actions(actions)
        .build();
  }

  // Populate the AccountPermission entity
  public Set<AppPermission> populateUserGroupAppPermissionEntity(QLUserGroupPermissions permissions) {
    if (permissions == null) {
      return Collections.emptySet();
    }
    List<QLAppPermissions> appPermissions = permissions.getAppPermissions();
    Set<AppPermission> userGroupAppPermissions = null;
    if (appPermissions != null) {
      userGroupAppPermissions =
          appPermissions.stream().map(this ::convertToAppPermissionEntity).collect(Collectors.toSet());
    }
    return userGroupAppPermissions;
  }

  // Populate the AccountPermission entity
  public AccountPermissions populateUserGroupAccountPermissionEntity(QLUserGroupPermissions permissions) {
    if (permissions == null || permissions.getAccountPermissions() == null) {
      return null;
    }

    Set<QLAccountPermissionType> accountPermissionsInput =
        permissions.getAccountPermissions().getAccountPermissionTypes();
    if (accountPermissionsInput == null) {
      return null;
    }
    if (accountPermissionsInput.contains(MANAGE_USERS_AND_GROUPS)
        && !accountPermissionsInput.contains(READ_USERS_AND_GROUPS)) {
      throw new InvalidRequestException(
          "The permission MANAGE_USERS_AND_GROUPS cannot be set without setting READ_USERS_AND_GROUPS");
    }
    Set<PermissionType> accountPermissions =
        accountPermissionsInput.stream().map(this ::mapAccountPermissions).collect(Collectors.toSet());
    return AccountPermissions.builder().permissions(accountPermissions).build();
  }

  /*
   *   Utility functions to convert UserGroup Types to Graphql output
   */
  // user portal permissionType to Account Permissions output
  private QLAccountPermissionType mapAccountPermissionsToOutput(PermissionType permissionType) {
    switch (permissionType) {
      case APPLICATION_CREATE_DELETE:
        return CREATE_AND_DELETE_APPLICATION;
      case USER_PERMISSION_READ:
        return READ_USERS_AND_GROUPS;
      case USER_PERMISSION_MANAGEMENT:
        return MANAGE_USERS_AND_GROUPS;
      case TEMPLATE_MANAGEMENT:
        return MANAGE_TEMPLATE_LIBRARY;
      case ACCOUNT_MANAGEMENT:
        return ADMINISTER_OTHER_ACCOUNT_FUNCTIONS;
      case AUDIT_VIEWER:
        return VIEW_AUDITS;
      case TAG_MANAGEMENT:
        return MANAGE_TAGS;
      default:
        logger.error("Invalid Account Permission Type {} given by the user", permissionType.toString());
    }
    throw new InvalidRequestException("Invalid Account Permission Type Given by the user");
  }

  // Map the portal Actions to GraphQL action Type
  private QLActions mapAppActionsToOutput(Action action) {
    switch (action) {
      case CREATE: // Change this later on
        return QLActions.CREATE;
      case READ:
        return QLActions.READ;
      case UPDATE:
        return QLActions.UPDATE;
      case DELETE:
        return QLActions.DELETE;
      case EXECUTE:
        return QLActions.EXECUTE;
      default:
        logger.error("Invalid Action {} given by the user", action.toString());
    }
    throw new InvalidRequestException("Invalid action given by the user");
  }

  // Convert app entity to Output App Entity
  private QLPermissionType mapToApplicationPermissionOutput(PermissionType permissionType) {
    switch (permissionType) {
      case ALL_APP_ENTITIES:
        return QLPermissionType.ALL;
      case SERVICE:
        return QLPermissionType.SERVICE;
      case ENV:
        return QLPermissionType.ENV;
      case WORKFLOW:
        return QLPermissionType.WORKFLOW;
      case PIPELINE:
        return QLPermissionType.PIPELINE;
      case DEPLOYMENT:
        return QLPermissionType.DEPLOYMENT;
      case PROVISIONER:
        return QLPermissionType.PROVISIONER;
      default:
        logger.error("Invalid Permission Type {} given by the user", permissionType.toString());
    }
    throw new InvalidRequestException("Invalid Permission Type given by the user");
  }

  private QLWorkflowPermissions createWorkflowFilterOutput(WorkflowFilter workflowPermissions) {
    if (isEmpty(workflowPermissions.getIds())) {
      EnumSet<QLWorkflowFilterType> filterTypes = EnumSet.noneOf(QLWorkflowFilterType.class);
      if (workflowPermissions.getFilterTypes().contains(PROD)) {
        filterTypes.add(QLWorkflowFilterType.PRODUCTION_WORKFLOWS);
      }
      if (workflowPermissions.getFilterTypes().contains(NON_PROD)) {
        filterTypes.add(QLWorkflowFilterType.NON_PRODUCTION_WORKFLOWS);
      }
      if (workflowPermissions.getFilterTypes().contains(TEMPLATES)) {
        filterTypes.add(QLWorkflowFilterType.WORKFLOW_TEMPLATES);
      }
      return QLWorkflowPermissions.builder().filterTypes(filterTypes).build();
    }
    return QLWorkflowPermissions.builder().envIds(workflowPermissions.getIds()).build();
  }

  private QLEnvPermissions createEnvFilterOutput(EnvFilter envPermissions) {
    if (isEmpty(envPermissions.getIds())) {
      EnumSet<QLEnvFilterType> filterTypes = EnumSet.noneOf(QLEnvFilterType.class);
      if (envPermissions.getFilterTypes().contains(PROD)) {
        filterTypes.add(QLEnvFilterType.PRODUCTION_ENVIRONMENTS);
      }
      if (envPermissions.getFilterTypes().contains(NON_PROD)) {
        filterTypes.add(QLEnvFilterType.NON_PRODUCTION_ENVIRONMENTS);
      }
      return QLEnvPermissions.builder().envIds(envPermissions.getIds()).filterTypes(filterTypes).build();
    }
    return QLEnvPermissions.builder().envIds(envPermissions.getIds()).build();
  }

  private QLDeploymentPermissions createDeploymentFilterOutput(EnvFilter envPermissions) {
    if (isEmpty(envPermissions.getIds())) {
      EnumSet<QLDeploymentFilterType> filterTypes = EnumSet.noneOf(QLDeploymentFilterType.class);
      if (envPermissions.getFilterTypes().contains(PROD)) {
        filterTypes.add(QLDeploymentFilterType.PRODUCTION_ENVIRONMENTS);
      }
      if (envPermissions.getFilterTypes().contains(NON_PROD)) {
        filterTypes.add(QLDeploymentFilterType.NON_PRODUCTION_ENVIRONMENTS);
      }
      return QLDeploymentPermissions.builder().envIds(envPermissions.getIds()).filterTypes(filterTypes).build();
    }
    return QLDeploymentPermissions.builder().envIds(envPermissions.getIds()).build();
  }

  private QLPipelinePermissions createPipelineFilterOutput(EnvFilter envPermissions) {
    if (isEmpty(envPermissions.getIds())) {
      EnumSet<QLPipelineFilterType> filterTypes = EnumSet.noneOf(QLPipelineFilterType.class);
      if (envPermissions.getFilterTypes().contains(PROD)) {
        filterTypes.add(QLPipelineFilterType.PRODUCTION_PIPELINES);
      }
      if (envPermissions.getFilterTypes().contains(NON_PROD)) {
        filterTypes.add(QLPipelineFilterType.NON_PRODUCTION_PIPELINES);
      }
      return QLPipelinePermissions.builder().envIds(envPermissions.getIds()).filterTypes(filterTypes).build();
    }
    return QLPipelinePermissions.builder().envIds(envPermissions.getIds()).build();
  }

  public List<QLAppPermissions> populateUserGroupAppPermissionOutput(Set<AppPermission> appPermissions) {
    List<QLAppPermissions> userGroupAppPermissions = null;
    if (appPermissions != null) {
      userGroupAppPermissions =
          appPermissions.stream().map(this ::convertToAppPermissionOutput).collect(Collectors.toList());
    }
    return userGroupAppPermissions;
  }

  private QLAppPermissions convertToAppPermissionOutput(AppPermission permission) {
    // Convert portal actions to graphQL output
    Set<Action> actionsList = permission.getActions();
    Set<QLActions> actions = actionsList.stream().map(this ::mapAppActionsToOutput).collect(Collectors.toSet());
    // Convert portal permissionType to graphQL output PermissionType
    PermissionType permissionType = permission.getPermissionType();
    QLPermissionType appPermissionType = mapToApplicationPermissionOutput(permissionType);
    // Convert the appFilter to the graphQLOutputType
    QLAppFilter appFilter = appFilterController.createAppFilterOutput(permission.getAppFilter());
    QLAppPermissionsBuilder builder = QLAppPermissions.builder().applications(appFilter).actions(actions);
    switch (permissionType) {
      case ALL_APP_ENTITIES:
        return builder.permissionType(appPermissionType).build();
      case SERVICE:
        QLServicePermissions servicePermissions;
        Set<String> serviceIds = permission.getEntityFilter().getIds();
        if (isEmpty(serviceIds)) {
          servicePermissions =
              QLServicePermissions.builder().filterType(QLPermissionsFilterType.ALL).serviceIds(serviceIds).build();
        } else {
          servicePermissions = QLServicePermissions.builder().serviceIds(serviceIds).build();
        }
        return builder.permissionType(appPermissionType).services(servicePermissions).build();
      case ENV:
        QLEnvPermissions envFilter = createEnvFilterOutput((EnvFilter) permission.getEntityFilter());
        return builder.permissionType(appPermissionType).environments(envFilter).build();
      case WORKFLOW:
        QLWorkflowPermissions workflowFilter =
            createWorkflowFilterOutput((WorkflowFilter) permission.getEntityFilter());
        return builder.permissionType(appPermissionType).workflows(workflowFilter).build();
      case DEPLOYMENT:
        QLDeploymentPermissions deploymentFilter =
            createDeploymentFilterOutput((EnvFilter) permission.getEntityFilter());
        return builder.permissionType(appPermissionType).deployments(deploymentFilter).build();
      case PIPELINE:
        QLPipelinePermissions pipelineFilter = createPipelineFilterOutput((EnvFilter) permission.getEntityFilter());
        return builder.permissionType(appPermissionType).pipelines(pipelineFilter).build();
      case PROVISIONER:
        QLProivionerPermissions provisionerPermissions;
        Set<String> provisionersId = permission.getEntityFilter().getIds();
        if (isEmpty(provisionersId)) {
          provisionerPermissions = QLProivionerPermissions.builder()
                                       .filterType(QLPermissionsFilterType.ALL)
                                       .provisionerIds(provisionersId)
                                       .build();
        } else {
          provisionerPermissions = QLProivionerPermissions.builder().provisionerIds(provisionersId).build();
        }
        return builder.permissionType(appPermissionType).provisioners(provisionerPermissions).build();
      default:
        logger.error("Invalid Application Permission Type {} given by the user", permissionType.toString());
        throw new InvalidRequestException("Invalid Application Permission type given by the user");
    }
  }

  public QLAccountPermissions populateUserGroupAccountPermission(AccountPermissions permissions) {
    Set<QLAccountPermissionType> outputPermissions = null;
    if (permissions == null) {
      return null;
    }
    Set<PermissionType> accountPermissions = permissions.getPermissions();
    if (accountPermissions != null) {
      outputPermissions =
          accountPermissions.stream().map(this ::mapAccountPermissionsToOutput).collect(Collectors.toSet());
    }
    return QLAccountPermissions.builder().accountPermissionTypes(outputPermissions).build();
  }

  public QLGroupPermissions populateUserGroupPermissions(UserGroup userGroup) {
    QLAccountPermissions accountPermissions = populateUserGroupAccountPermission(userGroup.getAccountPermissions());
    List<QLAppPermissions> appPermissions = populateUserGroupAppPermissionOutput(userGroup.getAppPermissions());
    return QLGroupPermissions.builder().appPermissions(appPermissions).accountPermissions(accountPermissions).build();
  }
}
