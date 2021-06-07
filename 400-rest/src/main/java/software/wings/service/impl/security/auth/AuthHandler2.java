package software.wings.service.impl.security.auth;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.noop;

import static software.wings.beans.security.UserGroup.DEFAULT_ACCOUNT_ADMIN_USER_GROUP_NAME;
import static software.wings.beans.security.UserGroup.DEFAULT_NON_PROD_SUPPORT_USER_GROUP_NAME;
import static software.wings.beans.security.UserGroup.DEFAULT_PROD_SUPPORT_USER_GROUP_NAME;
import static software.wings.security.EnvFilter.FilterType.NON_PROD;
import static software.wings.security.EnvFilter.FilterType.PROD;
import static software.wings.security.GenericEntityFilter.FilterType.SELECTED;
import static software.wings.security.PermissionAttribute.PermissionType.ACCOUNT_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.ALL_APP_ENTITIES;
import static software.wings.security.PermissionAttribute.PermissionType.AUDIT_VIEWER;
import static software.wings.security.PermissionAttribute.PermissionType.CE_ADMIN;
import static software.wings.security.PermissionAttribute.PermissionType.CE_VIEWER;
import static software.wings.security.PermissionAttribute.PermissionType.CREATE_CUSTOM_DASHBOARDS;
import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.PermissionType.ENV;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_ALERT_NOTIFICATION_RULES;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_API_KEYS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATIONS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATION_STACKS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_AUTHENTICATION_SETTINGS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CLOUD_PROVIDERS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONFIG_AS_CODE;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CONNECTORS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_CUSTOM_DASHBOARDS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATES;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DELEGATE_PROFILES;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_DEPLOYMENT_FREEZES;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_IP_WHITELIST;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_PIPELINE_GOVERNANCE_STANDARDS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_RESTRICTED_ACCESS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRETS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SECRET_MANAGERS;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_SSH_AND_WINRM;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_TAGS;
import static software.wings.security.PermissionAttribute.PermissionType.PIPELINE;
import static software.wings.security.PermissionAttribute.PermissionType.PROVISIONER;
import static software.wings.security.PermissionAttribute.PermissionType.SERVICE;
import static software.wings.security.PermissionAttribute.PermissionType.TEMPLATE_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_MANAGEMENT;
import static software.wings.security.PermissionAttribute.PermissionType.USER_PERMISSION_READ;
import static software.wings.security.PermissionAttribute.PermissionType.WORKFLOW;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EnvironmentType;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.Account;
import software.wings.beans.ApiKeyEntry;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.HttpMethod;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.notification.NotificationSettings;
import software.wings.beans.notification.SlackNotificationSetting;
import software.wings.beans.security.AccountPermissions;
import software.wings.beans.security.AppPermission;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerExpressionEvaluator;
import software.wings.security.AccountPermissionSummary;
import software.wings.security.AccountPermissionSummary.AccountPermissionSummaryBuilder;
import software.wings.security.AppPermissionSummary;
import software.wings.security.AppPermissionSummaryForUI;
import software.wings.security.AppPermissionSummaryForUI.AppPermissionSummaryForUIBuilder;
import software.wings.security.AppPermissionSummaryWithName;
import software.wings.security.AppPermissionSummaryWithName.EnvInfo;
import software.wings.security.EntityInfo;
import software.wings.security.EnvFilter;
import software.wings.security.Filter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.GenericEntityFilter.FilterType;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserPermissionInfo;
import software.wings.security.UserPermissionInfo.UserPermissionInfoBuilder;
import software.wings.security.UserPermissionNameInfo;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.security.WorkflowFilter;
import software.wings.service.impl.UserGroupServiceImpl;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.intfc.ApiKeyService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HarnessApiKeyService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;
import software.wings.sm.states.EnvState.EnvStateKeys;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import javax.swing.text.html.parser.Entity;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author rktummala on 3/7/18
 */
@OwnedBy(PL)
@TargetModule(HarnessModule.UNDEFINED)
@Singleton
@Slf4j
public class AuthHandler2 {
  /**
   * The constant DEFAULT_PROD_SUPPORT_USER_GROUP_DESCRIPTION.
   */
  private static final String DEFAULT_PROD_SUPPORT_USER_GROUP_DESCRIPTION =
      "Production Support members have access to override configuration, "
      + "setup infrastructure and setup/execute deployment workflows within PROD environments";
  /**
   * The constant DEFAULT_NON_PROD_SUPPORT_USER_GROUP_DESCRIPTION.
   */
  private static final String DEFAULT_NON_PROD_SUPPORT_USER_GROUP_DESCRIPTION =
      "Non-production Support members have access to override configuration, "
      + "setup infrastructure and setup/execute deployment workflows within NON_PROD environments";

  private static final String USER_NOT_AUTHORIZED = "User not authorized";

  @Inject private PipelineService pipelineService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private EnvironmentService environmentService;
  @Inject private WorkflowService workflowService;
  @Inject private UserGroupService userGroupService;
  @Inject private AuthService authService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private DashboardAuthHandler dashboardAuthHandler;
  @Inject private ApiKeyService apiKeyService;
  @Inject private HarnessApiKeyService harnessApiKeyService;
  @Inject private WorkflowServiceHelper workflowServiceHelper;

  public UserPermissionNameInfo evaluateUserPermissionInfo(String accountId, List<UserGroup> userGroups, User user) {
    UserPermissionNameInfo.UserPermissionNameInfoBuilder userPermissionInfoBuilder = UserPermissionNameInfo.builder().accountId(accountId);

    Set<PermissionType> accountPermissionSet = new HashSet<>();
    AccountPermissionSummaryBuilder accountPermissionSummaryBuilder =
        AccountPermissionSummary.builder().permissions(accountPermissionSet);

    populateRequiredAccountPermissions(userGroups, accountPermissionSet);

    // Get all app ids
    HashSet<String> allAppIds = new HashSet<>(appService.getAppIdsByAccountId(accountId));

    // Cache all the entities by app id first
    Map<PermissionType, Set<String>> permissionTypeAppIdSetMap = collectRequiredAppIds(userGroups, allAppIds);

    // Fetch all entities by appIds
    Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap =
        fetchRequiredEntities(accountId, permissionTypeAppIdSetMap);

    // Filter and assign permissions
    Map<String, AppPermissionSummaryWithName> appPermissionMap =
        populateAppPermissions(userGroups, permissionTypeAppIdEntityMap, allAppIds);
    // TODO: After here
    userPermissionInfoBuilder.appPermissionMapInternal(appPermissionMap)
        .accountPermissionSummary(accountPermissionSummaryBuilder.build());

    userPermissionInfoBuilder.hasAllAppAccess(allAppIds.size() <= appPermissionMap.keySet().size());

    UserPermissionNameInfo userPermissionInfo = userPermissionInfoBuilder.build();
    //    setAppPermissionMap(userPermissionInfo);

    //    Map<String, Set<io.harness.dashboard.Action>> dashboardPermissions =
    //        dashboardAuthHandler.getDashboardAccessPermissions(user, accountId, userPermissionInfo, userGroups);
    //    userPermissionInfo.setDashboardPermissions(dashboardPermissions);

    return userPermissionInfo;
  }

  private Map<String, AppPermissionSummaryWithName> populateAppPermissions(List<UserGroup> userGroups,
      Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap, HashSet<String> allAppIds) {
    Map<String, AppPermissionSummaryWithName> appPermissionMap = new HashMap<>();
    Multimap<String, Action> envActionMapForPipeline = HashMultimap.create();
    Multimap<String, Action> envActionMapForDeployment = HashMultimap.create();

    userGroups.forEach(userGroup -> {
      Set<AppPermission> appPermissions = userGroup.getAppPermissions();
      if (isEmpty(appPermissions)) {
        return;
      }

      appPermissions.forEach(appPermission -> {
        if (isEmpty(appPermission.getActions())) {
          log.error("Actions empty for apps: {}", appPermission.getAppFilter());
          return;
        }

        Set<String> appIds = getAppIdsByFilter(allAppIds, appPermission.getAppFilter());
        PermissionType permissionType = appPermission.getPermissionType();

        if (permissionType == ALL_APP_ENTITIES) {
          asList(SERVICE, PROVISIONER, ENV, WORKFLOW, DEPLOYMENT).forEach(permissionType1 -> {
            // ignoring entity filter in case of ALL_APP_ENTITIES
            attachPermission(appPermissionMap, permissionTypeAppIdEntityMap, appIds, permissionType1, null,
                appPermission.getActions());
          });

          attachPipelinePermission(envActionMapForPipeline, appPermissionMap, permissionTypeAppIdEntityMap, appIds,
              PIPELINE, null, appPermission.getActions());

          attachPipelinePermission(envActionMapForDeployment, appPermissionMap, permissionTypeAppIdEntityMap, appIds,
              DEPLOYMENT, null, appPermission.getActions());

        } else {
          if (permissionType == PIPELINE) {
            attachPipelinePermission(envActionMapForPipeline, appPermissionMap, permissionTypeAppIdEntityMap, appIds,
                permissionType, appPermission.getEntityFilter(), appPermission.getActions());
          } else {
            attachPermission(appPermissionMap, permissionTypeAppIdEntityMap, appIds, permissionType,
                appPermission.getEntityFilter(), appPermission.getActions());

            if (permissionType == DEPLOYMENT) {
              attachPipelinePermission(envActionMapForDeployment, appPermissionMap, permissionTypeAppIdEntityMap,
                  appIds, permissionType, appPermission.getEntityFilter(), appPermission.getActions());
            }
          }
        }
      });
    });

    return appPermissionMap;
  }
  private Map<Action, Set<EntityInfo>> buildActionEntityMap(
      Map<Action, Set<EntityInfo>> permissionMap, Set<EntityInfo> entityIdSet, Set<Action> actionSet) {
    if (permissionMap == null) {
      permissionMap = new HashMap<>();
    }

    Map<Action, Set<EntityInfo>> finalPermissionMap = permissionMap;
    actionSet.forEach(action -> {
      Set<EntityInfo> existingEntityIdSet = finalPermissionMap.get(action);
      if (isEmpty(existingEntityIdSet)) {
        existingEntityIdSet = new HashSet<>();
        finalPermissionMap.put(action, existingEntityIdSet);
      }
      existingEntityIdSet.addAll(entityIdSet);
    });
    return permissionMap;
  }

  private Map<Action, Set<EntityInfo>> buildActionPipelineMap(
      Map<Action, Set<EntityInfo>> permissionMap, Multimap<EntityInfo, Action> pipelineIdActionMap) {
    if (permissionMap == null) {
      permissionMap = new HashMap<>();
    }

    Map<Action, Set<EntityInfo>> finalPermissionMap = permissionMap;
    pipelineIdActionMap.forEach((pipelineEntity, action) -> {
      Set<EntityInfo> existingPipelineIdSet = finalPermissionMap.get(action);
      if (isEmpty(existingPipelineIdSet)) {
        existingPipelineIdSet = new HashSet<>();
        finalPermissionMap.put(action, existingPipelineIdSet);
      }
      existingPipelineIdSet.add(pipelineEntity);
    });
    return permissionMap;
  }

  private Map<Action, Set<AppPermissionSummaryWithName.EnvInfo>> buildActionEnvMap(
      Map<Action, Set<AppPermissionSummaryWithName.EnvInfo>> permissionMap,
      Set<AppPermissionSummaryWithName.EnvInfo> envInfoSet, Set<Action> actionSet) {
    if (permissionMap == null) {
      permissionMap = new HashMap<>();
    }

    Map<Action, Set<AppPermissionSummaryWithName.EnvInfo>> finalPermissionMap = permissionMap;
    actionSet.forEach(action -> {
      Set<AppPermissionSummaryWithName.EnvInfo> existingEnvIdSet = finalPermissionMap.get(action);
      if (isEmpty(existingEnvIdSet)) {
        existingEnvIdSet = new HashSet<>();
        finalPermissionMap.put(action, existingEnvIdSet);
      }
      existingEnvIdSet.addAll(envInfoSet);
    });
    return permissionMap;
  }

  private void attachPermission(Map<String, AppPermissionSummaryWithName> appPermissionMap,
      Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap, Set<String> appIds,
      PermissionType permissionType, Filter entityFilter, Set<Action> actions) {
    final HashSet<Action> fixedEntityActions = Sets.newHashSet(Action.READ, Action.UPDATE, Action.DELETE,
        Action.EXECUTE_PIPELINE, Action.EXECUTE_WORKFLOW, Action.EXECUTE_WORKFLOW_ROLLBACK);
    appIds.forEach(appId -> {
      AppPermissionSummaryWithName appPermissionSummary = appPermissionMap.get(appId);
      if (appPermissionSummary == null) {
        appPermissionSummary = new AppPermissionSummaryWithName();
        appPermissionMap.put(appId, appPermissionSummary);
      }

      SetView<Action> intersection = Sets.intersection(fixedEntityActions, actions);
      Set<Action> entityActions = new HashSet<>(intersection);
      final AppPermissionSummaryWithName finalAppPermissionSummary = appPermissionSummary;
      switch (permissionType) {
        case SERVICE: {
          if (actions.contains(Action.CREATE)) {
            appPermissionSummary.setCanCreateService(true);
          }

          if (isEmpty(entityActions)) {
            break;
          }

          Set<EntityInfo> entityIds = getServiceIdsByFilter(
              permissionTypeAppIdEntityMap.get(permissionType).get(appId), (GenericEntityFilter) entityFilter);
          if (isEmpty(entityIds)) {
            break;
          }
          Map<Action, Set<EntityInfo>> actionEntityIdMap =
              buildActionEntityMap(finalAppPermissionSummary.getServicePermissions(), entityIds, entityActions);
          finalAppPermissionSummary.setServicePermissions(actionEntityIdMap);
          break;
        }
        case PROVISIONER: {
          if (actions.contains(Action.CREATE)) {
            appPermissionSummary.setCanCreateProvisioner(true);
          }

          if (isEmpty(entityActions)) {
            break;
          }
          Set<EntityInfo> entityIds = getProvisionerIdsByFilter(
              permissionTypeAppIdEntityMap.get(permissionType).get(appId), (GenericEntityFilter) entityFilter);
          if (isEmpty(entityIds)) {
            break;
          }
          Map<Action, Set<EntityInfo>> actionEntityIdMap =
              buildActionEntityMap(finalAppPermissionSummary.getProvisionerPermissions(), entityIds, entityActions);
          finalAppPermissionSummary.setProvisionerPermissions(actionEntityIdMap);
          break;
        }
        case ENV: {
          if (actions.contains(Action.CREATE)) {
            appPermissionSummary.setCanCreateEnvironment(true);
            Set<EnvironmentType> environmentTypeSet = addEnvTypesByFilter(
                finalAppPermissionSummary.getEnvCreatePermissionsForEnvTypes(), (EnvFilter) entityFilter);
            finalAppPermissionSummary.setEnvCreatePermissionsForEnvTypes(environmentTypeSet);
          }

          if (isEmpty(entityActions)) {
            break;
          }
          Set<EnvInfo> envInfoSet = getEnvsInfoByFilter(
              permissionTypeAppIdEntityMap.get(permissionType).get(appId), (EnvFilter) entityFilter);
          if (isEmpty(envInfoSet)) {
            break;
          }

          Map<Action, Set<AppPermissionSummaryWithName.EnvInfo>> actionEnvInfoMap =
              buildActionEnvMap(finalAppPermissionSummary.getEnvPermissions(), envInfoSet, entityActions);
          finalAppPermissionSummary.setEnvPermissions(actionEnvInfoMap);
          break;
        }
        case WORKFLOW: {
          Set<String> envIdSet = null;
          if (actions.contains(Action.CREATE)) {
            appPermissionSummary.setCanCreateWorkflow(true);
            envIdSet = getEnvIdsByFilter(permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter);
            Set<String> updatedEnvIdSet =
                addToExistingEntityIdSet(finalAppPermissionSummary.getWorkflowCreatePermissionsForEnvs(), envIdSet);
            finalAppPermissionSummary.setWorkflowCreatePermissionsForEnvs(updatedEnvIdSet);

            if (!finalAppPermissionSummary.isCanCreateTemplatizedWorkflow()) {
              WorkflowFilter workflowFilter = getDefaultWorkflowFilterIfNull((WorkflowFilter) entityFilter);
              Set<String> filterTypes = workflowFilter.getFilterTypes();
              if (isNotEmpty(filterTypes)) {
                boolean hasTemplateFilterType = filterTypes.contains(WorkflowFilter.FilterType.TEMPLATES);
                finalAppPermissionSummary.setCanCreateTemplatizedWorkflow(hasTemplateFilterType);
              }
            }
          }

          if (isEmpty(entityActions)) {
            break;
          }

          if (entityActions.contains(Action.UPDATE)) {
            if (envIdSet == null) {
              envIdSet = getEnvIdsByFilter(permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter);
            }

            Set<String> updatedEnvIdSet =
                addToExistingEntityIdSet(finalAppPermissionSummary.getWorkflowUpdatePermissionsForEnvs(), envIdSet);
            finalAppPermissionSummary.setWorkflowUpdatePermissionsForEnvs(updatedEnvIdSet);
          }

          Set<EntityInfo> entityIds =
              getWorkflowIdsByFilter(permissionTypeAppIdEntityMap.get(permissionType).get(appId),
                  permissionTypeAppIdEntityMap.get(ENV).get(appId), (WorkflowFilter) entityFilter);

          if (isEmpty(entityIds)) {
            break;
          }

          Map<Action, Set<EntityInfo>> actionEntityIdMap =
              buildActionEntityMap(finalAppPermissionSummary.getWorkflowPermissions(), entityIds, entityActions);
          finalAppPermissionSummary.setWorkflowPermissions(actionEntityIdMap);
          break;
        }
        case DEPLOYMENT: {
          if (isEmpty(entityActions)) {
            break;
          }
          Set<EntityInfo> entityIds = getDeploymentIdsByFilter(permissionTypeAppIdEntityMap.get(WORKFLOW).get(appId),
              permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter, appId);

          if (isEmpty(entityIds)) {
            break;
          }

          Map<Action, Set<EntityInfo>> actionEntityIdMap =
              buildActionEntityMap(finalAppPermissionSummary.getDeploymentPermissions(), entityIds, entityActions);
          finalAppPermissionSummary.setDeploymentPermissions(actionEntityIdMap);

          Set<String> envIdSet =
              getEnvIdsByFilter(permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter);

          if (entityActions.contains(Action.EXECUTE_WORKFLOW)) {
            Set<String> updatedEnvIdSet =
                addToExistingEntityIdSet(finalAppPermissionSummary.getWorkflowExecutePermissionsForEnvs(), envIdSet);
            finalAppPermissionSummary.setWorkflowExecutePermissionsForEnvs(updatedEnvIdSet);
          }
          if (entityActions.contains(Action.EXECUTE_PIPELINE)) {
            Set<String> updatedEnvIdSet =
                addToExistingEntityIdSet(finalAppPermissionSummary.getPipelineExecutePermissionsForEnvs(), envIdSet);
            finalAppPermissionSummary.setPipelineExecutePermissionsForEnvs(updatedEnvIdSet);
          }
          if (entityActions.contains(Action.EXECUTE_WORKFLOW_ROLLBACK)) {
            Set<String> updatedEnvIdSet = addToExistingEntityIdSet(
                finalAppPermissionSummary.getRollbackWorkflowExecutePermissionsForEnvs(), envIdSet);
            finalAppPermissionSummary.setRollbackWorkflowExecutePermissionsForEnvs(updatedEnvIdSet);
          }
          break;
        }
        default:
          noop();
      }
    });
  }

  private Set<String> addToExistingEntityIdSet(Set<String> existingEntityIdSet, Set<String> entityIdSet) {
    if (isEmpty(entityIdSet)) {
      return existingEntityIdSet;
    }

    if (existingEntityIdSet == null) {
      existingEntityIdSet = new HashSet<>();
    }

    existingEntityIdSet.addAll(entityIdSet);
    return existingEntityIdSet;
  }

  private Map<String, Workflow> prepareWorkflowCacheFromEntityMap(
      String appId, Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap) {
    // Try to create a workflow cache using permissionTypeAppIdEntityMap.
    Map<String, Workflow> workflowCache = new HashMap<>();
    if (isEmpty(permissionTypeAppIdEntityMap)) {
      return workflowCache;
    }

    Map<String, List<Base>> appIdWorkflowsMap = permissionTypeAppIdEntityMap.get(WORKFLOW);
    if (isNotEmpty(appIdWorkflowsMap)) {
      List<Base> workflowList = appIdWorkflowsMap.get(appId);
      if (isNotEmpty(workflowList)) {
        for (Base entity : workflowList) {
          if (!(entity instanceof Workflow)) {
            continue;
          }

          Workflow workflow = (Workflow) entity;
          workflowCache.put(workflow.getUuid(), workflow);
        }
      }
    }

    return workflowCache;
  }

  private void attachPipelinePermission(Multimap<String, Action> envActionMap,
      Map<String, AppPermissionSummaryWithName> appPermissionMap,
      Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap, Set<String> appIds,
      PermissionType permissionType, Filter entityFilter, Set<Action> actions) {
    final HashSet<Action> fixedEntityActions = Sets.newHashSet(Action.READ, Action.UPDATE, Action.DELETE,
        Action.EXECUTE_PIPELINE, Action.EXECUTE_WORKFLOW, Action.EXECUTE_WORKFLOW_ROLLBACK);
    appIds.forEach(appId -> {
      AppPermissionSummaryWithName appPermissionSummary = appPermissionMap.get(appId);
      if (appPermissionSummary == null) {
        appPermissionSummary = new AppPermissionSummaryWithName();
        appPermissionMap.put(appId, appPermissionSummary);
      }

      Map<String, Workflow> workflowCache = prepareWorkflowCacheFromEntityMap(appId, permissionTypeAppIdEntityMap);
      SetView<Action> intersection = Sets.intersection(fixedEntityActions, actions);
      Set<Action> entityActions = new HashSet<>(intersection);
      AppPermissionSummaryWithName finalAppPermissionSummary = appPermissionSummary;
      Multimap<EntityInfo, Action> pipelineIdActionMap;
      switch (permissionType) {
        case PIPELINE:
          Set<String> envIdSet = null;
          if (actions.contains(Action.CREATE)) {
            appPermissionSummary.setCanCreatePipeline(true);
            envIdSet = getEnvIdsByFilter(permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter);
            Set<String> updatedEnvIdSet =
                addToExistingEntityIdSet(finalAppPermissionSummary.getPipelineCreatePermissionsForEnvs(), envIdSet);
            finalAppPermissionSummary.setPipelineCreatePermissionsForEnvs(updatedEnvIdSet);
          }

          if (isEmpty(entityActions)) {
            break;
          }

          if (entityActions.contains(Action.UPDATE)) {
            if (envIdSet == null) {
              envIdSet = getEnvIdsByFilter(permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter);
            }
            Set<String> updatedEnvIdSet =
                addToExistingEntityIdSet(finalAppPermissionSummary.getPipelineUpdatePermissionsForEnvs(), envIdSet);
            finalAppPermissionSummary.setPipelineUpdatePermissionsForEnvs(updatedEnvIdSet);
          }

          pipelineIdActionMap = getPipelineIdsByFilter(permissionTypeAppIdEntityMap.get(PIPELINE).get(appId),
              permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter, envActionMap, entityActions,
              workflowCache);

          Map<Action, Set<EntityInfo>> actionEntityIdMap =
              buildActionPipelineMap(finalAppPermissionSummary.getPipelinePermissions(), pipelineIdActionMap);
          finalAppPermissionSummary.setPipelinePermissions(actionEntityIdMap);
          break;

        case DEPLOYMENT:
          if (isEmpty(entityActions)) {
            break;
          }

          pipelineIdActionMap = getPipelineIdsByFilter(permissionTypeAppIdEntityMap.get(PIPELINE).get(appId),
              permissionTypeAppIdEntityMap.get(ENV).get(appId), (EnvFilter) entityFilter, envActionMap, entityActions,
              workflowCache);

          actionEntityIdMap =
              buildActionPipelineMap(finalAppPermissionSummary.getDeploymentPermissions(), pipelineIdActionMap);
          finalAppPermissionSummary.setDeploymentPermissions(actionEntityIdMap);
          break;

        default:
          noop();
      }
    });
  }

  private Map<PermissionType, Map<String, List<Base>>> fetchRequiredEntities(
      String accountId, Map<PermissionType, Set<String>> permissionTypeAppIdSetMap) {
    Map<PermissionType, Map<String, List<Base>>> permissionTypeAppIdEntityMap = new HashMap<>();
    if ((permissionTypeAppIdSetMap.containsKey(PIPELINE) || permissionTypeAppIdSetMap.containsKey(DEPLOYMENT))
        && !permissionTypeAppIdSetMap.containsKey(WORKFLOW)) {
      // Read workflows in case the WORKFLOW permission type is not present but the PIPELINE or DEPLOYMENT permission is
      // present.
      permissionTypeAppIdEntityMap.put(WORKFLOW, getAppIdWorkflowMap(accountId));
    }

    permissionTypeAppIdSetMap.keySet().forEach(permissionType -> {
      switch (permissionType) {
        case SERVICE: {
          permissionTypeAppIdEntityMap.put(permissionType, getAppIdServiceMap(accountId));
          break;
        }
        case PROVISIONER: {
          permissionTypeAppIdEntityMap.put(permissionType, getAppIdProvisionerMap(accountId));
          break;
        }
        case ENV: {
          permissionTypeAppIdEntityMap.put(permissionType, getAppIdEnvMap(accountId));
          break;
        }
        case WORKFLOW: {
          permissionTypeAppIdEntityMap.put(permissionType, getAppIdWorkflowMap(accountId));
          break;
        }
        case PIPELINE: {
          permissionTypeAppIdEntityMap.put(permissionType, getAppIdPipelineMap(accountId));
          break;
        }
        default: {
          noop();
        }
      }
    });
    return permissionTypeAppIdEntityMap;
  }

  private Map<String, List<Base>> getAppIdServiceMap(String accountId) {
    PageRequest<Service> pageRequest =
        aPageRequest().addFilter("accountId", Operator.EQ, accountId).addFieldsIncluded("_id", "appId").build();
    List<Service> list =
        getAllEntities(pageRequest, () -> serviceResourceService.list(pageRequest, false, false, false, null));
    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private Map<String, List<Base>> getAppIdProvisionerMap(String accountId) {
    PageRequest<InfrastructureProvisioner> pageRequest =
        aPageRequest().addFilter("accountId", Operator.EQ, accountId).addFieldsIncluded("_id", "appId").build();

    List<InfrastructureProvisioner> list =
        getAllEntities(pageRequest, () -> infrastructureProvisionerService.list(pageRequest));
    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private Map<String, List<Base>> getAppIdEnvMap(String accountId) {
    PageRequest<Environment> pageRequest = aPageRequest()
                                               .addFilter("accountId", Operator.EQ, accountId)
                                               .addFieldsIncluded("_id", "appId", "environmentType")
                                               .build();

    List<Environment> list = getAllEntities(pageRequest, () -> environmentService.list(pageRequest, false, null));

    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private Map<String, List<Base>> getAppIdWorkflowMap(String accountId) {
    PageRequest<Workflow> pageRequest =
        aPageRequest()
            .addFilter("accountId", Operator.EQ, accountId)
            .addFieldsIncluded("_id", "appId", "envId", "templatized", "templateExpressions")
            .build();

    List<Workflow> list =
        getAllEntities(pageRequest, () -> workflowService.listWorkflowsWithoutOrchestration(pageRequest));
    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private Map<String, List<Base>> getAppIdPipelineMap(String accountId) {
    PageRequest<Pipeline> pageRequest = aPageRequest().addFilter("accountId", Operator.EQ, accountId).build();
    List<Pipeline> list = getAllEntities(pageRequest, () -> pipelineService.listPipelines(pageRequest));
    return list.stream().collect(Collectors.groupingBy(Base::getAppId));
  }

  private void populateRequiredAccountPermissions(
      List<UserGroup> userGroups, Set<PermissionType> accountPermissionSet) {
    userGroups.forEach(userGroup -> {
      AccountPermissions accountPermissions = userGroup.getAccountPermissions();
      if (accountPermissions != null) {
        Set<PermissionType> permissions = accountPermissions.getPermissions();
        if (CollectionUtils.isNotEmpty(permissions)) {
          accountPermissionSet.addAll(permissions);
        }
      }
    });
  }

  private Map<PermissionType, Set<String>> collectRequiredAppIds(
      List<UserGroup> userGroups, HashSet<String> allAppIds) {
    Map<PermissionType, Set<String>> permissionTypeAppIdSetMap = new HashMap<>();
    // initialize
    asList(SERVICE, PROVISIONER, ENV, WORKFLOW, PIPELINE, DEPLOYMENT)
        .forEach(permissionType -> permissionTypeAppIdSetMap.put(permissionType, new HashSet<>()));

    userGroups.forEach(userGroup -> {
      Set<AppPermission> appPermissions = userGroup.getAppPermissions();
      if (isEmpty(appPermissions)) {
        return;
      }

      appPermissions.forEach(appPermission -> {
        Set<String> appIdSet = getAppIdsByFilter(allAppIds, appPermission.getAppFilter());
        if (isEmpty(appIdSet)) {
          return;
        }
        PermissionType permissionType = appPermission.getPermissionType();
        if (permissionType == PermissionType.ALL_APP_ENTITIES) {
          asList(SERVICE, PROVISIONER, ENV, WORKFLOW, PIPELINE, DEPLOYMENT).forEach(permissionType1 -> {
            permissionTypeAppIdSetMap.get(permissionType1).addAll(appIdSet);
          });
        } else {
          permissionTypeAppIdSetMap.get(permissionType).addAll(appIdSet);
        }
      });
    });

    Set<String> appIdSetForWorkflowPermission = permissionTypeAppIdSetMap.get(WORKFLOW);
    if (isEmpty(appIdSetForWorkflowPermission)) {
      appIdSetForWorkflowPermission = new HashSet<>();
      permissionTypeAppIdSetMap.put(WORKFLOW, appIdSetForWorkflowPermission);
    }

    // pipeline will need workflow
    appIdSetForWorkflowPermission.addAll(permissionTypeAppIdSetMap.get(PIPELINE));

    Set<String> appIdSetForEnvPermission = permissionTypeAppIdSetMap.get(ENV);
    if (isEmpty(appIdSetForEnvPermission)) {
      appIdSetForEnvPermission = new HashSet<>();
      permissionTypeAppIdSetMap.put(ENV, appIdSetForEnvPermission);
    }

    // workflow will need env
    appIdSetForEnvPermission.addAll(appIdSetForWorkflowPermission);

    // DEPLOYMENT will need env
    appIdSetForEnvPermission.addAll(permissionTypeAppIdSetMap.get(DEPLOYMENT));

    return permissionTypeAppIdSetMap;
  }

  private Set<String> getAppIdsByFilter(Set<String> allAppIds, GenericEntityFilter appFilter) {
    if (appFilter == null || FilterType.ALL.equals(appFilter.getFilterType())) {
      return new HashSet<>(allAppIds);
    }

    if (FilterType.SELECTED.equals(appFilter.getFilterType())) {
      SetView<String> intersection = Sets.intersection(appFilter.getIds(), allAppIds);
      return new HashSet<>(intersection);
    } else {
      throw new InvalidRequestException("Unknown app filter type: " + appFilter.getFilterType());
    }
  }

  public Set<String> getAppIdsByFilter(String accountId, GenericEntityFilter appFilter) {
    List<String> appIdsByAccountId = appService.getAppIdsByAccountId(accountId);
    return getAppIdsByFilter(Sets.newHashSet(appIdsByAccountId), appFilter);
  }

  public Set<String> getEnvIdsByFilter(String appId, EnvFilter envFilter) {
    PageRequest<Environment> pageRequest =
        aPageRequest().addFilter("appId", Operator.EQ, appId).addFieldsIncluded("_id", "environmentType").build();
    List<Environment> envList = getAllEntities(pageRequest, () -> environmentService.list(pageRequest, false, null));

    return getEnvIdsByFilter(envList, envFilter);
  }

  public <T extends Base> List<T> getAllEntities(PageRequest<T> pageRequest, Callable<PageResponse<T>> callable) {
    return wingsPersistence.getAllEntities(pageRequest, callable);
  }

  private void setEntityIdFilter(List<PermissionAttribute> requiredPermissionAttributes,
      UserRequestContext userRequestContext, List<String> appIds) {
    String entityFieldName = getEntityFieldName(requiredPermissionAttributes);

    userRequestContext.setEntityIdFilterRequired(true);

    Set<String> entityIds =
        getEntityIds(requiredPermissionAttributes, userRequestContext.getUserPermissionInfo(), appIds);
    UserRequestContext.EntityInfo entityInfo = UserRequestContext.EntityInfo.builder().entityFieldName(entityFieldName).entityIds(entityIds).build();
    String entityClassName = getEntityClassName(requiredPermissionAttributes);
    userRequestContext.getEntityInfoMap().put(entityClassName, entityInfo);
  }

  private Set<String> getEntityIds(
      List<PermissionAttribute> permissionAttributes, UserPermissionInfo userPermissionInfo, List<String> appIds) {
    final Set<String> entityIds = new HashSet<>();

    if (appIds == null) {
      return entityIds;
    }

    Map<String, AppPermissionSummary> appPermissionMap = userPermissionInfo.getAppPermissionMapInternal();
    if (MapUtils.isEmpty(appPermissionMap)) {
      return entityIds;
    }

    for (String appId : appIds) {
      AppPermissionSummary appPermissionSummary = appPermissionMap.get(appId);
      if (appPermissionSummary == null) {
        continue;
      }

      for (PermissionAttribute permissionAttribute : permissionAttributes) {
        PermissionType permissionType = permissionAttribute.getPermissionType();
        Action action = permissionAttribute.getAction();

        Map<Action, Set<String>> entityPermissions = null;
        if (permissionType == SERVICE) {
          entityPermissions = appPermissionSummary.getServicePermissions();
        } else if (permissionType == PROVISIONER) {
          entityPermissions = appPermissionSummary.getProvisionerPermissions();
        } else if (permissionType == ENV) {
          Map<Action, Set<AppPermissionSummary.EnvInfo>> envEntityPermissions = appPermissionSummary.getEnvPermissions();
          if (isNotEmpty(envEntityPermissions)) {
            Set<AppPermissionSummary.EnvInfo> envInfoSet = envEntityPermissions.get(action);
            if (isNotEmpty(envInfoSet)) {
              envInfoSet.forEach(envInfo -> entityIds.add(envInfo.getEnvId()));
            }
          }
          continue;
        } else if (permissionType == WORKFLOW) {
          entityPermissions = appPermissionSummary.getWorkflowPermissions();
        } else if (permissionType == PIPELINE) {
          entityPermissions = appPermissionSummary.getPipelinePermissions();
        } else if (permissionType == DEPLOYMENT) {
          entityPermissions = appPermissionSummary.getDeploymentPermissions();
        }

        if (isEmpty(entityPermissions)) {
          continue;
        }

        Set<String> entityIdCollection = entityPermissions.get(action);
        if (isNotEmpty(entityIdCollection)) {
          entityIds.addAll(entityIdCollection);
        }
      }
    }
    return entityIds;
  }

  private String getEntityFieldName(List<PermissionAttribute> permissionAttributes) {
    Optional<String> entityFieldNameOptional = permissionAttributes.stream()
                                                   .map(permissionAttribute -> {
                                                     if (StringUtils.isNotBlank(permissionAttribute.getDbFieldName())) {
                                                       return permissionAttribute.getDbFieldName();
                                                     }

                                                     return "_id";
                                                   })
                                                   .findFirst();

    if (entityFieldNameOptional.isPresent()) {
      return entityFieldNameOptional.get();
    }

    return null;
  }

  private String getEntityClassName(List<PermissionAttribute> permissionAttributes) {
    Optional<String> entityFieldNameOptional =
        permissionAttributes.stream()
            .map(permissionAttribute -> {
              if (StringUtils.isNotBlank(permissionAttribute.getDbCollectionName())) {
                return permissionAttribute.getDbCollectionName();
              }

              PermissionType permissionType = permissionAttribute.getPermissionType();

              String className;
              if (permissionType == SERVICE) {
                className = Service.class.getName();
              } else if (permissionType == PROVISIONER) {
                className = InfrastructureProvisioner.class.getName();
              } else if (permissionType == ENV) {
                className = Environment.class.getName();
              } else if (permissionType == WORKFLOW) {
                className = Workflow.class.getName();
              } else if (permissionType == PIPELINE) {
                className = Pipeline.class.getName();
              } else if (permissionType == DEPLOYMENT) {
                className = WorkflowExecution.class.getName();
              } else {
                throw new InvalidRequestException("Invalid permission type: " + permissionType);
              }

              return className;
            })
            .findFirst();

    if (entityFieldNameOptional.isPresent()) {
      return entityFieldNameOptional.get();
    }

    return null;
  }

  private Set<EntityInfo> getServiceIdsByFilter(List<Base> services, GenericEntityFilter serviceFilter) {
    if (isEmpty(services)) {
      return new HashSet<>();
    }
    if (serviceFilter == null) {
      serviceFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    }

    if (FilterType.ALL.equals(serviceFilter.getFilterType())) {
      return services.stream()
          .map(s -> EntityInfo.builder().id(s.getUuid()).name(((Service) s).getName()).build())
          .collect(Collectors.toSet());
    } else if (SELECTED.equals(serviceFilter.getFilterType())) {
      GenericEntityFilter finalServiceFilter = serviceFilter;
      return services.stream()
          .filter(service -> finalServiceFilter.getIds().contains(service.getUuid()))
          .map(s -> EntityInfo.builder().id(s.getUuid()).name(((Service) s).getName()).build())
          .collect(Collectors.toSet());
    } else {
      String msg = "Unknown service filter type: " + serviceFilter.getFilterType();
      log.error(msg);
      throw new InvalidRequestException(msg);
    }
  }

  private Set<EntityInfo> getProvisionerIdsByFilter(List<Base> provisioners, GenericEntityFilter provisionerFilter) {
    if (isEmpty(provisioners)) {
      return new HashSet<>();
    }
    if (provisionerFilter == null) {
      provisionerFilter = GenericEntityFilter.builder().filterType(FilterType.ALL).build();
    }

    if (FilterType.ALL.equals(provisionerFilter.getFilterType())) {
      return provisioners.stream()
          .map(p -> EntityInfo.builder().id(p.getUuid()).name(((InfrastructureProvisioner) p).getName()).build())
          .collect(Collectors.toSet());
    } else if (SELECTED.equals(provisionerFilter.getFilterType())) {
      GenericEntityFilter finalServiceFilter = provisionerFilter;
      return provisioners.stream()
          .filter(service -> finalServiceFilter.getIds().contains(service.getUuid()))
          .map(p -> EntityInfo.builder().id(p.getUuid()).name(((InfrastructureProvisioner) p).getName()).build())
          .collect(Collectors.toSet());
    } else {
      String msg = "Unknown service filter type: " + provisionerFilter.getFilterType();
      log.error(msg);
      throw new InvalidRequestException(msg);
    }
  }

  private EnvFilter getDefaultEnvFilterIfNull(EnvFilter envFilter) {
    if (envFilter == null || isEmpty(envFilter.getFilterTypes())) {
      envFilter = new EnvFilter();
      envFilter.setFilterTypes(Sets.newHashSet(PROD, NON_PROD));
    }
    return envFilter;
  }

  private <T extends Base> Set<String> getEnvIdsByFilter(List<T> environments, EnvFilter envFilter) {
    if (environments == null) {
      return new HashSet<>();
    }

    envFilter = getDefaultEnvFilterIfNull(envFilter);

    Set<String> filterTypes = envFilter.getFilterTypes();

    boolean selected = hasEnvSelectedType(envFilter);
    if (selected) {
      EnvFilter finalEnvFilter = envFilter;
      return environments.stream()
          .filter(environment -> finalEnvFilter.getIds().contains(environment.getUuid()))
          .map(Base::getUuid)
          .collect(Collectors.toSet());
    } else {
      return environments.stream()
          .filter(environment -> filterTypes.contains(((Environment) environment).getEnvironmentType().name()))
          .map(Base::getUuid)
          .collect(Collectors.toSet());
    }
  }

  private Set<EnvInfo> getEnvsInfoByFilter(List<Base> environments, EnvFilter envFilter) {
    if (environments == null) {
      return new HashSet<>();
    }

    envFilter = getDefaultEnvFilterIfNull(envFilter);

    Set<String> filterTypes = envFilter.getFilterTypes();

    boolean selected = hasEnvSelectedType(envFilter);
    if (selected) {
      EnvFilter finalEnvFilter = envFilter;
      return environments.stream()
          .filter(environment -> finalEnvFilter.getIds().contains(environment.getUuid()))
          .map(environment
              -> EnvInfo.builder()
                     .envId(environment.getUuid())
                     .envType(((Environment) environment).getEnvironmentType().name())
                     .envName(((Environment) environment).getName())
                     .build())
          .collect(Collectors.toSet());
    } else {
      return environments.stream()
          .filter(environment -> filterTypes.contains(((Environment) environment).getEnvironmentType().name()))
          .map(environment
              -> EnvInfo.builder()
                     .envId(environment.getUuid())
                     .envType(((Environment) environment).getEnvironmentType().name())
                     .envName(((Environment) environment).getName())
                     .build())
          .collect(Collectors.toSet());
    }
  }

  private Set<EnvironmentType> addEnvTypesByFilter(Set<EnvironmentType> existingEnvTypes, EnvFilter envFilter) {
    envFilter = getDefaultEnvFilterIfNull(envFilter);

    if (existingEnvTypes == null) {
      existingEnvTypes = new HashSet<>();
    }

    Set<EnvironmentType> environmentTypeSet = envFilter.getFilterTypes()
                                                  .stream()
                                                  .filter(filter -> !filter.equals(EnvFilter.FilterType.SELECTED))
                                                  .map(EnvironmentType::valueOf)
                                                  .collect(Collectors.toSet());
    existingEnvTypes.addAll(environmentTypeSet);

    return existingEnvTypes;
  }

  private WorkflowFilter getDefaultWorkflowFilterIfNull(WorkflowFilter workflowFilter) {
    if (workflowFilter == null || isEmpty(workflowFilter.getFilterTypes())) {
      workflowFilter = new WorkflowFilter();
      workflowFilter.setFilterTypes(Sets.newHashSet(PROD, NON_PROD, WorkflowFilter.FilterType.TEMPLATES));
    }
    return workflowFilter;
  }

  private Set<EntityInfo> getWorkflowIdsByFilter(
      List<Base> workflows, List<Base> environments, WorkflowFilter workflowFilter) {
    if (workflows == null) {
      return new HashSet<>();
    }

    workflowFilter = getDefaultWorkflowFilterIfNull(workflowFilter);

    Set<String> filterEnvIds = workflowFilter.getIds();
    if (filterEnvIds == null) {
      filterEnvIds = new HashSet<>();
    }

    boolean hasTemplateFilterType = workflowFilter.getFilterTypes().contains(WorkflowFilter.FilterType.TEMPLATES);

    Set<String> finalFilterEnvIds = filterEnvIds;
    WorkflowFilter finalWorkflowFilter = workflowFilter;

    final Set<String> envIds;
    if (environments != null) {
      envIds = environments.stream()
                   .filter(environment
                       -> finalFilterEnvIds.contains(environment.getUuid())
                           || finalWorkflowFilter.getFilterTypes().contains(
                               ((Environment) environment).getEnvironmentType().name()))
                   .map(Base::getUuid)
                   .collect(Collectors.toSet());
    } else {
      envIds = Collections.emptySet();
    }

    return workflows.stream()
        .filter(workflow -> {
          Workflow workflowObj = (Workflow) workflow;
          if (isEnvTemplatized(workflowObj)) {
            return hasTemplateFilterType;
          }

          if (workflowObj.getEnvId() == null) {
            return true;
          }

          return envIds.contains(workflowObj.getEnvId());
        })
        .map(w -> EntityInfo.builder().id(w.getUuid()).name(((Workflow) w).getName()).build())
        .collect(Collectors.toSet());
  }

  private Set<EntityInfo> getDeploymentIdsByFilter(
      List<Base> workflows, List<Base> environments, EnvFilter envFilter, String appId) {
    WorkflowFilter workflowFilter = getWorkflowFilterFromEnvFilter(envFilter);

    if (environments != null) {
      Set<String> envIds = getEnvIdsByFilter(environments, envFilter);
      if (CollectionUtils.isEmpty(envIds)) {
        log.info("No environments matched the filter for app {}. Returning empty set of deployments", appId);
        return new HashSet<>();
      }
    }

    return getWorkflowIdsByFilter(workflows, environments, workflowFilter);
  }

  private WorkflowFilter getWorkflowFilterFromEnvFilter(EnvFilter envFilter) {
    envFilter = getDefaultEnvFilterIfNull(envFilter);

    // Construct workflow filter since we also want to include templates to deployments
    WorkflowFilter workflowFilter = new WorkflowFilter();

    Set<String> workflowFilterTypes = Sets.newHashSet();

    final EnvFilter envFilterFinal = envFilter;

    envFilter.getFilterTypes().forEach(filterType -> {
      workflowFilterTypes.add(filterType);
      if (filterType.equals(EnvFilter.FilterType.SELECTED)) {
        workflowFilter.setIds(envFilterFinal.getIds());
      }
    });

    workflowFilterTypes.add(WorkflowFilter.FilterType.TEMPLATES);
    workflowFilter.setFilterTypes(workflowFilterTypes);

    return workflowFilter;
  }

  private Map<String, Workflow> fillWorkflowCache(Map<String, Workflow> workflowCache, List<Base> pipelines) {
    Set<String> newWorkflowIds = new HashSet<>();
    // Find all new workflow ids that are not present in the cache but present in the list of pipelines.
    pipelines.forEach(p -> {
      Pipeline pipeline = (Pipeline) p;
      if (pipeline.getPipelineStages() == null) {
        return;
      }

      pipeline.getPipelineStages().forEach(pipelineStage -> {
        if (pipelineStage == null || pipelineStage.getPipelineStageElements() == null) {
          return;
        }

        pipelineStage.getPipelineStageElements().forEach(pipelineStageElement -> {
          final Map<String, Object> pipelineStageElementProperties = pipelineStageElement.getProperties();
          if (pipelineStageElementProperties == null
              || pipelineStageElementProperties.get(EnvStateKeys.workflowId) == null) {
            return;
          }

          String workflowId = (String) pipelineStageElement.getProperties().get(EnvStateKeys.workflowId);
          if (workflowCache == null || !workflowCache.containsKey(workflowId)) {
            newWorkflowIds.add(workflowId);
          }
        });
      });
    });

    // Return if no new workflow ids found. Cache has all the needed workflows.
    Map<String, Workflow> finalWorkflowCache = (workflowCache == null) ? new HashMap<>() : workflowCache;
    if (isEmpty(newWorkflowIds)) {
      return finalWorkflowCache;
    }

    // Fetch all the workflows in batch.
    List<Workflow> workflows = workflowService.listWorkflowsWithoutOrchestration(newWorkflowIds);
    if (isEmpty(workflows)) {
      return finalWorkflowCache;
    }

    // Update the workflowCache and return.
    for (Workflow workflow : workflows) {
      finalWorkflowCache.put(workflow.getUuid(), workflow);
    }
    return finalWorkflowCache;
  }

  private Multimap<EntityInfo, Action> getPipelineIdsByFilter(List<Base> pipelines, List<Base> environments,
      EnvFilter envFilter, Multimap<String, Action> envActionMap, Set<Action> entityActionsFromCurrentPermission,
      Map<String, Workflow> workflowCache) {
    Multimap<EntityInfo, Action> pipelineActionMap = HashMultimap.create();
    if (isEmpty(pipelines)) {
      return pipelineActionMap;
    }

    Set<String> envIds;
    if (isNotEmpty(environments)) {
      envIds = getEnvIdsByFilter(environments, envFilter);
      envIds.forEach(envId -> envActionMap.putAll(envId, entityActionsFromCurrentPermission));
    } else {
      envIds = Collections.emptySet();
    }

    final Map<String, Workflow> finalWorkflowCache = fillWorkflowCache(workflowCache, pipelines);
    Set<String> envIdsFromOtherPermissions = envActionMap.keySet();
    pipelines.forEach(p -> {
      Set<Action> entityActions = new HashSet<>(entityActionsFromCurrentPermission);
      boolean match;
      Pipeline pipeline = (Pipeline) p;
      if (pipeline.getPipelineStages() == null) {
        match = true;
      } else {
        match = pipeline.getPipelineStages().stream().allMatch(pipelineStage
            -> pipelineStage != null && pipelineStage.getPipelineStageElements() != null
                && pipelineStage.getPipelineStageElements().stream().allMatch(pipelineStageElement -> {
                     Pair<String, Boolean> pair = resolveEnvIdForPipelineStageElement(
                         pipeline.getAppId(), pipelineStageElement, finalWorkflowCache);
                     String envId = pair.getLeft();
                     if (isBlank(envId)) {
                       return pair.getRight();
                     }

                     if (envIds.contains(envId)) {
                       return true;
                     } else if (envIdsFromOtherPermissions.contains(envId)) {
                       entityActions.retainAll(envActionMap.get(envId));
                       return true;
                     }

                     return false;
                   }));
      }

      if (match) {
        pipelineActionMap.putAll(
            EntityInfo.builder().id(pipeline.getUuid()).name(pipeline.getName()).build(), entityActions);
      }
    });
    return pipelineActionMap;
  }

  public boolean checkIfPipelineHasOnlyGivenEnvs(Pipeline pipeline, Set<String> allowedEnvIds) {
    if (isEmpty(pipeline.getPipelineStages())) {
      return true;
    }

    Map<String, Workflow> workflowCache = fillWorkflowCache(new HashMap<>(), Collections.singletonList(pipeline));
    return pipeline.getPipelineStages().stream().allMatch(pipelineStage
        -> pipelineStage != null && pipelineStage.getPipelineStageElements() != null
            && pipelineStage.getPipelineStageElements().stream().allMatch(pipelineStageElement -> {
                 Pair<String, Boolean> pair =
                     resolveEnvIdForPipelineStageElement(pipeline.getAppId(), pipelineStageElement, workflowCache);
                 String envId = pair.getLeft();
                 if (isBlank(envId)) {
                   return pair.getRight();
                 }

                 return isNotEmpty(allowedEnvIds) && allowedEnvIds.contains(envId);
               }));
  }

  private Pair<String, Boolean> resolveEnvIdForPipelineStageElement(
      String appId, PipelineStageElement pipelineStageElement, Map<String, Workflow> workflowCache) {
    if (pipelineStageElement.getType().equals(StateType.APPROVAL.name())) {
      return ImmutablePair.of(null, Boolean.TRUE);
    }

    final Map<String, Object> pipelineStageElementProperties = pipelineStageElement.getProperties();
    if (pipelineStageElementProperties == null || pipelineStageElementProperties.get(EnvStateKeys.workflowId) == null) {
      return ImmutablePair.of(null, Boolean.FALSE);
    }

    String envId = resolveEnvId(appId, pipelineStageElement, workflowCache);
    if (envId == null || (pipelineStageElement.checkDisableAssertion() && isEmpty(envId))
        || ManagerExpressionEvaluator.matchesVariablePattern(envId)) {
      return ImmutablePair.of(null, Boolean.TRUE);
    }

    return ImmutablePair.of(envId, Boolean.FALSE);
  }

  private String resolveEnvId(
      String appId, PipelineStageElement pipelineStageElement, Map<String, Workflow> workflowCache) {
    String workflowId = (String) pipelineStageElement.getProperties().get(EnvStateKeys.workflowId);
    Workflow workflow;
    if (workflowCache.containsKey(workflowId)) {
      workflow = workflowCache.get(workflowId);
    } else {
      log.info("Workflow not found in cache: {}", workflowId);
      workflow = workflowService.readWorkflowWithoutOrchestration(appId, workflowId);
      if (workflow == null) {
        return null;
      }
      workflowCache.put(workflowId, workflow);
    }
    return workflowServiceHelper.obtainEnvIdWithoutOrchestration(workflow, pipelineStageElement.getWorkflowVariables());
  }

  public boolean isEnvTemplatized(Workflow workflow) {
    List<TemplateExpression> templateExpressions = workflow.getTemplateExpressions();
    if (CollectionUtils.isNotEmpty(templateExpressions)) {
      return templateExpressions.stream()
          .filter(templateExpression -> templateExpression.getFieldName().equals("envId"))
          .findFirst()
          .isPresent();
    }
    return false;
  }

  private boolean hasEnvSelectedType(EnvFilter envFilter) {
    Set<String> filterTypes = envFilter.getFilterTypes();
    if (isEmpty(filterTypes)) {
      return false;
    }

    return filterTypes.stream()
        .filter(filterType -> filterType.equals(EnvFilter.FilterType.SELECTED))
        .findFirst()
        .isPresent();
  }

  private void setAppPermissionMap(UserPermissionInfo userPermissionInfo) {
    Map<String, AppPermissionSummary> fromAppPermissionSummaryMap = userPermissionInfo.getAppPermissionMapInternal();
    Map<String, AppPermissionSummaryForUI> toAppPermissionSummaryMap = new HashMap<>();
    if (MapUtils.isEmpty(fromAppPermissionSummaryMap)) {
      userPermissionInfo.setAppPermissionMap(toAppPermissionSummaryMap);
    }

    fromAppPermissionSummaryMap.forEach((key, summary) -> {
      AppPermissionSummaryForUI toAppPermissionSummary = convertAppSummaryToAppSummaryForUI(summary);
      toAppPermissionSummaryMap.put(key, toAppPermissionSummary);
    });

    userPermissionInfo.setAppPermissionMap(toAppPermissionSummaryMap);
  }

  private AppPermissionSummaryForUI convertAppSummaryToAppSummaryForUI(AppPermissionSummary fromSummary) {
    AppPermissionSummaryForUIBuilder toAppPermissionSummaryBuilder =
        AppPermissionSummaryForUI.builder()
            .canCreateService(fromSummary.isCanCreateService())
            .canCreateProvisioner(fromSummary.isCanCreateProvisioner())
            .canCreateEnvironment(fromSummary.isCanCreateEnvironment())
            .canCreateWorkflow(fromSummary.isCanCreateWorkflow())
            .canCreatePipeline(fromSummary.isCanCreatePipeline())
            .servicePermissions(convertActionEntityIdMapToEntityActionMap(fromSummary.getServicePermissions()))
            .provisionerPermissions(convertActionEntityIdMapToEntityActionMap(fromSummary.getProvisionerPermissions()))
//            .envPermissions(convertActionEnvMapToEnvActionMap(fromSummary.getEnvPermissions()))
            .workflowPermissions(convertActionEntityIdMapToEntityActionMap(fromSummary.getWorkflowPermissions()))
            .pipelinePermissions(convertActionEntityIdMapToEntityActionMap(fromSummary.getPipelinePermissions()))
            .deploymentPermissions(convertActionEntityIdMapToEntityActionMap(fromSummary.getDeploymentPermissions()));
    return toAppPermissionSummaryBuilder.build();
  }

  /**
   * Transforms the Map -> (Action, Set of EntityIds) to Map -> (EntityId, Set of Actions)
   * The second format is organized in the way optimized for UI consumption.
   * @param fromMap
   * @return
   */
  private Map<String, Set<Action>> convertActionEntityIdMapToEntityActionMap(Map<Action, Set<String>> fromMap) {
    Map<String, Set<Action>> toMap = new HashMap<>();
    if (isEmpty(fromMap)) {
      return null;
    }

    fromMap.forEach((action, entitySet) -> {
      if (CollectionUtils.isNotEmpty(entitySet)) {
        entitySet.forEach(entityId -> {
          Set<Action> actionSet = toMap.get(entityId);
          if (actionSet == null) {
            actionSet = new HashSet<>();
          }
          actionSet.add(action);
          toMap.put(entityId, actionSet);
        });
      }
    });

    return toMap;
  }

  private Map<String, Set<Action>> convertActionEnvMapToEnvActionMap(Map<Action, Set<EnvInfo>> fromMap) {
    Map<String, Set<Action>> toMap = new HashMap<>();
    if (isEmpty(fromMap)) {
      return toMap;
    }

    fromMap.forEach((action, envInfoSet) -> {
      if (CollectionUtils.isNotEmpty(envInfoSet)) {
        envInfoSet.forEach(envInfo -> {
          Set<Action> actionSet = toMap.get(envInfo.getEnvId());
          if (actionSet == null) {
            actionSet = new HashSet<>();
          }
          actionSet.add(action);
          toMap.put(envInfo.getEnvId(), actionSet);
        });
      }
    });

    return toMap;
  }

  public Set<PermissionType> getAllAccountPermissions() {
    return Sets.newHashSet(USER_PERMISSION_MANAGEMENT, ACCOUNT_MANAGEMENT, MANAGE_APPLICATIONS, TEMPLATE_MANAGEMENT,
        USER_PERMISSION_READ, AUDIT_VIEWER, MANAGE_TAGS, CE_ADMIN, CE_VIEWER, MANAGE_CLOUD_PROVIDERS, MANAGE_CONNECTORS,
        MANAGE_APPLICATION_STACKS, MANAGE_DELEGATES, MANAGE_ALERT_NOTIFICATION_RULES, MANAGE_DELEGATE_PROFILES,
        MANAGE_CONFIG_AS_CODE, MANAGE_SECRETS, MANAGE_SECRET_MANAGERS, MANAGE_AUTHENTICATION_SETTINGS,
        MANAGE_IP_WHITELIST, MANAGE_DEPLOYMENT_FREEZES, MANAGE_PIPELINE_GOVERNANCE_STANDARDS, MANAGE_API_KEYS,
        MANAGE_CUSTOM_DASHBOARDS, CREATE_CUSTOM_DASHBOARDS, MANAGE_SSH_AND_WINRM, MANAGE_RESTRICTED_ACCESS);
  }

  private Set<Action> getAllActions() {
    return Sets.newHashSet(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE, Action.EXECUTE_WORKFLOW,
        Action.EXECUTE_WORKFLOW_ROLLBACK, Action.EXECUTE_PIPELINE);
  }

  private Set<Action> getAllNonDeploymentActions() {
    return Sets.newHashSet(Action.CREATE, Action.READ, Action.UPDATE, Action.DELETE);
  }
}
